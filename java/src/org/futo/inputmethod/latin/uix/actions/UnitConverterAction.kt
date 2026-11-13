package org.futo.inputmethod.latin.uix.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.math.BigDecimal

// Hexboard modification: an offline unit converter that opens in place of the keyboard.

/** Which pair of units each category starts on: from, to. Mirrors UnitConverter.CATEGORIES. */
private val DefaultUnitPairs = listOf(
    Pair(3, 7), // Length: mi -> km
    Pair(1, 4), // Weight: lb -> kg
    Pair(0, 1), // Temp: °F -> °C
    Pair(3, 7), // Volume: cup -> mL
    Pair(0, 4), // Area: sq ft -> m²
    Pair(0, 3), // Speed: mph -> km/h
    Pair(1, 2), // Time: min -> hr
)

private object ConverterMemory {
    var category by mutableStateOf(0)
    var from by mutableStateOf(DefaultUnitPairs[0].first)
    var to by mutableStateOf(DefaultUnitPairs[0].second)
    var input by mutableStateOf("")
}

private val ConverterKeys = listOf(
    listOf("1", "2", "3", "4", "5", "6", "⌫"),
    listOf("7", "8", "9", "0", ".", "±", "C"),
)

private fun pressConverterKey(key: String) {
    val mem = ConverterMemory
    var text = mem.input
    when (key) {
        "C" -> text = ""
        "⌫" -> text = text.dropLast(1)
        "±" -> text = if (text.startsWith("-")) text.drop(1) else "-$text"
        "." -> if (!text.contains('.')) text += if (text.isEmpty() || text == "-") "0." else "."
        else -> if (text.length < 15) text += key
    }
    mem.input = text
}

@Composable
private fun ConverterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = LocalKeyboardScheme.current
    val shape = RoundedCornerShape(50)
    Surface(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) scheme.primary else scheme.keyboardContainer,
        contentColor = if (selected) scheme.onPrimary else scheme.onKeyboardContainer
    ) {
        Text(label, fontSize = 15.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
    }
}

@Composable
private fun ConverterChipRow(title: String, labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val scheme = LocalKeyboardScheme.current
    Row(modifier = Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
        if (title.isNotEmpty()) {
            Text(title, fontSize = 12.sp, color = scheme.onSurfaceVariant, modifier = Modifier.width(38.dp))
        }
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labels.forEachIndexed { i, label ->
                ConverterChip(label, i == selected) { onSelect(i) }
            }
        }
    }
}

@Composable
private fun UnitConverterScreen(manager: KeyboardManagerForAction) {
    val view = LocalView.current
    val context = LocalContext.current
    val scheme = LocalKeyboardScheme.current
    val mem = ConverterMemory

    val hexagonal = remember {
        try { context.getSettingBlocking(HexKeysSetting) } catch (_: Exception) { true }
    }
    val keyShape: Shape = if (hexagonal) HexagonShape else RoundedCornerShape(10.dp)

    val categories = UnitConverter.CATEGORIES
    val category = categories[mem.category.coerceIn(0, categories.size - 1)]
    val units = category.units.toList()
    val from = mem.from.coerceIn(0, units.size - 1)
    val to = mem.to.coerceIn(0, units.size - 1)

    val value: BigDecimal? = try { BigDecimal(mem.input) } catch (_: Exception) { null }
    val result: String? = value?.let {
        try {
            CalculatorEngine.format(category.convert(it, from, to), 8).replace('-', '−')
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun feedback() {
        manager.performHapticAndAudioFeedback(Constants.CODE_TAB, view)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = mem.input.replace('-', '−').ifEmpty { "0" } + " " + units[from],
                    fontSize = 20.sp,
                    maxLines = 1,
                    color = scheme.onSurface
                )
                Text(
                    text = "= " + (result ?: "…") + " " + units[to],
                    fontSize = 18.sp,
                    maxLines = 1,
                    color = scheme.primary
                )
            }
            CalculatorButton("⇄", Modifier.size(width = 48.dp, height = 46.dp), RoundedCornerShape(10.dp), false) {
                feedback()
                val oldFrom = mem.from
                mem.from = mem.to
                mem.to = oldFrom
            }
            CalculatorButton("Insert", Modifier.size(width = 72.dp, height = 46.dp), RoundedCornerShape(10.dp), true) {
                feedback()
                if (result != null) manager.typeText(result.replace('−', '-') + " " + units[to])
            }
        }

        ConverterChipRow("", categories.map { it.name }, mem.category) { i ->
            feedback()
            mem.category = i
            mem.from = DefaultUnitPairs[i].first
            mem.to = DefaultUnitPairs[i].second
        }
        ConverterChipRow("From", units, from) { i ->
            feedback()
            mem.from = i
        }
        ConverterChipRow("To", units, to) { i ->
            feedback()
            mem.to = i
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 2.dp)) {
            val rows = ConverterKeys.size
            val columns = ConverterKeys[0].size
            val cellWidth = if (hexagonal) maxWidth / (columns + 0.5f) else maxWidth / columns.toFloat()
            val cellHeight = if (hexagonal) maxHeight / (rows * 0.75f + 0.25f) else maxHeight / rows.toFloat()
            val rowPitch = if (hexagonal) cellHeight * 0.75f else cellHeight

            ConverterKeys.forEachIndexed { r, row ->
                row.forEachIndexed { c, key ->
                    val x = cellWidth * c.toFloat() + if (hexagonal && r % 2 == 1) cellWidth / 2f else 0.dp
                    val y = rowPitch * r.toFloat()
                    CalculatorButton(
                        label = key,
                        modifier = Modifier.offset(x = x, y = y).size(width = cellWidth, height = cellHeight),
                        shape = keyShape,
                        emphasized = false
                    ) {
                        feedback()
                        pressConverterKey(key)
                    }
                }
            }
        }
    }
}

val UnitConverterAction = Action(
    icon = R.drawable.unit_converter,
    name = R.string.action_unit_converter_title,
    simplePressImpl = null,
    persistentState = null,
    canShowKeyboard = false,
    windowImpl = { manager, _ ->
        object : ActionWindow() {
            @Composable
            override fun windowName(): String {
                return stringResource(R.string.action_unit_converter_title)
            }

            @Composable
            override fun WindowContents(keyboardShown: Boolean) {
                UnitConverterScreen(manager)
            }
        }
    }
)
