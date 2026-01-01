package com.example.smartcalculator

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartcalculator.ui.calculator.CalculatorScreen
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

                    if (biometricUtils.isBiometricAvailable()) {
                        biometricUtils.authenticate(
                            title = "Unlock ${if (isRealVault) "Private" else ""} Gallery",
                            subtitle = "Use your fingerprint or device credentials",
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

            GalleryScreen(
                isDecoyVault = isDecoyVault,
                files = files,
                onBack = {
                    currentVaultType = null
                    navController.popBackStack()
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
                    // TODO: Open file viewer
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
    }
}