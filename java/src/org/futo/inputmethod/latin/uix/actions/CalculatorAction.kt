package org.futo.inputmethod.latin.uix.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.futo.inputmethod.keyboard.HexKeysSetting
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.ActionWindow
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.LocalKeyboardScheme
import org.futo.inputmethod.latin.uix.getSettingBlocking

// Hexboard modification: a calculator that opens in place of the keyboard.

/** Kept outside the window so the sum is still there when the calculator is reopened. */
private object CalculatorMemory {
    var expression by mutableStateOf("")
    var showingResult by mutableStateOf(false)
    var error by mutableStateOf("")
}

private const val OPERATORS = "+−×÷"

private val CalculatorKeys = listOf(
    listOf("C", "(", ")", "÷"),
    listOf("7", "8", "9", "×"),
    listOf("4", "5", "6", "−"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "%", "="),
)

/** Pointy-top hexagon filling its bounds, matching the hexagonal keyboard keys. */
private val HexagonShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height * 0.25f)
    lineTo(size.width, size.height * 0.75f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height * 0.75f)
    lineTo(0f, size.height * 0.25f)
    close()
}

/** The value of [expression] as text, or null when there is nothing sensible to show yet. */
private fun calculate(expression: String): Pair<String?, String> {
    return try {
        Pair(CalculatorEngine.format(CalculatorEngine.evaluate(expression)), "")
    } catch (e: CalculatorEngine.CalcException) {
        Pair(null, if (e.incomplete) "" else (e.message ?: ""))
    } catch (e: ArithmeticException) {
        Pair(null, "Can't work that out")
    }
}

private fun pressKey(key: String) {
    val mem = CalculatorMemory
    mem.error = ""
    when (key) {
        "C" -> {
            mem.expression = ""
            mem.showingResult = false
        }
        "=" -> {
            val (result, error) = calculate(mem.expression)
            if (result != null) {
                mem.expression = result.replace('-', '−')
                mem.showingResult = true
            } else {
                mem.error = error.ifEmpty { if (mem.expression.isEmpty()) "" else "Not finished yet" }
            }
        }
        else -> {
            val isOperator = key.length == 1 && OPERATORS.contains(key)
            var text = mem.expression
            // A digit typed straight after a result starts a new sum; an operator carries on from it.
            if (mem.showingResult && !isOperator && key != "%") text = ""
            mem.showingResult = false

            if (isOperator) {
                if (text.isEmpty()) {
                    if (key != "−") return
                } else if (OPERATORS.contains(text.last())) {
                    // Swap the operator rather than stacking two, except for a minus sign after × or ÷.
                    val last = text.last()
                    if (key == "−" && (last == '×' || last == '÷')) {
                        // keep both: "6×−2"
                    } else {
                        text = text.dropLast(1)
                        if (text.isNotEmpty() && OPERATORS.contains(text.last())) text = text.dropLast(1)
                        if (text.isEmpty() && key != "−") return
                    }
                }
            }
            if (text.length < 80) mem.expression = text + key
        }
    }
}

private fun backspace() {
    val mem = CalculatorMemory
    mem.error = ""
    mem.showingResult = false
    if (mem.expression.isNotEmpty()) mem.expression = mem.expression.dropLast(1)
}

@Composable
private fun CalculatorButton(
    label: String,
    modifier: Modifier,
    shape: Shape,
    emphasized: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalKeyboardScheme.current
    Surface(
        modifier = modifier
            .padding(2.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (emphasized) scheme.primary else scheme.keyboardContainer,
        contentColor = if (emphasized) scheme.onPrimary else scheme.onKeyboardContainer
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = if (label.length > 1) 15.sp else 22.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CalculatorScreen(manager: KeyboardManagerForAction) {
    val view = LocalView.current
    val context = LocalContext.current
    val scheme = LocalKeyboardScheme.current
    val mem = CalculatorMemory

    val hexagonal = remember {
        try { context.getSettingBlocking(HexKeysSetting) } catch (_: Exception) { true }
    }
    val buttonShape: Shape = if (hexagonal) HexagonShape else RoundedCornerShape(10.dp)

    val expression = mem.expression
    val (liveResult, liveError) = remember(expression) { calculate(expression) }
    val isPlainNumber = expression.none { OPERATORS.contains(it) || it == '%' || it == '(' }
    val secondLine = when {
        mem.error.isNotEmpty() -> mem.error
        liveError.isNotEmpty() -> liveError
        liveResult != null && !isPlainNumber && !mem.showingResult -> "= " + liveResult.replace('-', '−')
        else -> ""
    }

    fun feedback() {
        manager.performHapticAndAudioFeedback(Constants.CODE_TAB, view)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                val scroll = rememberScrollState()
                LaunchedEffect(expression) { scroll.scrollTo(scroll.maxValue) }
                Text(
                    text = expression.ifEmpty { "0" },
                    fontSize = 26.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = scheme.onSurface,
                    modifier = Modifier.horizontalScroll(scroll)
                )
                Text(
                    text = secondLine,
                    fontSize = 15.sp,
                    maxLines = 1,
                    color = scheme.onSurfaceVariant
                )
            }

            CalculatorButton(
                label = "⌫",
                modifier = Modifier.padding(start = 6.dp).size(width = 52.dp, height = 48.dp),
                shape = RoundedCornerShape(10.dp),
                emphasized = false
            ) {
                feedback()
                backspace()
            }

            CalculatorButton(
                label = "Insert",
                modifier = Modifier.size(width = 76.dp, height = 48.dp),
                shape = RoundedCornerShape(10.dp),
                emphasized = true
            ) {
                feedback()
                // Type the answer if there is one, otherwise whatever is on the display.
                val text = (liveResult ?: expression).replace('−', '-')
                if (text.isNotEmpty()) manager.typeText(text)
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val rows = CalculatorKeys.size
            val columns = CalculatorKeys[0].size

            // Hexagons: odd rows sit half a button to the right and rows overlap by a quarter,
            // so the buttons interlock like the keyboard's keys.
            val cellWidth = if (hexagonal) maxWidth / (columns + 0.5f) else maxWidth / columns.toFloat()
            val cellHeight = if (hexagonal) maxHeight / (rows * 0.75f + 0.25f) else maxHeight / rows.toFloat()
            val rowPitch = if (hexagonal) cellHeight * 0.75f else cellHeight

            CalculatorKeys.forEachIndexed { r, row ->
                row.forEachIndexed { c, key ->
                    val x = cellWidth * c.toFloat() + if (hexagonal && r % 2 == 1) cellWidth / 2f else 0.dp
                    val y = rowPitch * r.toFloat()
                    CalculatorButton(
                        label = key,
                        modifier = Modifier.offset(x = x, y = y).size(width = cellWidth, height = cellHeight),
                        shape = buttonShape,
                        emphasized = key == "="
                    ) {
                        feedback()
                        pressKey(key)
                    }
                }
            }
        }
    }
}

val CalculatorAction = Action(
    icon = R.drawable.calculator,
    name = R.string.action_calculator_title,
    simplePressImpl = null,
    persistentState = null,
    canShowKeyboard = false,
    windowImpl = { manager, _ ->
        object : ActionWindow() {
            @Composable
            override fun windowName(): String {
                return stringResource(R.string.action_calculator_title)
            }

            @Composable
            override fun WindowContents(keyboardShown: Boolean) {
                CalculatorScreen(manager)
            }
        }
    }
)
