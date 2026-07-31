package com.my.vpn

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.my.vpn.data.local.AppSettingsStorage
import com.my.vpn.util.LocaleHelper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.vpn.ui.screens.AppMenuScreen
import com.my.vpn.ui.screens.ConfigPickerScreen
import com.my.vpn.ui.screens.HelpScreen
import com.my.vpn.ui.screens.MainScreen
import com.my.vpn.ui.screens.OnboardingScreen
import com.my.vpn.ui.screens.menu.MenuAdvancedScreen
import com.my.vpn.ui.screens.menu.MenuAppearanceScreen
import com.my.vpn.ui.screens.menu.MenuAppUpdateScreen
import com.my.vpn.ui.screens.menu.MenuSubscriptionsScreen
import com.my.vpn.ui.theme.AppThemeMode
import com.my.vpn.ui.theme.MyVPNTheme
import com.my.vpn.ui.viewmodel.AppScreen
import com.my.vpn.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleHelper.wrap(newBase, AppSettingsStorage.readLanguageBlocking(newBase))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MyVpnApp
        lifecycleScope.launch {
            viewModel.requestActivityRecreate.collectLatest {
                recreate()
            }
        }
        setContent {
            val uiScaleLevel by app.appSettingsStorage.uiScaleLevelFlow.collectAsStateWithLifecycle(
                initialValue = com.my.vpn.ui.theme.UI_SCALE_LEVEL_DEFAULT
            )
            val themeMode by app.appSettingsStorage.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = AppThemeMode.DARK_STANDARD
            )
            MyVPNTheme(uiScaleLevel = uiScaleLevel, themeMode = themeMode) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                BackHandler {
                    viewModel.handleSystemBack()
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                val vpnPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        viewModel.onVpnPermissionGranted()
                    } else {
                        viewModel.onVpnPermissionDenied()
                    }
                }
                LaunchedEffect(state.vpnPermissionIntent) {
                    state.vpnPermissionIntent?.let { vpnPermissionLauncher.launch(it) }
                }

                when (state.currentScreen) {
                    AppScreen.Onboarding -> OnboardingScreen(
                        step = state.onboardingStep,
                        onNext = {
                            if (state.onboardingStep == 1 &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                notificationPermissionLauncher.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                            viewModel.onboardingNext()
                        },
                        onComplete = viewModel::completeOnboarding,
                        onOpenDeveloper = { openDeveloperGitHub() },
                        onOpenInspiration = { openInspirationRepo() }
                    )
                    AppScreen.Main -> MainScreen(viewModel = viewModel)
                    AppScreen.ConfigPicker -> ConfigPickerScreen(
                        viewModel = viewModel,
                        onBack = viewModel::closeConfigPicker
                    )
                    AppScreen.Menu -> AppMenuScreen(
                        viewModel = viewModel,
                        state = state,
                        onBack = viewModel::closeAppMenu,
                        onExit = { finishAndRemoveTask() }
                    )
                    AppScreen.MenuAppearance -> MenuAppearanceScreen(
                        viewModel = viewModel,
                        state = state,
                        onBack = viewModel::closeMenuAppearance
                    )
                    AppScreen.MenuSubscriptions -> MenuSubscriptionsScreen(
                        viewModel = viewModel,
                        state = state,
                        onBack = viewModel::closeMenuSubscriptions
                    )
                    AppScreen.MenuAppUpdate -> MenuAppUpdateScreen(
                        viewModel = viewModel,
                        state = state,
                        onBack = viewModel::closeMenuAppUpdate
                    )
                    AppScreen.MenuAdvanced -> MenuAdvancedScreen(
                        viewModel = viewModel,
                        state = state,
                        onBack = viewModel::closeMenuAdvanced
                    )
                    AppScreen.Help -> HelpScreen(onBack = viewModel::closeHelp)
                    AppScreen.Bypass -> AppMenuScreen(
                        viewModel = viewModel,
                        state = state,
                        onBack = viewModel::closeAppMenu,
                        onExit = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }

    private fun openDeveloperGitHub() {
        openUrl("https://github.com/${AppConstants.APP_GITHUB_OWNER}")
    }

    private fun openInspirationRepo() {
        openUrl(AppConstants.CONFIG_INSPIRATION_URL)
    }

    private fun openUrl(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForeground()
    }
}
