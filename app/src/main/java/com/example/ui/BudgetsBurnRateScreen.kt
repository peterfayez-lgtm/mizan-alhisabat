package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BurnRateStatus
import com.example.ui.theme.DangerRose
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.WarningAmber

@Composable
fun BudgetsBurnRateScreen(
    burnRates: List<BurnRateStatus>,
    elapsedDaysInMonth: Int,
    totalDaysInMonth: Int,
    onUpdateCategoryBudget: (String, Double) -> Unit
) {
    var editingStatus by remember { mutableStateOf<BurnRateStatus?>(null) }
    val monthPct = ((elapsedDaysInMonth.toFloat() / totalDaysInMonth.coerceAtLeast(1)) * 100).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_burn_rate_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Burn Rate Overview Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "مؤشر سرعة الحرق",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رادار الميزانيات ومؤشر سرعة الحرق (Burn Rate)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "انقضى $elapsedDaysInMonth من $totalDaysInMonth يوماً في الشهر ($monthPct%). يقارن المؤشر نسبة صرف كل فئة بنسبة الأيام المنقضية لينبهك مبكراً قبل نفاد الميزانية.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. Category Budget Cards with Color-Coded Progress Bars & Burn Rate Alerts
        items(burnRates, key = { it.category }) { status ->
            BudgetBurnRateCard(
                status = status,
                onEditClick = { editingStatus = status }
            )
        }
    }

    if (editingStatus != null) {
        val current = editingStatus!!
        var limitText by remember(current) { mutableStateOf(current.limit.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { editingStatus = null },
            title = { Text("تعديل سقف ميزانية «${current.category}»", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("السقف الشهري الجديد (ج.م)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLim = limitText.toDoubleOrNull() ?: current.limit
                        onUpdateCategoryBudget(current.category, newLim)
                        editingStatus = null
                    }
                ) {
                    Text("حفظ السقف")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStatus = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun BudgetBurnRateCard(
    status: BurnRateStatus,
    onEditClick: () -> Unit
) {
    val barColor: Color = when (status.statusLevel) {
        "exceeded" -> DangerRose
        "warning" -> WarningAmber
        else -> SafeGreen
    }
    val spentPct = (status.spentRatio * 100).toInt()
    val elapsedPct = (status.monthElapsedRatio * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (status.statusLevel) {
                            "exceeded", "warning" -> Icons.Default.WarningAmber
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = barColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = status.category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المنصرف: ${status.spent.toInt()} ج.م من سقف ${status.limit.toInt()} ج.م",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = barColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "$spentPct%",
                            style = MaterialTheme.typography.titleMedium,
                            color = barColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    FilledTonalIconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل سقف الميزانية",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Progress bar (Green <80%, Orange 80..99%, Red >=100%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(status.spentRatio.toFloat().coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(barColor)
                )
            }

            // Early Burn Rate Warning when spending velocity exceeds elapsed days of the month
            if (status.isBurningFast || status.statusLevel == "exceeded") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (status.statusLevel == "exceeded") DangerRose else WarningAmber).copy(alpha = 0.16f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "تحذير سرعة الحرق",
                            tint = if (status.statusLevel == "exceeded") DangerRose else WarningAmber
                        )
                        Text(
                            text = if (status.statusLevel == "exceeded") {
                                "تجاوزت ميزانية «${status.category}» بالكامل ($spentPct%) قبل انتهاء الشهر!"
                            } else {
                                "تحذير سرعة الحرق: صرفت $spentPct% من ميزانية «${status.category}» بينما انقضى $elapsedPct% فقط من الشهر! ستنفد الميزانية خلال ${status.estimatedDaysUntilDepleted} أيام بهذا المعدل."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (status.statusLevel == "exceeded") DangerRose else WarningAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
