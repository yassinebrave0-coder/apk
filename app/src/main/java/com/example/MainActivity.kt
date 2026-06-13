package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    containerColor = Color.Black
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        CalculatorScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Format display text using thousands commas formatting
    val displayedText = remember(uiState.displayText) {
        formatDisplayForUi(uiState.displayText)
    }

    // Dynamic sizing helper to fit inputs perfectly on one line without overflow
    val fontSize = when {
        displayedText.length <= 5 -> 84.sp
        displayedText.length == 6 -> 74.sp
        displayedText.length == 7 -> 64.sp
        displayedText.length == 8 -> 55.sp
        displayedText.length == 9 -> 48.sp
        displayedText.length == 10 -> 42.sp
        else -> 36.sp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Outer box limits calculator width on wide screens (tablets, foldables)
        // to maintain an ergonomic, visually elegant phone aspect ratio representation.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            
            // Screen display container: contains the running formula history plus main readout input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .pointerInput(Unit) {
                        var accumulatedDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulatedDragX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDragX += dragAmount
                            },
                            onDragEnd = {
                                if (kotlin.math.abs(accumulatedDragX) > 40f) {
                                    viewModel.onSwipeBackspace()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        )
                    }
                    .testTag("display_container"),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom
            ) {
                // Secondary Helper Formula Text matching the "Immersive UI" style
                if (uiState.formulaText.isNotEmpty()) {
                    Text(
                        text = uiState.formulaText,
                        color = Color(0xFFA1A1AA), // zinc-400 equivalent for clear secondary guidance
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .testTag("formula_text")
                    )
                }

                // Main display primary numbers text
                Text(
                    text = displayedText,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = fontSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_text")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad Grid with precision scaling matching physical iOS button aspects
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Compute precise sizing based on exact screen width and standard padding
                val spacing = 14.dp
                val buttonSize = (maxWidth - (spacing * 3)) / 4
                val zeroButtonWidth = (buttonSize * 2) + spacing

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Row 1: AC/C, +/-, %, /
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(
                            text = if (uiState.showClearAll) "AC" else "C",
                            backgroundColor = Color(0xFFA5A5A5),
                            textColor = Color.Black,
                            size = buttonSize,
                            testTag = "clear_button",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onClearPressed()
                            }
                        )
                        CalculatorButton(
                            text = "+/−",
                            backgroundColor = Color(0xFFA5A5A5),
                            textColor = Color.Black,
                            size = buttonSize,
                            testTag = "negate_button",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onNegatePressed()
                            }
                        )
                        CalculatorButton(
                            text = "%",
                            backgroundColor = Color(0xFFA5A5A5),
                            textColor = Color.Black,
                            size = buttonSize,
                            testTag = "percent_button",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onPercentPressed()
                            }
                        )
                        CalculatorButton(
                            text = "÷",
                            backgroundColor = if (uiState.activeOperator == CalculatorOperator.DIVIDE) Color.White else Color(0xFFFE9F0A),
                            textColor = if (uiState.activeOperator == CalculatorOperator.DIVIDE) Color(0xFFFE9F0A) else Color.White,
                            size = buttonSize,
                            testTag = "operator_divide",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onOperatorPressed(CalculatorOperator.DIVIDE)
                            }
                        )
                    }

                    // Row 2: 7, 8, 9, x
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(
                            text = "7",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_7",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("7")
                            }
                        )
                        CalculatorButton(
                            text = "8",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_8",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("8")
                            }
                        )
                        CalculatorButton(
                            text = "9",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_9",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("9")
                            }
                        )
                        CalculatorButton(
                            text = "×",
                            backgroundColor = if (uiState.activeOperator == CalculatorOperator.MULTIPLY) Color.White else Color(0xFFFE9F0A),
                            textColor = if (uiState.activeOperator == CalculatorOperator.MULTIPLY) Color(0xFFFE9F0A) else Color.White,
                            size = buttonSize,
                            testTag = "operator_multiply",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onOperatorPressed(CalculatorOperator.MULTIPLY)
                            }
                        )
                    }

                    // Row 3: 4, 5, 6, -
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(
                            text = "4",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_4",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("4")
                            }
                        )
                        CalculatorButton(
                            text = "5",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_5",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("5")
                            }
                        )
                        CalculatorButton(
                            text = "6",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_6",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("6")
                            }
                        )
                        CalculatorButton(
                            text = "−",
                            backgroundColor = if (uiState.activeOperator == CalculatorOperator.SUBTRACT) Color.White else Color(0xFFFE9F0A),
                            textColor = if (uiState.activeOperator == CalculatorOperator.SUBTRACT) Color(0xFFFE9F0A) else Color.White,
                            size = buttonSize,
                            testTag = "operator_subtract",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onOperatorPressed(CalculatorOperator.SUBTRACT)
                            }
                        )
                    }

                    // Row 4: 1, 2, 3, +
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(
                            text = "1",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_1",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("1")
                            }
                        )
                        CalculatorButton(
                            text = "2",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_2",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("2")
                            }
                        )
                        CalculatorButton(
                            text = "3",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "digit_3",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("3")
                            }
                        )
                        CalculatorButton(
                            text = "+",
                            backgroundColor = if (uiState.activeOperator == CalculatorOperator.ADD) Color.White else Color(0xFFFE9F0A),
                            textColor = if (uiState.activeOperator == CalculatorOperator.ADD) Color(0xFFFE9F0A) else Color.White,
                            size = buttonSize,
                            testTag = "operator_add",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onOperatorPressed(CalculatorOperator.ADD)
                            }
                        )
                    }

                    // Row 5: 0, ., =
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CalculatorButton(
                            text = "0",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            width = zeroButtonWidth,
                            testTag = "digit_0",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDigitPressed("0")
                            }
                        )
                        CalculatorButton(
                            text = ".",
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "decimal_button",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onDecimalPressed()
                            }
                        )
                        CalculatorButton(
                            text = "=",
                            backgroundColor = Color(0xFFFE9F0A),
                            textColor = Color.White,
                            size = buttonSize,
                            testTag = "equal_button",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.onEqualPressed()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HomeIndicator()
        }
    }
}

