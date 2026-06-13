package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

enum class CalculatorOperator(val symbol: String) {
    ADD("+"),
    SUBTRACT("−"),
    MULTIPLY("×"),
    DIVIDE("÷")
}

data class CalculatorUiState(
    val displayText: String = "0",
    val activeOperator: CalculatorOperator? = null,
    val showClearAll: Boolean = true, // true means AC, false means C
    val hasError: Boolean = false,
    val formulaText: String = ""
)

class CalculatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    private var firstOperand: BigDecimal? = null
    private var pendingOperator: CalculatorOperator? = null
    private var isEnteringNewNumber: Boolean = false
    private var previousSecondOperand: BigDecimal? = null
    private var previousOperator: CalculatorOperator? = null

    private fun computeFormulaText(firstOp: BigDecimal?, op: CalculatorOperator?, secondOpStr: String?): String {
        if (firstOp == null || op == null) return ""
        val formattedFirst = formatDisplayForUi(firstOp.toPlainString())
        val formattedSecond = if (secondOpStr != null) {
            formatDisplayForUi(secondOpStr)
        } else {
            ""
        }
        return if (formattedSecond.isNotEmpty()) {
            "$formattedFirst ${op.symbol} $formattedSecond"
        } else {
            "$formattedFirst ${op.symbol}"
        }
    }

    fun onDigitPressed(digit: String) {
        if (_uiState.value.hasError) {
            resetAll()
        }

        val currentText = _uiState.value.displayText
        val newText = if (isEnteringNewNumber || currentText == "0") {
            isEnteringNewNumber = false
            digit
        } else {
            // Respect maximum character inputs (like iOS limit of 9 digits in display)
            val digitCount = currentText.count { it.isDigit() }
            if (digitCount >= 9) {
                return // Limit reached
            }
            currentText + digit
        }

        val formula = computeFormulaText(firstOperand, pendingOperator, newText)
        _uiState.value = _uiState.value.copy(
            displayText = newText,
            showClearAll = false,
            formulaText = formula
        )
    }

    fun onDecimalPressed() {
        if (_uiState.value.hasError) {
            resetAll()
        }

        val currentText = _uiState.value.displayText
        val newText = if (isEnteringNewNumber) {
            isEnteringNewNumber = false
            "0."
        } else if (currentText.contains(".")) {
            return
        } else {
            "$currentText."
        }

        val formula = computeFormulaText(firstOperand, pendingOperator, newText)
        _uiState.value = _uiState.value.copy(
            displayText = newText,
            showClearAll = false,
            formulaText = formula
        )
    }

    fun onOperatorPressed(operator: CalculatorOperator) {
        if (_uiState.value.hasError) return

        val currentDisplayBD = try {
            BigDecimal(_uiState.value.displayText)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        var displayVal = _uiState.value.displayText

        if (firstOperand != null && pendingOperator != null && !isEnteringNewNumber) {
            // Perform intermediate calculation
            val result = calculateResult(firstOperand!!, currentDisplayBD, pendingOperator!!)
            if (result == null) {
                _uiState.value = _uiState.value.copy(displayText = "Error", hasError = true, formulaText = "")
                return
            }
            firstOperand = result
            displayVal = formatBigDecimal(result)
        } else {
            firstOperand = currentDisplayBD
        }

        pendingOperator = operator
        isEnteringNewNumber = true
        
        // Reset chaining parameters upon setting new operator
        previousSecondOperand = null
        previousOperator = null

        val formula = computeFormulaText(firstOperand, operator, null)
        _uiState.value = _uiState.value.copy(
            displayText = displayVal,
            activeOperator = operator,
            formulaText = formula
        )
    }

    fun onEqualPressed() {
        if (_uiState.value.hasError) return

        val currentDisplayBD = try {
            BigDecimal(_uiState.value.displayText)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        val op1: BigDecimal
        val op2: BigDecimal
        val op: CalculatorOperator

        if (pendingOperator != null) {
            op1 = firstOperand ?: BigDecimal.ZERO
            op2 = currentDisplayBD
            op = pendingOperator!!
            
            // Save for button repeating (= chaining)
            previousSecondOperand = op2
            previousOperator = op
        } else if (previousOperator != null && previousSecondOperand != null) {
            // iOS chain behavior: pressing equals repeatedly applies last operator and second operand
            op1 = currentDisplayBD
            op2 = previousSecondOperand!!
            op = previousOperator!!
        } else {
            return // Nothing to calculate
        }

        val result = calculateResult(op1, op2, op)
        if (result == null) {
            _uiState.value = _uiState.value.copy(displayText = "Error", hasError = true, formulaText = "")
            return
        }

        val resultStr = formatBigDecimal(result)
        val formattedFirst = formatDisplayForUi(op1.toPlainString())
        val formattedSecond = formatDisplayForUi(op2.toPlainString())
        val finalFormula = "$formattedFirst ${op.symbol} $formattedSecond"

        firstOperand = result
        _uiState.value = _uiState.value.copy(
            displayText = resultStr,
            activeOperator = null,
            showClearAll = true,
            formulaText = finalFormula
        )

        // Clear only the pending operator so repeat equal continues to work
        pendingOperator = null
        isEnteringNewNumber = true
    }

    fun onClearPressed() {
        if (_uiState.value.showClearAll) {
            resetAll()
        } else {
            // Clear current input only (C behavior)
            val formula = computeFormulaText(firstOperand, pendingOperator, "0")
            _uiState.value = _uiState.value.copy(
                displayText = "0",
                showClearAll = true,
                formulaText = formula
            )
            isEnteringNewNumber = false
        }
    }

    fun onNegatePressed() {
        if (_uiState.value.hasError) return

        val currentText = _uiState.value.displayText
        val newText = if (currentText == "0") {
            return
        } else if (currentText.startsWith("-")) {
            currentText.substring(1)
        } else {
            "-$currentText"
        }

        val formula = computeFormulaText(firstOperand, pendingOperator, newText)
        _uiState.value = _uiState.value.copy(
            displayText = newText,
            formulaText = formula
        )
    }

    fun onPercentPressed() {
        if (_uiState.value.hasError) return

        val currentVal = try {
            BigDecimal(_uiState.value.displayText)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }

        val result = currentVal.divide(BigDecimal("100"), MathContext.DECIMAL128)
        val resultStr = formatBigDecimal(result)
        val formula = computeFormulaText(firstOperand, pendingOperator, resultStr)
        _uiState.value = _uiState.value.copy(
            displayText = resultStr,
            formulaText = formula
        )
        isEnteringNewNumber = true
    }

    fun onSwipeBackspace() {
        if (_uiState.value.hasError || isEnteringNewNumber) return

        val currentText = _uiState.value.displayText
        // iOS swipe gesture backspace deletes the last character. If 1 char remains or just "-", resets to "0"
        val newText = if (currentText.length <= 1 || (currentText.length == 2 && currentText.startsWith("-"))) {
            "0"
        } else {
            currentText.dropLast(1)
        }

        val formula = computeFormulaText(firstOperand, pendingOperator, newText)
        _uiState.value = _uiState.value.copy(
            displayText = newText,
            showClearAll = newText == "0",
            formulaText = formula
        )
    }

    private fun resetAll() {
        firstOperand = null
        pendingOperator = null
        isEnteringNewNumber = false
        previousSecondOperand = null
        previousOperator = null
        _uiState.value = CalculatorUiState()
    }

    private fun calculateResult(op1: BigDecimal, op2: BigDecimal, operator: CalculatorOperator): BigDecimal? {
        return try {
            when (operator) {
                CalculatorOperator.ADD -> op1.add(op2, MathContext.DECIMAL128)
                CalculatorOperator.SUBTRACT -> op1.subtract(op2, MathContext.DECIMAL128)
                CalculatorOperator.MULTIPLY -> op1.multiply(op2, MathContext.DECIMAL128)
                CalculatorOperator.DIVIDE -> {
                    if (op2.compareTo(BigDecimal.ZERO) == 0) {
                        null // Zero division
                    } else {
                        op1.divide(op2, MathContext.DECIMAL128)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatBigDecimal(value: BigDecimal): String {
        val stripped = value.stripTrailingZeros()
        val plainString = stripped.toPlainString()
        val absVal = stripped.abs()
        
        // Switch to scientific notation if overflow or minuscule decimal
        if (absVal.compareTo(BigDecimal("999999999")) > 0 || 
            (absVal.compareTo(BigDecimal("0.000001")) < 0 && absVal.compareTo(BigDecimal.ZERO) > 0)) {
            val sciStr = String.format(Locale.US, "%.5e", stripped.toDouble())
            return sciStr.replace("e+", "e").replace("e0", "e")
        }

        if (plainString.length <= 9) {
            return plainString
        }

        val dotIdx = plainString.indexOf('.')
        if (dotIdx == -1) {
            val sciStr = String.format(Locale.US, "%.5e", stripped.toDouble())
            return sciStr.replace("e+", "e").replace("e0", "e")
        }

        val allowFractionalDigits = 9 - 1 - dotIdx
        if (allowFractionalDigits <= 0) {
            return plainString.substring(0, dotIdx)
        }

        val rounded = stripped.setScale(allowFractionalDigits, RoundingMode.HALF_UP).stripTrailingZeros()
        val roundedStr = rounded.toPlainString()
        return if (roundedStr.length > 9) {
            roundedStr.substring(0, 9).removeSuffix(".")
        } else {
            roundedStr
        }
    }
}
