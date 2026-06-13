package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Calculator", appName)
  }

  @Test
  fun `test basic digit entry`() {
    val viewModel = CalculatorViewModel()
    viewModel.onDigitPressed("2")
    viewModel.onDigitPressed("5")
    assertEquals("25", viewModel.uiState.value.displayText)
    assertFalse(viewModel.uiState.value.showClearAll)
  }

  @Test
  fun `test simple addition`() {
    val viewModel = CalculatorViewModel()
    viewModel.onDigitPressed("5")
    viewModel.onOperatorPressed(CalculatorOperator.ADD)
    viewModel.onDigitPressed("3")
    viewModel.onEqualPressed()
    assertEquals("8", viewModel.uiState.value.displayText)
  }

  @Test
  fun `test negation`() {
    val viewModel = CalculatorViewModel()
    viewModel.onDigitPressed("9")
    viewModel.onNegatePressed()
    assertEquals("-9", viewModel.uiState.value.displayText)
    viewModel.onNegatePressed()
    assertEquals("9", viewModel.uiState.value.displayText)
  }

  @Test
  fun `test division by zero handles gracefully`() {
    val viewModel = CalculatorViewModel()
    viewModel.onDigitPressed("5")
    viewModel.onOperatorPressed(CalculatorOperator.DIVIDE)
    viewModel.onDigitPressed("0")
    viewModel.onEqualPressed()
    assertEquals("Error", viewModel.uiState.value.displayText)
    assertTrue(viewModel.uiState.value.hasError)
  }

  @Test
  fun `test percent operation`() {
    val viewModel = CalculatorViewModel()
    viewModel.onDigitPressed("2")
    viewModel.onDigitPressed("5")
    viewModel.onPercentPressed()
    assertEquals("0.25", viewModel.uiState.value.displayText)
  }

  @Test
  fun `test backspace swipe functionality`() {
    val viewModel = CalculatorViewModel()
    viewModel.onDigitPressed("1")
    viewModel.onDigitPressed("2")
    viewModel.onDigitPressed("3")
    viewModel.onSwipeBackspace()
    assertEquals("12", viewModel.uiState.value.displayText)
    viewModel.onSwipeBackspace()
    assertEquals("1", viewModel.uiState.value.displayText)
    viewModel.onSwipeBackspace()
    assertEquals("0", viewModel.uiState.value.displayText)
  }
}