@Composable
fun CalculatorButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    width: Dp = size,
    testTag: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth background color light-dim when button is held down, emulating native iOS touch sheets
    val displayColor = if (isPressed) {
        if (backgroundColor == Color.White) {
            Color(0xFFE5E5E5) // slightly gray-white
        } else {
            backgroundColor.copy(alpha = 0.6f)
        }
    } else {
        backgroundColor
    }

    // Determine correct rounded layout: Circular standard buttons versus oval capsule '0' button
    val isZeroBtn = text == "0"
    val shape = if (isZeroBtn) RoundedCornerShape(percent = 50) else CircleShape

    Box(
        contentAlignment = if (isZeroBtn) Alignment.CenterStart else Alignment.Center,
        modifier = modifier
            .testTag(testTag)
            .width(width)
            .height(size)
            .background(color = displayColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Disable standard Android ripple to capture high-fidelity iOS fading button response
                onClick = onClick
            )
            .then(
                if (isZeroBtn) {
                    // Perfect visual alignment of text "0" placing it exactly beneath "1" above
                    Modifier.padding(start = size * 0.38f)
                } else {
                    Modifier
                }
            )
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = if (text.length > 2) 23.sp else 31.sp,
            fontWeight = FontWeight.Normal,
            textAlign = if (isZeroBtn) TextAlign.Left else TextAlign.Center
        )
    }
}

@Composable
fun HomeIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(128.dp)
                .height(4.9.dp)
                .background(Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(percent = 100))
        )
    }
}

/**
 * Beautifully formats numbers with thousands separators while preserving typing decimal notations
 */
fun formatDisplayForUi(rawText: String): String {
    if (rawText == "Error") return "Error"
    if (rawText.contains("e")) return rawText

    val dotIdx = rawText.indexOf('.')
    val integerPart = if (dotIdx == -1) rawText else rawText.substring(0, dotIdx)
    val decimalPart = if (dotIdx == -1) "" else rawText.substring(dotIdx)

    val isNegative = integerPart.startsWith("-")
    val absIntegerPart = if (isNegative) integerPart.substring(1) else integerPart

    if (!absIntegerPart.all { it.isDigit() }) return rawText

    val formattedAbsInteger = if (absIntegerPart.isEmpty()) {
        ""
    } else {
        absIntegerPart.reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    }

    val finalInteger = if (isNegative && formattedAbsInteger.isNotEmpty()) "-$formattedAbsInteger" else if (isNegative) "-" else formattedAbsInteger
    return finalInteger + decimalPart
}
