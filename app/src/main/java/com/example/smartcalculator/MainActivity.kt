package com.example.smartcalculator

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.smartcalculator.ui.calculator.CalculatorScreen
import com.example.smartcalculator.ui.gallery.FileViewerScreen
import com.example.smartcalculator.ui.gallery.GalleryScreen
import com.example.smartcalculator.ui.gallery.GalleryViewModel
import com.example.smartcalculator.ui.settings.SettingsScreen
import com.example.smartcalculator.ui.theme.SmartCalculatorTheme
import com.example.smartcalculator.utils.BiometricUtils
import com.example.smartcalculator.utils.SettingsManager

class MainActivity : FragmentActivity() {

    private lateinit var biometricUtils: BiometricUtils
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        biometricUtils = BiometricUtils(this)
        settingsManager = SettingsManager(this)

        setContent {
            SmartCalculatorTheme {
                SmartCalculatorApp(
                    biometricUtils = biometricUtils,
                    settingsManager = settingsManager
                )
            }
        }
    }
}

@Composable
fun SmartCalculatorApp(
    biometricUtils: BiometricUtils,
    settingsManager: SettingsManager
) {
    val navController = rememberNavController()
    val galleryViewModel: GalleryViewModel = viewModel()

    val realVaultFiles by galleryViewModel.realVaultFiles.collectAsState()
    val decoyVaultFiles by galleryViewModel.decoyVaultFiles.collectAsState()

    var currentVaultType by remember { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = "calculator",
        modifier = Modifier.fillMaxSize()
    ) {
        composable("calculator") {
            CalculatorScreen(
                onSecretPinDetected = { isRealVault ->
                    currentVaultType = if (isRealVault) "real" else "decoy"

                    if (biometricUtils.isPinAvailable()) {
                        biometricUtils.authenticateWithPin(
                            title = "Unlock ${if (isRealVault) "Private" else ""} Gallery",
                            subtitle = "Enter your device PIN to continue",
                            onSuccess = {
                                navController.navigate("gallery")
                            },
                            onError = { error ->
                                currentVaultType = null
                            },
                            onFailed = {
                                currentVaultType = null
                            }
                        )
                    } else {
                        navController.navigate("gallery")
                    }
                }
            )
        }

        composable("gallery") {
            val isDecoyVault = currentVaultType == "decoy"
            val files = if (isDecoyVault) decoyVaultFiles else realVaultFiles

            // Clear vault type when leaving this screen
            DisposableEffect(Unit) {
                onDispose {
                    currentVaultType = null
                }
            }

            GalleryScreen(
                isDecoyVault = isDecoyVault,
                files = files,
                onBack = {
                    navController.popBackStack("calculator", inclusive = false)
                },
                onAddFile = { uri ->
                    currentVaultType?.let { vaultType ->
                        galleryViewModel.addFile(uri, vaultType)
                    }
                },
                onDeleteFile = { file ->
                    galleryViewModel.deleteFile(file)
                },
                onFileClick = { file ->
                    navController.navigate("fileViewer/${file.id}")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                currentRealPin = settingsManager.realPin,
                currentDecoyPin = settingsManager.decoyPin,
                onBack = {
                    navController.popBackStack()
                },
                onUpdatePins = { realPin, decoyPin ->
                    settingsManager.realPin = realPin
                    settingsManager.decoyPin = decoyPin
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "fileViewer/{fileId}",
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) { backStackEntry ->
            val fileId = backStackEntry.arguments?.getLong("fileId") ?: return@composable
            val allFiles = realVaultFiles + decoyVaultFiles
            val file = allFiles.find { it.id == fileId }

            file?.let {
                FileViewerScreen(
                    file = it,
                    onBack = {
                        navController.popBackStack()
                    },
                    getDecryptedFile = { encryptedFile ->
                        galleryViewModel.getDecryptedFile(encryptedFile)
                    }
                )
            }
        }
    }
}