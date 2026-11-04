package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppDesignPreset
import com.example.data.AppUpdateManager
import com.example.data.DefaultFinanceCatalog
import com.example.data.FinanceAnalyticsEngine
import com.example.data.FinanceRepository
import com.example.ui.AppUpdateCenterDialog
import com.example.ui.BudgetsBurnRateScreen
import com.example.ui.CommitmentsAndGoalsScreen
import com.example.ui.ComprehensiveSettingsDialog
import com.example.ui.DailyLimitControlDialog
import com.example.ui.DashboardScreen
import com.example.ui.DesignStudioDialog
import com.example.ui.FinanceViewModel
import com.example.ui.ForecastSimulatorExcelScreen
import com.example.ui.PinLockOverlay
import com.example.ui.SecurityAndEncryptionDialog
import com.example.ui.TransactionsScreen
import com.example.ui.UiState
import com.example.ui.theme.DangerRose
import com.example.ui.theme.MizanFinanceTheme
import com.example.ui.theme.SafeGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import android.content.Intent
import com.example.data.MizanHomeWidgetProvider
import com.example.ui.QuickCalculatorAddSheetDialog
import com.example.ui.SmartPaymentAndIntegrationsHubDialog

class MainActivity : ComponentActivity() {
    private val widgetActionState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetActionState.value = intent?.getStringExtra(MizanHomeWidgetProvider.EXTRA_WIDGET_ACTION)
        enableEdgeToEdge()
        setContent {
            AppRoot(
                initialWidgetAction = widgetActionState.value,
                onConsumeWidgetAction = { widgetActionState.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetActionState.value = intent.getStringExtra(MizanHomeWidgetProvider.EXTRA_WIDGET_ACTION)
    }
}

private data class NavTabItem(
    val index: Int,
    val title: String,
    val icon: ImageVector,
    val tag: String
)

private val MainNavTabs = listOf(
    NavTabItem(0, "الرئيسية", Icons.Default.Dashboard, "nav_tab_dashboard"),
    NavTabItem(1, "المعاملات", Icons.AutoMirrored.Filled.ReceiptLong, "nav_tab_transactions"),
    NavTabItem(2, "الميزانية والالتزامات", Icons.Default.Savings, "nav_tab_budgets"),
    NavTabItem(3, "التحليلات والأدوات", Icons.Default.AutoGraph, "nav_tab_forecast")
)

@Composable
fun AppRoot(
    initialWidgetAction: String? = null,
    onConsumeWidgetAction: () -> Unit = {}
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val effectiveUserId = currentUser?.uid ?: FinanceRepository.OFFLINE_USER_ID

    val financeViewModel: FinanceViewModel = viewModel(
        key = effectiveUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                FinanceViewModel(FinanceRepository(app), effectiveUserId)
            }
        }
    )

    val isDark by financeViewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isPinLocked by financeViewModel.isPinLocked.collectAsStateWithLifecycle()
    val appSettings by financeViewModel.appSettings.collectAsStateWithLifecycle()
    val currentDesignPreset = remember(appSettings.designPresetId) {
        AppDesignPreset.fromId(appSettings.designPresetId)
    }

