package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppDesignPreset
import com.example.ui.theme.SafeGreen

@Composable
fun DesignStudioDialog(
    currentPreset: AppDesignPreset,
    isDarkTheme: Boolean,
    onSelectPreset: (AppDesignPreset) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "استوديو تصميمات الواجهة (5 ديزاينات)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "اضغط على أي ديزاين لمعاينته وتطبيقه فوراً على كامل البرنامج",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                // Quick Dark / Light Switcher inside the Studio
                FilledTonalButton(
                    onClick = onToggleDarkTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_toggle_dark_light_button")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isDarkTheme) "الوضع الحالي: ليلي داكن (اضغط لتجربة الوضع النهاري الفاتح ☀️)"
                        else "الوضع الحالي: نهاري فاتح (اضغط لتجربة الوضع الليلي الداكن 🌙)"
                    )
                }

                AppDesignPreset.entries.forEach { preset ->
                    DesignPresetPreviewCard(
                        preset = preset,
                        isSelected = currentPreset == preset,
                        onSelect = { onSelectPreset(preset) }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("اعتماد الديزاين الحالي")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun DesignPresetPreviewCard(
    preset: AppDesignPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val (gradientColors, accentColor, surfaceBg, cornerRadius) = when (preset) {
        AppDesignPreset.EMERALD_GOLD -> QuadPreviewStyle(
            listOf(Color(0xFF065F46), Color(0xFF042F2E), Color(0xFF0F766E)),
            Color(0xFFFBBF24),
            Color(0xFF0B1512),
            22.dp
        )
        AppDesignPreset.SAPPHIRE_FINTECH -> QuadPreviewStyle(
            listOf(Color(0xFF1E3A8A), Color(0xFF0F172A), Color(0xFF0284C7)),
            Color(0xFF38BDF8),
            Color(0xFF090F1D),
            18.dp
        )
        AppDesignPreset.AMETHYST_GLASS -> QuadPreviewStyle(
            listOf(Color(0xFF581C87), Color(0xFF1E1B4B), Color(0xFF9D174D)),
            Color(0xFFFB7185),
            Color(0xFF110A1E),
            26.dp
        )
        AppDesignPreset.MINIMAL_EXECUTIVE -> QuadPreviewStyle(
            listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF334155)),
            Color(0xFFF97316),
            Color(0xFF090A0F),
            10.dp
        )
        AppDesignPreset.WARM_OASIS -> QuadPreviewStyle(
            listOf(Color(0xFF78350F), Color(0xFF365314), Color(0xFF92400E)),
            Color(0xFFFBBF24),
            Color(0xFF17130E),
            20.dp
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) SafeGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(onClick = onSelect)
            .testTag("design_preset_${preset.id}"),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.titleAr,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = preset.subtitleAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SafeGreen.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مُفعّل",
                                style = MaterialTheme.typography.labelSmall,
                                color = SafeGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Live Visual Miniature UI Mockup of this Design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius - 4.dp))
                    .background(surfaceBg)
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Mini Hero Mockup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(cornerRadius - 6.dp))
                            .background(Brush.linearGradient(gradientColors))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (preset) {
                                        AppDesignPreset.SAPPHIRE_FINTECH -> "💳 MIZAN SMART CARD •••• 4829"
                                        AppDesignPreset.AMETHYST_GLASS -> "🔮 حلقة القياس المزدوجة (الحد اليومي + الصحة)"
                                        AppDesignPreset.MINIMAL_EXECUTIVE -> "📊 لوحة القيادة التنفيذية (6 مؤشرات)"
                                        AppDesignPreset.WARM_OASIS -> "🌴 واحة ميزان المالية (توزيع المحافظ)"
                                        else -> "🛡️ المسموح صرفه اليوم (Safe-to-Spend)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "450 ج.م / يومياً",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                gradientColors.forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(c)
                                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                )
                            }
                        }
                    }

                    // Mini KPI Row Mockup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("الدخل +26.5k", "المنصرف -14.2k", "الصافي +12.3k").forEach { itemText ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = itemText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "✨ تخطيط الواجهة: ${preset.layoutDescriptionAr}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class QuadPreviewStyle(
    val gradientColors: List<Color>,
    val accentColor: Color,
    val surfaceBg: Color,
    val cornerRadius: androidx.compose.ui.unit.Dp
)
