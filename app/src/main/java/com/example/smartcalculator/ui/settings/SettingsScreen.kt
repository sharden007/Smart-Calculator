package com.example.smartcalculator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentRealPin: String,
    currentDecoyPin: String,
    onBack: () -> Unit,
    onUpdatePins: (realPin: String, decoyPin: String) -> Unit
) {
    var realPin by remember { mutableStateOf(currentRealPin) }
    var decoyPin by remember { mutableStateOf(currentDecoyPin) }
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Secret PIN Patterns",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C2C2C)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Real Vault PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = realPin,
                        onValueChange = { realPin = it },
                        label = { Text("Enter PIN pattern") },
                        placeholder = { Text("e.g., 123+=") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9500),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFF9500),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Text(
                        "This PIN opens your real private vault",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C2C2C)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Decoy Vault PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = decoyPin,
                        onValueChange = { decoyPin = it },
                        label = { Text("Enter PIN pattern") },
                        placeholder = { Text("e.g., 456+=") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9500),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFFFF9500),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Text(
                        "This PIN opens a fake vault with harmless photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9500)
                )
            ) {
                Text("Save Changes", modifier = Modifier.padding(8.dp))
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C2C2C).copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "How to use:",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        "• Type your secret PIN pattern on the calculator",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "• The calculator works normally for everyone else",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        "• Use the decoy vault if someone forces you to open",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save PIN Changes") },
            text = { Text("Are you sure you want to update your PIN patterns? Make sure to remember them!") },
            confirmButton = {
                TextButton(onClick = {
                    onUpdatePins(realPin, decoyPin)
                    showSaveDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