    MizanFinanceTheme(
        darkTheme = isDark,
        designPreset = currentDesignPreset
    ) {
        if (isPinLocked) {
            PinLockOverlay(
                onVerifyPin = { pin -> financeViewModel.unlockWithPin(pin) }
            )
        } else {
            AuthenticatedFinanceShell(
                viewModel = financeViewModel,
                isCloudSignedIn = currentUser != null,
                cloudEmail = currentUser?.email,
                onCloudAuthStateChanged = { currentUser = Firebase.auth.currentUser },
                initialWidgetAction = initialWidgetAction,
                onConsumeWidgetAction = onConsumeWidgetAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedFinanceShell(
    viewModel: FinanceViewModel,
    isCloudSignedIn: Boolean,
    cloudEmail: String?,
    onCloudAuthStateChanged: () -> Unit,
    initialWidgetAction: String? = null,
    onConsumeWidgetAction: () -> Unit = {}
) {
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDesignStudioDialog by remember { mutableStateOf(false) }
    var showDailyLimitControlDialog by remember { mutableStateOf(false) }
    var showWhatsAppDailyReportDialog by remember { mutableStateOf(false) }
    var showUpdateCenterDialog by remember { mutableStateOf(false) }
    var showQuickCalculatorDialog by remember { mutableStateOf(false) }
    var showSmartIntegrationsHubDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialWidgetAction) {
        when (initialWidgetAction) {
            MizanHomeWidgetProvider.ACTION_QUICK_ADD_TX -> {
                showQuickCalculatorDialog = true
                onConsumeWidgetAction()
            }
            MizanHomeWidgetProvider.ACTION_OPEN_WHATSAPP_REPORT -> {
                showWhatsAppDailyReportDialog = true
                onConsumeWidgetAction()
            }
        }
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isPrivacyMasked by viewModel.isPrivacyMasked.collectAsStateWithLifecycle()
    val isPinConfigured by viewModel.isPinConfigured.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val currentDesignPreset = remember(appSettings.designPresetId) {
        AppDesignPreset.fromId(appSettings.designPresetId)
    }

    val txState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val budgetsState by viewModel.budgetsState.collectAsStateWithLifecycle()
    val templatesState by viewModel.templatesState.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategoriesState.collectAsStateWithLifecycle()
    val incomeCategories by viewModel.incomeCategoriesState.collectAsStateWithLifecycle()

    val commitments by viewModel.commitmentsState.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoalsState.collectAsStateWithLifecycle()
    val assetHoldings by viewModel.assetHoldingsState.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterPriority by viewModel.filterPriority.collectAsStateWithLifecycle()
    val filterWallet by viewModel.filterWallet.collectAsStateWithLifecycle()

    val forecastMode by viewModel.forecastMode.collectAsStateWithLifecycle()
    val luxuryCutPercent by viewModel.luxuryCutPercent.collectAsStateWithLifecycle()
    val selectedSimCategory by viewModel.selectedSimCategory.collectAsStateWithLifecycle()
    val categoryCutPercent by viewModel.categoryCutPercent.collectAsStateWithLifecycle()

    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val isAlertDanger by viewModel.isAlertDanger.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val hasAvailableUpdate = availableUpdate != null &&
        (availableUpdate?.latestVersionCode ?: 0) > AppUpdateManager.currentVersionCode

    // Handle back button on secondary tabs to return to Dashboard (Tab 0)
    BackHandler(enabled = selectedTab != 0) {
        viewModel.selectTab(0)
    }

    val transactions = remember(txState) {
        (txState as? UiState.Success)?.data ?: DefaultFinanceCatalog.buildSeedTransactions()
    }
    val budgets = remember(budgetsState) {
        val list = (budgetsState as? UiState.Success)?.data
        if (!list.isNullOrEmpty()) list else DefaultFinanceCatalog.buildSeedBudgets()
    }
    val templates = remember(templatesState) {
        val list = (templatesState as? UiState.Success)?.data
        if (!list.isNullOrEmpty()) list else DefaultFinanceCatalog.buildSeedTemplates()
    }

    val dashboardMetrics = remember(transactions, budgets, appSettings) {
        FinanceAnalyticsEngine.computeDashboardMetrics(
            transactions = transactions,
            budgets = budgets,
            appSettings = appSettings
        )
    }
    val burnRates = remember(transactions, budgets) {
        FinanceAnalyticsEngine.computeBurnRates(transactions, budgets)
    }
    val forecastRows = remember(transactions, budgets, forecastMode) {
        FinanceAnalyticsEngine.computeForecastTable(transactions, budgets, forecastMode)
    }

    if (showSecurityDialog) {
        SecurityAndEncryptionDialog(
            isCloudSignedIn = isCloudSignedIn,
            cloudEmail = cloudEmail,
            isPinConfigured = isPinConfigured,
            onConfigurePin = viewModel::configureAppPin,
            onLockAppNow = viewModel::lockAppNow,
            onDismiss = { showSecurityDialog = false },
            onCloudSignInSuccess = onCloudAuthStateChanged,
            onCloudSignOut = onCloudAuthStateChanged
        )
    }

    if (showDesignStudioDialog) {
        DesignStudioDialog(
            currentPreset = currentDesignPreset,
            isDarkTheme = isDark,
            onSelectPreset = { preset ->
                viewModel.selectDesignPreset(preset)
            },
            onToggleDarkTheme = viewModel::toggleDarkTheme,
            onDismiss = { showDesignStudioDialog = false }
        )
    }

    if (showDailyLimitControlDialog) {
        DailyLimitControlDialog(
            appSettings = appSettings,
            metrics = dashboardMetrics,
            onSaveDailySpendControl = viewModel::updateDailySpendControl,
            onSetTotalMonthlyBudget = viewModel::setTotalMonthlyBudgetAndDistribute,
            onZeroAllBudgets = viewModel::zeroAllCategoryBudgets,
            onNavigateToBudgetsTab = { viewModel.selectTab(2) },
            onDismiss = { showDailyLimitControlDialog = false }
        )
    }

    if (showWhatsAppDailyReportDialog) {
        com.example.ui.WhatsAppDailyReportDialog(
            appSettings = appSettings,
            transactions = transactions,
            metrics = dashboardMetrics,
            onSaveWhatsAppSettings = viewModel::updateWhatsAppDailyReportSettings,
            onTestBackgroundSend = viewModel::testSendBackgroundWhatsAppNow,
            onDismiss = { showWhatsAppDailyReportDialog = false }
        )
    }

    if (showUpdateCenterDialog) {
        AppUpdateCenterDialog(
            appSettings = appSettings,
            availableUpdate = availableUpdate,
            isCheckingUpdate = isCheckingUpdate,
            onCheckForUpdatesNow = { viewModel.checkForAppUpdates(manualTrigger = true) },
            onSaveUpdateSettings = viewModel::saveUpdateCenterSettings,
            onSimulateUpdatePreview = viewModel::simulateNewUpdatePreview,
            onDismiss = { showUpdateCenterDialog = false }
        )
    }

    if (showQuickCalculatorDialog) {
        QuickCalculatorAddSheetDialog(
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            onAddTransaction = viewModel::addTransactionWithInstantCheck,
            onRecordWalletTransfer = viewModel::recordWalletTransfer,
            onDismiss = { showQuickCalculatorDialog = false }
        )
    }

    if (showSmartIntegrationsHubDialog) {
        SmartPaymentAndIntegrationsHubDialog(
            appSettings = appSettings,
            metrics = dashboardMetrics,
            burnRates = burnRates,
            commitments = commitments,
            onAddTransaction = viewModel::addTransactionWithInstantCheck,
            onToggleAutoCaptureNotifications = viewModel::toggleAutoCaptureBankNotifications,
            onSaveGoogleSheetsSettings = viewModel::saveGoogleSheetsSettings,
            onSyncGoogleSheetsNow = viewModel::syncAllDataToGoogleSheetsNow,
            onDismiss = { showSmartIntegrationsHubDialog = false }
        )
    }

    if (showSettingsDialog) {
        ComprehensiveSettingsDialog(
            appSettings = appSettings,
            isDarkTheme = isDark,
            isPrivacyMasked = isPrivacyMasked,
            isPinConfigured = isPinConfigured,
            isCloudSignedIn = isCloudSignedIn,
            cloudEmail = cloudEmail,
            transactions = transactions,
            budgets = budgets,
            templates = templates,
            commitments = commitments,
            savingsGoals = savingsGoals,
            assetHoldings = assetHoldings,
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            metrics = dashboardMetrics,
            burnRates = burnRates,
            forecastRows = forecastRows,
            forecastMode = forecastMode,
            onUpdateAppSettings = viewModel::updateAppSettings,
            onToggleDarkTheme = viewModel::toggleDarkTheme,
            onTogglePrivacyMask = viewModel::togglePrivacyMask,
            onOpenSecurityAndPinDialog = {
                showSettingsDialog = false
                showSecurityDialog = true
            },
            onAddCustomCategory = viewModel::addCustomCategory,
            onDeleteCustomCategory = viewModel::deleteCustomCategory,
            onImportBackupOrCsvContent = viewModel::importCsvContent,
            onRefreshLiveRates = viewModel::refreshLiveCurrencyAndGoldRates,
            onOpenUpdateCenter = {
                showSettingsDialog = false
                showUpdateCenterDialog = true
            },
            onClearAllData = viewModel::clearAllDataAndStartFresh,
            onResetSeedData = viewModel::resetAndPopulateSeedData,
            onDismiss = { showSettingsDialog = false }
        )
    }

    var budgetsSubSection by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ميزان المالي",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isCloudSignedIn) "متصل سحابياً • مشفر" else "حفظ محلي آمن • مشفر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // 1. إخفاء أو إظهار الأرقام بضغطة واحدة
                        IconButton(
                            onClick = { viewModel.togglePrivacyMask() },
                            modifier = Modifier.testTag("toggle_privacy_mask_button")
                        ) {
                            Icon(
                                imageVector = if (isPrivacyMasked) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "إخفاء أو إظهار الأرقام السرية",
                                tint = if (isPrivacyMasked) DangerRose else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 2. المظهر والألوان (أبيض وبيج / ليلي)
                        IconButton(
                            onClick = { showDesignStudioDialog = true },
                            modifier = Modifier.testTag("open_design_studio_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "تخصيص الألوان والمظهر",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 3. مركز التحديث السحابي (GitHub Releases) بضغطة واحدة
                        IconButton(
                            onClick = { showUpdateCenterDialog = true },
                            modifier = Modifier.testTag("open_update_center_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "مركز التحديث السحابي GitHub Releases",
                                tint = if (hasAvailableUpdate) SafeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 4. الإعدادات الشاملة والأمان والتحديثات
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("open_comprehensive_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "الإعدادات الشاملة والأمان",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (selectedTab == 0 || selectedTab == 1) {
                    ExtendedFloatingActionButton(
                        onClick = { showQuickCalculatorDialog = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "إضافة معاملة سريعة"
                            )
                        },
                        text = {
                            Text(
                                text = "إضافة معاملة",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("fab_quick_add_transaction")
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        MainNavTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab.index,
                                onClick = { viewModel.selectTab(tab.index) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = {
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedTab == tab.index) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainNavTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab.index,
                                onClick = { viewModel.selectTab(tab.index) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // شريط التحديثات المتاحة إن وجد
                    AnimatedVisibility(visible = hasAvailableUpdate && availableUpdate != null) {
                        val upd = availableUpdate
                        if (upd != null) {
                            Surface(
                                color = SafeGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                                    .testTag("new_update_available_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "تحديث جديد متاح: v${upd.latestVersionName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SafeGreen
                                        )
                                        Text(
                                            text = "يحفظ جميع معاملاتك وبياناتك تلقائياً",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { showUpdateCenterDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                                        modifier = Modifier.testTag("banner_open_update_button")
                                    ) {
                                        Text(
                                            text = "تحديث",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!upd.isMandatoryUpdate) {
                                        IconButton(onClick = { viewModel.dismissAvailableUpdateBanner() }) {
                                            Icon(Icons.Default.Close, contentDescription = "إغلاق إشعار التحديث")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = bannerMessage != null) {
                        val msg = bannerMessage ?: ""
                        Surface(
                            color = (if (isAlertDanger) DangerRose else SafeGreen).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlertDanger) DangerRose else SafeGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.dismissBanner() }) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق التنبيه")
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> DashboardScreen(
                                metrics = dashboardMetrics,
                                transactions = transactions,
                                burnRates = burnRates,
                                savingsGoals = savingsGoals,
                                commitments = commitments,
                                isPrivacyMasked = isPrivacyMasked,
                                designPreset = currentDesignPreset,
                                simpleMinimalistUiMode = appSettings.simpleMinimalistUiMode,
                                whatsappPhoneNumber = appSettings.whatsappPhoneNumber,
                                whatsappMidnightAutoEnabled = appSettings.whatsappMidnightAutoEnabled,
                                whatsappScheduleLabelAr = com.example.data.WhatsAppMidnightScheduler.formatScheduleTimeAr(
                                    appSettings.whatsappReportHour,
                                    appSettings.whatsappReportMinute
                                ),
                                hasAvailableUpdate = hasAvailableUpdate,
                                availableUpdateVersionName = availableUpdate?.latestVersionName ?: "",
                                onSelectDesignPreset = viewModel::selectDesignPreset,
                                onToggleMinimalistUiMode = viewModel::toggleSimpleMinimalistUiMode,
                                onOpenDesignStudio = { showDesignStudioDialog = true },
                                onOpenDailyLimitControl = { showDailyLimitControlDialog = true },
                                onOpenWhatsAppDailyReport = { showWhatsAppDailyReportDialog = true },
                                onOpenUpdateCenter = { showUpdateCenterDialog = true },
                                onOpenQuickCalculator = { showQuickCalculatorDialog = true },
                                onOpenSmartIntegrationsHub = { showSmartIntegrationsHubDialog = true },
                                onNavigateToPersonalDebts = {
                                    budgetsSubSection = 1
                                    viewModel.selectTab(2)
                                },
                                onPayCommitmentInstallment = viewModel::payCommitmentInstallment,
                                onQuickNavigateToTransactions = { viewModel.selectTab(1) },
                                onResetSeedData = { viewModel.resetAndPopulateSeedData() },
                                onClearAllData = { viewModel.clearAllDataAndStartFresh() }
                            )
                            1 -> TransactionsScreen(
                                transactions = transactions,
                                templates = templates,
                                expenseCategories = expenseCategories,
                                incomeCategories = incomeCategories,
                                isPrivacyMasked = isPrivacyMasked,
                                searchQuery = searchQuery,
                                filterType = filterType,
                                filterPriority = filterPriority,
                                filterWallet = filterWallet,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onFilterTypeChange = viewModel::updateFilterType,
                                onFilterPriorityChange = viewModel::updateFilterPriority,
                                onFilterWalletChange = viewModel::updateFilterWallet,
                                onRecordQuickTemplate = viewModel::recordQuickTemplate,
                                onAddTransaction = viewModel::addTransactionWithInstantCheck,
                                onDeleteTransaction = viewModel::deleteTransaction,
                                onAddQuickTemplate = viewModel::addQuickTemplate,
                                onAddCustomCategory = viewModel::addCustomCategory,
                                onRecordWalletTransfer = viewModel::recordWalletTransfer,
                                onAutoGenerateFixedTransactions = viewModel::autoGenerateMonthlyFixedTransactions
                            )
                            2 -> Column(modifier = Modifier.fillMaxSize()) {
                                // شريط تبديل علوي أنيق باللون البيج يدمج الميزانيات مع الجمعيات والديون والأهداف
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = budgetsSubSection == 0,
                                            onClick = { budgetsSubSection = 0 },
                                            label = {
                                                Text(
                                                    text = "الميزانيات ومعدل الحرق",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    fontWeight = if (budgetsSubSection == 0) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = budgetsSubSection == 1,
                                            onClick = { budgetsSubSection = 1 },
                                            label = {
                                                Text(
                                                    text = "الجمعيات والديون والأهداف",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    fontWeight = if (budgetsSubSection == 1) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("nav_tab_commitments")
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    if (budgetsSubSection == 0) {
                                        BudgetsBurnRateScreen(
                                            burnRates = burnRates,
                                            elapsedDaysInMonth = dashboardMetrics.elapsedDaysInMonth,
                                            totalDaysInMonth = dashboardMetrics.totalDaysInMonth,
                                            onUpdateCategoryBudget = viewModel::updateCategoryBudget
                                        )
                                    } else {
                                        CommitmentsAndGoalsScreen(
                                            metrics = dashboardMetrics,
                                            transactions = transactions,
                                            commitments = commitments,
                                            savingsGoals = savingsGoals,
                                            assetHoldings = assetHoldings,
                                            isPrivacyMasked = isPrivacyMasked,
                                            onAddCommitment = viewModel::addCommitment,
                                            onPayCommitmentInstallment = viewModel::payCommitmentInstallment,
                                            onDeleteCommitment = viewModel::deleteCommitment,
                                            onAddSavingsGoal = viewModel::addSavingsGoal,
                                            onDepositIntoGoal = viewModel::depositIntoSavingsGoal,
                                            onDeleteSavingsGoal = viewModel::deleteSavingsGoal,
                                            onUpsertAssetHolding = viewModel::upsertAssetHolding,
                                            onRefreshLiveRates = viewModel::refreshLiveCurrencyAndGoldRates,
                                            onRecordSpiritualGiving = viewModel::recordSpiritualGiving,
                                            defaultSpiritualGivingMode = appSettings.defaultSpiritualGivingMode,
                                            defaultUseGrossIncomeForTithe = appSettings.useGrossIncomeForOrthodoxTithe
                                        )
                                    }
                                }
                            }
                            else -> ForecastSimulatorExcelScreen(
                                transactions = transactions,
                                metrics = dashboardMetrics,
                                burnRates = burnRates,
                                forecastRows = forecastRows,
                                forecastMode = forecastMode,
                                luxuryCutPercent = luxuryCutPercent,
                                selectedSimCategory = selectedSimCategory,
                                categoryCutPercent = categoryCutPercent,
                                commitments = commitments,
                                onSelectForecastMode = viewModel::setForecastMode,
                                onLuxuryCutChange = viewModel::setLuxuryCutPercent,
                                onSelectSimCategory = viewModel::setSelectedSimCategory,
                                onCategoryCutChange = viewModel::setCategoryCutPercent,
                                onImportCsvContent = viewModel::importCsvContent,
                                onOpenSmartIntegrationsHub = { showSmartIntegrationsHubDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
