package com.example.smartcalculator.ui.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun CalculatorScreen(
    onSecretPinDetected: (Boolean) -> Unit // true for real vault, false for decoy
) {
    var display by remember { mutableStateOf("0") }
    var currentNumber by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf<String?>(null) }
    var previousNumber by remember { mutableStateOf<Double?>(null) }
    var secretPinInput by remember { mutableStateOf("") }

    val buttons = listOf(
        listOf("C", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "=")
    )

    fun checkSecretPin(input: String) {
        if (input.endsWith("123+=")) {
            onSecretPinDetected(true)
            secretPinInput = ""
            display = "0"
            currentNumber = ""
            operator = null
            previousNumber = null
        } else if (input.endsWith("456+=")) {
            onSecretPinDetected(false)
            secretPinInput = ""
            display = "0"
            currentNumber = ""
            operator = null
            previousNumber = null
        }

        if (secretPinInput.length > 10) {
            secretPinInput = secretPinInput.takeLast(10)
        }
    }

    fun handleButtonClick(button: String) {
        secretPinInput += button
        checkSecretPin(secretPinInput)

        when (button) {
            "C" -> {
                display = "0"
                currentNumber = ""
                operator = null
                previousNumber = null
            }
            "⌫" -> {
                if (currentNumber.isNotEmpty()) {
                    currentNumber = currentNumber.dropLast(1)
                    display = currentNumber.ifEmpty { "0" }
                }
            }
            in listOf("+", "-", "×", "÷", "%") -> {
                if (currentNumber.isNotEmpty()) {
                    previousNumber = currentNumber.toDoubleOrNull()
                    operator = button
                    currentNumber = ""
                }
            }
            "=" -> {
                if (previousNumber != null && operator != null && currentNumber.isNotEmpty()) {
                    val current = currentNumber.toDoubleOrNull() ?: 0.0
                    val previous = previousNumber ?: 0.0

                    val result = when (operator) {
                        "+" -> previous + current
                        "-" -> previous - current
                        "×" -> previous * current
                        "÷" -> if (current != 0.0) previous / current else 0.0
                        "%" -> previous % current
                        else -> 0.0
                    }

                    val df = DecimalFormat("#.##########")
                    display = df.format(result)
                    currentNumber = display
                    operator = null
                    previousNumber = null
                }
            }
            "." -> {
                if (!currentNumber.contains(".")) {
                    currentNumber = if (currentNumber.isEmpty()) "0." else "$currentNumber."
                    display = currentNumber
                }
            }
            else -> {
                if (display == "0" && currentNumber.isEmpty()) {
                    currentNumber = button
                } else {
                    currentNumber += button
                }
                display = currentNumber
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = display,
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Buttons
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { button ->
                    val buttonColor = when (button) {
                        in listOf("+", "-", "×", "÷", "%", "=") -> Color(0xFFFF9500)
                        in listOf("C", "⌫") -> Color(0xFF505050)
                        else -> Color(0xFF333333)
                    }

                    val weight = if (button == "0") 2f else 1f

                    Button(
                        onClick = { handleButtonClick(button) },
                        modifier = Modifier
                            .weight(weight)
                            .aspectRatio(if (button == "0") 2f else 1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = button,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
