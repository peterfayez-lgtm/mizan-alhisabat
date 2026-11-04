package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppSettings
import com.example.data.AppUpdateManager
import com.example.data.AppUpdateManifest
import com.example.data.FreeIntegrationsHelper
import com.example.ui.theme.EssentialBlue
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppUpdateCenterDialog(
    appSettings: AppSettings,
    availableUpdate: AppUpdateManifest?,
    isCheckingUpdate: Boolean,
    onCheckForUpdatesNow: () -> Unit,
    onSaveUpdateSettings: (
        autoCheckOnLaunch: Boolean,
        updateManifestUrl: String,
        publishedVersionCode: Int,
        publishedVersionName: String,
        publishedApkUrl: String,
        publishedReleaseNotes: String,
        publishedIsMandatory: Boolean
    ) -> Unit,
    onSimulateUpdatePreview: (
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotes: String,
        isMandatory: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersionCode = AppUpdateManager.currentVersionCode
    val currentVersionName = AppUpdateManager.currentVersionName
    val applicationId = AppUpdateManager.applicationId

    // 0 = فحص وتثبيت التحديثات (للمستخدم), 1 = الربط التلقائي مع GitHub Releases ونشر التحديثات
    var selectedTab by remember { mutableIntStateOf(0) }

    var autoCheckOnLaunch by remember(appSettings.autoCheckUpdatesOnLaunch) {
        mutableStateOf(appSettings.autoCheckUpdatesOnLaunch)
    }
    var updateManifestUrl by remember(appSettings.updateManifestUrl) {
        mutableStateOf(
            appSettings.updateManifestUrl.ifBlank {
                "https://api.github.com/repos/peterfayez-lgtm/mizan-alhisabat/releases/latest"
            }
        )
    }
    var githubRepoShorthand by remember { mutableStateOf("peterfayez-lgtm/mizan-alhisabat") }
    var pubVersionCodeInput by remember(appSettings.publishedLatestVersionCode) {
        mutableStateOf(appSettings.publishedLatestVersionCode.toString())
    }
    var pubVersionNameInput by remember(appSettings.publishedLatestVersionName) {
        mutableStateOf(appSettings.publishedLatestVersionName)
    }
    var pubApkUrlInput by remember(appSettings.publishedApkDownloadUrl) {
        mutableStateOf(
            appSettings.publishedApkDownloadUrl.ifBlank {
                "https://github.com/peterfayez-lgtm/mizan-alhisabat/releases/latest/download/Mizan-AlHisabat-Latest.apk"
            }
        )
    }
    var pubReleaseNotesInput by remember(appSettings.publishedReleaseNotesAr) {
        mutableStateOf(appSettings.publishedReleaseNotesAr)
    }
    var pubIsMandatory by remember(appSettings.publishedIsMandatoryUpdate) {
        mutableStateOf(appSettings.publishedIsMandatoryUpdate)
    }
    var showManifestJsonDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSendingCloudAnnouncement by remember { mutableStateOf(false) }

    val hasNewerUpdate = availableUpdate != null && availableUpdate.latestVersionCode > currentVersionCode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "مركز التحديث السحابي (GitHub Releases)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الإصدار الحالي المثبت: v$currentVersionName (Build #$currentVersionCode)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeGreen
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Switcher
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("١. فحص وتثبيت التحديثات") },
                        modifier = Modifier.testTag("update_center_tab_user")
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("٢. الربط مع GitHub ونشر التحديث 🛠️") },
                        modifier = Modifier.testTag("update_center_tab_publisher")
                    )
                }

                if (feedbackMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SafeGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (selectedTab == 0) {
                    // 1. Data Preservation Guarantee Shield
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EssentialBlue.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EssentialBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = EssentialBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "🔒 ضمان حفظ البيانات 100% عند التحديث",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EssentialBlue
                                )
                            }
                            Text(
                                text = "معرّف الحزمة الثابت: ($applicationId)\nعند تثبيت أي تحديث مستقبلي من GitHub Releases فوق النسخة الحالية، يقوم نظام أندرويد بتحديث الكود والواجهة فقط ويحتفظ تلقائياً بجميع معاملاتك، أرصدتك، ميزانياتك، وإعداداتك دون مسح أي شيء.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Current Update Status Card
                    if (hasNewerUpdate && availableUpdate != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SafeGreen.copy(alpha = 0.14f),
                            border = BorderStroke(1.5.dp, SafeGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NewReleases,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🚀 يتوفر تحديث جديد: الإصدار v${availableUpdate.latestVersionName} (Build #${availableUpdate.latestVersionCode})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SafeGreen
                                    )
                                }

                                if (availableUpdate.publishedDateAr.isNotBlank()) {
                                    Text(
                                        text = "تاريخ الإصدار: ${availableUpdate.publishedDateAr}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "✨ الجديد في هذا الإصدار:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = availableUpdate.releaseNotesAr,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Button(
                                    onClick = {
                                        val ok = AppUpdateManager.openUpdateDownloadUrl(
                                            context = context,
                                            rawUrl = availableUpdate.apkDownloadUrl
                                        )
                                        if (!ok) {
                                            feedbackMessage = "ℹ️ لم يتم إدخال رابط تحميل مباشر بعد. يمكنك ربط مستودع GitHub من التبويب الثاني بضغطة واحدة."
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("download_install_update_button")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⬇️ تحميل وتثبيت التحديث الآن (مع حفظ البيانات)",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "✅ هاتفك يعمل بأحدث إصدار (v$currentVersionName)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SafeGreen
                                        )
                                        Text(
                                            text = "رقم البناء الحالي: Build #$currentVersionCode • جاهز للتحديث التلقائي عبر GitHub",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "📋 مميزات الإصدار الحالي المثبت (v$currentVersionName):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = appSettings.publishedReleaseNotesAr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // 3. Check Updates Button & Auto-Check Switch
                    FilledTonalButton(
                        onClick = onCheckForUpdatesNow,
                        enabled = !isCheckingUpdate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_for_updates_now_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCheckingUpdate) "جاري فحص GitHub Releases..." else "🔄 فحص وجود تحديث جديد على GitHub الآن",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "فحص التحديثات تلقائياً عند فتح البرنامج",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إظهار شريط تنبيه فوري أعلى الشاشة عند نشر إصدار جديد على GitHub",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoCheckOnLaunch,
                                onCheckedChange = { autoCheckOnLaunch = it }
                            )
                        }
                    }
                } else {
                    // TAB 2: GitHub Releases Automated Setup + Publisher Panel
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SafeGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SafeGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚡ تم برمجة البناء والرفع التلقائي على GitHub Releases لك!",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                            Text(
                                text = "لقد أضفت لك داخل المشروع ملف تشغيل تلقائي (.github/workflows/android-release.yml) يقوم بكل الخطوات الصعبة نيابةً عنك:\n" +
                                    "١. اضغط فقط على أيقونة GitHub أعلى شاشة AI Studio واختر (Push to GitHub).\n" +
                                    "٢. سيقوم GitHub تلقائياً ببناء ملف الـ APK وإنشاء صفحة Release جديدة ورفع ملف التحديث (Mizan-AlHisabat-Latest.apk) بدون أي تدخل منك!\n" +
                                    "٣. اكتب اسم مستودعك بالأسفل مرة واحدة فقط ليرتبط التطبيق به للأبد.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // 1-Click GitHub Repo Linker
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🔗 ربط سريع بمستودع GitHub الخاص بك (بضغطة واحدة):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = githubRepoShorthand,
                                onValueChange = { githubRepoShorthand = it },
                                label = { Text("اسم المستودع أو الرابط (مثال: username/mizan-alhisabat)") },
                                placeholder = { Text("username/mizan-alhisabat") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("github_repo_shorthand_input")
                            )
                            FilledTonalButton(
                                onClick = {
                                    val cleanInput = githubRepoShorthand.trim()
                                    val apiLink = AppUpdateManager.normalizeGitHubOrManifestUrl(cleanInput)
                                    if (apiLink.contains("api.github.com/repos/")) {
                                        val repoPath = apiLink
                                            .substringAfter("api.github.com/repos/")
                                            .substringBefore("/releases")
                                        updateManifestUrl = apiLink
                                        pubApkUrlInput = "https://github.com/$repoPath/releases/latest/download/Mizan-AlHisabat-Latest.apk"
                                        feedbackMessage = "✅ تم توليد روابط GitHub Releases التلقائية للمستودع ($repoPath) بنجاح! اضغط «حفظ الإعدادات» بالأسفل."
                                    } else {
                                        feedbackMessage = "⚠️ يرجى كتابة اسم المستودع بصيغة: اسم_المستخدم/اسم_المشروع (مثال: peter/mizan)"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auto_generate_github_links_button")
                            ) {
                                Text("⚡ توليد روابط GitHub Releases التلقائية", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pubVersionCodeInput,
                            onValueChange = { pubVersionCodeInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("كود الإصدار الجديد (أكبر من $currentVersionCode)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("publisher_version_code_input")
                        )
                        OutlinedTextField(
                            value = pubVersionNameInput,
                            onValueChange = { pubVersionNameInput = it },
                            label = { Text("اسم الإصدار (مثلاً 2.2.0)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("publisher_version_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = pubApkUrlInput,
                        onValueChange = { pubApkUrlInput = it },
                        label = { Text("رابط تحميل الـ APK المباشر من GitHub Releases") },
                        placeholder = { Text("https://github.com/USER/REPO/releases/latest/download/Mizan-AlHisabat-Latest.apk") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publisher_apk_url_input")
                    )

                    OutlinedTextField(
                        value = updateManifestUrl,
                        onValueChange = { updateManifestUrl = it },
                        label = { Text("رابط الفحص التلقائي (GitHub Releases API)") },
                        placeholder = { Text("https://api.github.com/repos/USER/REPO/releases/latest") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publisher_manifest_url_input")
                    )

                    OutlinedTextField(
                        value = pubReleaseNotesInput,
                        onValueChange = { pubReleaseNotesInput = it },
                        label = { Text("مميزات التحديث الجديد (تظهر للمستخدمين في إشعار التحديث)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("publisher_release_notes_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحديث هام / إجباري لجميع المستخدمين",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = pubIsMandatory,
                            onCheckedChange = { pubIsMandatory = it }
                        )
                    }

                    HorizontalDivider()

                    // Action 1: Live Preview of how the Update Banner & Popup look
                    FilledTonalButton(
                        onClick = {
                            val code = (pubVersionCodeInput.toIntOrNull() ?: (currentVersionCode + 1))
                                .coerceAtLeast(currentVersionCode + 1)
                            val name = pubVersionNameInput.trim().ifBlank { "$code.0.0" }
                            pubVersionCodeInput = code.toString()
                            pubVersionNameInput = name
                            onSimulateUpdatePreview(
                                code,
                                name,
                                pubApkUrlInput,
                                pubReleaseNotesInput,
                                pubIsMandatory
                            )
                            selectedTab = 0
                            feedbackMessage = "🚀 تم تفعيل معاينة التحديث الجديد (v$name - Build #$code)! شاهده الآن في التبويب الأول وأعلى الشاشة الرئيسية."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_update_preview_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👁️ معاينة ظهور إشعار التحديث الجديد على الهاتف الآن",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Action 2: Send Automatic Cloud Update Notification via CallMeBot & Telegram Bot
                    Button(
                        onClick = {
                            isSendingCloudAnnouncement = true
                            val code = pubVersionCodeInput.toIntOrNull() ?: (currentVersionCode + 1)
                            val name = pubVersionNameInput.trim().ifBlank { "$code.0.0" }
                            val msg = AppUpdateManager.buildWhatsAppUpdateAnnouncement(
                                versionCode = code,
                                versionName = name,
                                apkDownloadUrl = pubApkUrlInput,
                                releaseNotesAr = pubReleaseNotesInput
                            )
                            scope.launch {
                                val waRes = FreeIntegrationsHelper.sendBackgroundWhatsAppReport(
                                    phoneNumber = appSettings.whatsappPhoneNumber.ifBlank { "201021992233" },
                                    callMeBotApiKey = appSettings.whatsappCallMeBotApiKey.ifBlank { "3591043" },
                                    message = msg
                                )
                                if (appSettings.telegramBotEnabled &&
                                    appSettings.telegramBotToken.isNotBlank() &&
                                    appSettings.telegramChatId.isNotBlank()
                                ) {
                                    FreeIntegrationsHelper.sendTelegramBotReport(
                                        botToken = appSettings.telegramBotToken,
                                        chatId = appSettings.telegramChatId,
                                        message = msg
                                    )
                                }
                                isSendingCloudAnnouncement = false
                                feedbackMessage = waRes.getOrElse {
                                    "⚠️ تعذر الإرسال التلقائي عبر CallMeBot: ${it.localizedMessage}"
                                }
                            }
                        },
                        enabled = !isSendingCloudAnnouncement,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_update_whatsapp_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSendingCloudAnnouncement) {
                                "جاري إرسال إشعار التحديث عبر CallMeBot..."
                            } else {
                                "📲 إرسال إشعار التحديث ورابط التحميل عبر CallMeBot وتليجرام"
                            },
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Action 3: Generate & Copy version_manifest.json for cloud hosting
                    OutlinedButton(
                        onClick = { showManifestJsonDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_manifest_json_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📋 توليد ونسخ ملف التحديث السحابي (version_manifest.json)")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val code = pubVersionCodeInput.toIntOrNull() ?: currentVersionCode
                    val name = pubVersionNameInput.trim().ifBlank { currentVersionName }
                    val normalizedManifestUrl = AppUpdateManager.normalizeGitHubOrManifestUrl(updateManifestUrl)
                    onSaveUpdateSettings(
                        autoCheckOnLaunch,
                        normalizedManifestUrl,
                        code,
                        name,
                        pubApkUrlInput,
                        pubReleaseNotesInput,
                        pubIsMandatory
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_update_center_settings_button")
            ) {
                Text("حفظ الإعدادات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )

    if (showManifestJsonDialog) {
        val code = (pubVersionCodeInput.toIntOrNull() ?: (currentVersionCode + 1)).coerceAtLeast(1)
        val name = pubVersionNameInput.trim().ifBlank { "$code.0.0" }
        val jsonContent = remember(code, name, pubApkUrlInput, pubReleaseNotesInput, pubIsMandatory) {
            AppUpdateManager.buildManifestJsonString(
                versionCode = code,
                versionName = name,
                apkDownloadUrl = pubApkUrlInput,
                releaseNotesAr = pubReleaseNotesInput,
                isMandatory = pubIsMandatory
            )
        }
        AlertDialog(
            onDismissRequest = { showManifestJsonDialog = false },
            title = {
                Text("ملف التحديث التلقائي (version_manifest.json)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "يتم توليد هذا الملف تلقائياً بواسطة GitHub Actions عند رفع المشروع، أو يمكنك نسخه يدوياً:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = jsonContent,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("version_manifest.json", jsonContent))
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "version_manifest.json")
                            putExtra(Intent.EXTRA_TEXT, jsonContent)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "نسخ ومشاركة version_manifest.json"))
                        showManifestJsonDialog = false
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نسخ ومشاركة الكود")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManifestJsonDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
