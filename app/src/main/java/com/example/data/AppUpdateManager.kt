package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AppUpdateManager {

    const val DEFAULT_GITHUB_REPO = "peterfayez-lgtm/mizan-alhisabat"
    const val DEFAULT_GITHUB_RELEASES_API = "https://api.github.com/repos/$DEFAULT_GITHUB_REPO/releases/latest"
    const val DEFAULT_RAW_MANIFEST_URL = "https://raw.githubusercontent.com/$DEFAULT_GITHUB_REPO/main/version_manifest.json"
    const val DEFAULT_APK_DOWNLOAD_URL = "https://github.com/$DEFAULT_GITHUB_REPO/releases/latest/download/Mizan-AlHisabat-Latest.apk"

    val currentVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val currentVersionName: String
        get() = BuildConfig.VERSION_NAME

    val applicationId: String
        get() = BuildConfig.APPLICATION_ID

    /**
     * يحوّل اسم مستودع GitHub (مثل username/repo أو رابط المستودع العادي)
     * تلقائياً إلى رابط واجهة GitHub Releases الرسمية:
     * https://api.github.com/repos/username/repo/releases/latest
     */
    fun normalizeGitHubOrManifestUrl(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return DEFAULT_GITHUB_RELEASES_API

        // If already a direct API or raw JSON URL, keep as-is
        if (trimmed.contains("api.github.com/repos/", ignoreCase = true) ||
            trimmed.endsWith(".json", ignoreCase = true)
        ) {
            return trimmed
        }

        // Convert https://github.com/owner/repo or https://github.com/owner/repo/releases...
        val githubUrlRegex = Regex("""github\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)""", RegexOption.IGNORE_CASE)
        val urlMatch = githubUrlRegex.find(trimmed)
        if (urlMatch != null) {
            val owner = urlMatch.groupValues[1]
            val repo = urlMatch.groupValues[2].removeSuffix(".git")
            return "https://api.github.com/repos/$owner/$repo/releases/latest"
        }

        // Convert shorthand "owner/repo"
        val shorthandRegex = Regex("""^([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)$""")
        val shortMatch = shorthandRegex.matchEntire(trimmed)
        if (shortMatch != null) {
            val owner = shortMatch.groupValues[1]
            val repo = shortMatch.groupValues[2].removeSuffix(".git")
            return "https://api.github.com/repos/$owner/$repo/releases/latest"
        }

        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    /**
     * فحص وجود تحديث جديد عبر GitHub Releases API أو ملف version_manifest.json في المستودع
     * أو من إعدادات الناشر المحفوظة.
     */
    suspend fun checkForUpdates(settings: AppSettings): Result<AppUpdateManifest> =
        withContext(Dispatchers.IO) {
            val primaryUrl = normalizeGitHubOrManifestUrl(settings.updateManifestUrl)
            val candidateUrls = buildList {
                if (primaryUrl.startsWith("http://") || primaryUrl.startsWith("https://")) {
                    add(primaryUrl)
                }
                if (primaryUrl.contains("api.github.com/repos/")) {
                    val repoPath = primaryUrl
                        .substringAfter("api.github.com/repos/")
                        .substringBefore("/releases")
                    add("https://raw.githubusercontent.com/$repoPath/main/version_manifest.json")
                } else {
                    add(DEFAULT_RAW_MANIFEST_URL)
                }
            }.distinct()

            var lastException: Exception? = null
            for (url in candidateUrls) {
                try {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github+json, application/json")
                        setRequestProperty("User-Agent", "Mizan-Finance-Android-App")
                        connectTimeout = 8000
                        readTimeout = 8000
                    }
                    val code = conn.responseCode
                    if (code in 200..299) {
                        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                        val parsed = parseRemoteManifestJson(body, settings)
                        if (parsed != null) {
                            return@withContext Result.success(parsed)
                        }
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }

            if (lastException != null && settings.publishedLatestVersionCode <= currentVersionCode) {
                return@withContext Result.failure(lastException)
            }

            // Fallback to local/published broadcast settings
            val fallbackManifest = AppUpdateManifest(
                latestVersionCode = settings.publishedLatestVersionCode.coerceAtLeast(currentVersionCode),
                latestVersionName = settings.publishedLatestVersionName.ifBlank { currentVersionName },
                apkDownloadUrl = settings.publishedApkDownloadUrl.trim().ifBlank { DEFAULT_APK_DOWNLOAD_URL },
                releaseNotesAr = settings.publishedReleaseNotesAr.ifBlank {
                    "تحديث جديد يتضمن تحسينات في الأداء ومزايا ذكية إضافية مع الحفاظ الكامل على بياناتك."
                },
                isMandatoryUpdate = settings.publishedIsMandatoryUpdate,
                publishedDateAr = FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())
            )
            Result.success(fallbackManifest)
        }

    /**
     * يدعم كلاً من:
     * 1. ملف JSON المخصص لمركز تحديثات ميزان (version_manifest.json)
     * 2. واجهة GitHub Releases الرسمية (https://api.github.com/repos/USER/REPO/releases/latest)
     */
    fun parseRemoteManifestJson(rawJson: String, fallbackSettings: AppSettings): AppUpdateManifest? {
        return try {
            val json = JSONObject(rawJson.trim())

            // Case 1: Custom Mizan Update Manifest JSON
            if (json.has("latestVersionCode") || json.has("versionCode")) {
                val vCode = json.optInt("latestVersionCode", json.optInt("versionCode", currentVersionCode))
                val vName = json.optString("latestVersionName", json.optString("versionName", "$vCode.0"))
                val url = json.optString("apkDownloadUrl", json.optString("downloadUrl", fallbackSettings.publishedApkDownloadUrl))
                val notes = json.optString("releaseNotesAr", json.optString("releaseNotes", fallbackSettings.publishedReleaseNotesAr))
                val mandatory = json.optBoolean("isMandatoryUpdate", false)
                val dateAr = json.optString("publishedDateAr", FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis()))
                return AppUpdateManifest(
                    latestVersionCode = vCode,
                    latestVersionName = vName,
                    apkDownloadUrl = url,
                    releaseNotesAr = notes,
                    isMandatoryUpdate = mandatory,
                    publishedDateAr = dateAr
                )
            }

            // Case 2: GitHub Releases API JSON (tag_name, body, assets[0].browser_download_url)
            if (json.has("tag_name")) {
                val rawTag = json.optString("tag_name", "").trim()
                val bodyText = json.optString("body", "").trim()

                // Check if tag has "-b<versionCode>" (from our automated GitHub Action) or "Build #<versionCode>"
                val buildCodeRegex = Regex("""(?:-b|Build\s*#)(\d+)""", RegexOption.IGNORE_CASE)
                val matchedBuildCode = buildCodeRegex.find(rawTag)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: buildCodeRegex.find(bodyText)?.groupValues?.getOrNull(1)?.toIntOrNull()

                val cleanVersionName = rawTag
                    .removePrefix("v")
                    .removePrefix("V")
                    .substringBefore("-b")
                    .trim()
                    .ifBlank { currentVersionName }

                val vCode = matchedBuildCode ?: run {
                    if (cleanVersionName != currentVersionName && cleanVersionName > currentVersionName) {
                        currentVersionCode + 1
                    } else {
                        val digits = rawTag.filter { it.isDigit() }.take(4).toIntOrNull()
                        digits ?: currentVersionCode
                    }
                }

                val bodyNotes = bodyText.ifBlank { fallbackSettings.publishedReleaseNotesAr }
                var apkUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val assetObj = assets.optJSONObject(i) ?: continue
                        val downloadUrl = assetObj.optString("browser_download_url", "")
                        if (downloadUrl.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = downloadUrl
                            break
                        }
                    }
                }
                return AppUpdateManifest(
                    latestVersionCode = vCode,
                    latestVersionName = cleanVersionName,
                    apkDownloadUrl = apkUrl,
                    releaseNotesAr = bodyNotes,
                    isMandatoryUpdate = false,
                    publishedDateAr = FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis())
                )
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * توليد ملف JSON جاهز للرفع مجاناً على Google Drive / GitHub Gist / GitHub Repo
     * لكي تفحصه جميع الهواتف تلقائياً عند فتح البرنامج.
     */
    fun buildManifestJsonString(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotesAr: String,
        isMandatory: Boolean
    ): String {
        val obj = JSONObject().apply {
            put("appName", "ميزان المالي الذكي")
            put("applicationId", applicationId)
            put("latestVersionCode", versionCode)
            put("latestVersionName", versionName.trim())
            put("apkDownloadUrl", apkDownloadUrl.trim())
            put("releaseNotesAr", releaseNotesAr.trim())
            put("isMandatoryUpdate", isMandatory)
            put("publishedDateAr", FinanceAnalyticsEngine.formatDateAr(System.currentTimeMillis()))
        }
        return obj.toString(2)
    }

    /**
     * بناء رسالة إعلان التحديث الرسمية لإرسالها عبر CallMeBot وتليجرام أو واتساب المباشر
     */
    fun buildWhatsAppUpdateAnnouncement(
        versionCode: Int,
        versionName: String,
        apkDownloadUrl: String,
        releaseNotesAr: String
    ): String {
        val safeUrl = apkDownloadUrl.trim().ifBlank { DEFAULT_APK_DOWNLOAD_URL }
        return buildString {
            appendLine("🚀 *تحديث سحابي جديد لتطبيق ميزان المالي (الإصدار v$versionName)*")
            appendLine("📦 رقم البناء: *Build #$versionCode*")
            appendLine("━━━━━━━━━━━━━━━━━━")
            appendLine("✨ *أهم المميزات والتحسينات في هذا التحديث:*")
            appendLine(releaseNotesAr.trim())
            appendLine("━━━━━━━━━━━━━━━━━━")
            appendLine("🔒 *ضمان حفظ البيانات 100%:*")
            appendLine("قم بتثبيت التحديث مباشرة فوق النسخة الحالية *دون حذف التطبيق القديم*، وسيحتفظ هاتفك تلقائياً بجميع معاملاتك، أرصدتك، ميزانياتك، وإعداداتك بالكامل!")
            appendLine("━━━━━━━━━━━━━━━━━━")
            appendLine("⬇️ *رابط تحميل وتثبيت التحديث المباشر (GitHub Releases):*")
            appendLine(safeUrl)
        }
    }

    /**
     * فتح رابط تحميل التحديث المباشر (APK أو صفحة GitHub Releases)
     */
    fun openUpdateDownloadUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return false
        val normalizedUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
