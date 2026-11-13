package org.futo.inputmethod.latin.uix.actions

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.delay
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.ActionTextEditor
import org.futo.inputmethod.latin.uix.ActionWindow
import org.futo.inputmethod.latin.uix.DialogRequestItem
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.LocalKeyboardScheme
import org.futo.inputmethod.latin.uix.SettingsKey
import org.futo.inputmethod.latin.uix.getSettingBlocking
import org.futo.inputmethod.latin.uix.setSetting
import org.futo.inputmethod.latin.uix.setSettingBlocking

// Hexboard modification: a notepad that sits above the keyboard. Notes are kept on the device in the
// keyboard's own settings storage.

/** All notes in one string, separated by the ASCII record separator. */
val NotepadNotesSetting = SettingsKey(stringPreferencesKey("hexboard_notepad_notes"), "")

private const val NOTE_SEPARATOR = '\u001E'

private object NotepadMemory {
    var notes by mutableStateOf(listOf(""))
    var index by mutableStateOf(0)
    private var loaded = false

    fun loadOnce(context: Context) {
        if (loaded) return
        loaded = true
        val saved = try { context.getSettingBlocking(NotepadNotesSetting) } catch (_: Exception) { "" }
        val list = saved.split(NOTE_SEPARATOR)
        notes = if (list.isEmpty()) listOf("") else list
        index = notes.size - 1
    }

    fun setCurrent(text: String) {
        if (index in notes.indices && notes[index] != text) {
            notes = notes.toMutableList().also { it[index] = text.replace(NOTE_SEPARATOR, ' ') }
        }
    }

    fun serialize(): String = notes.joinToString(NOTE_SEPARATOR.toString())
}

@Composable
private fun NotepadScreen(manager: KeyboardManagerForAction) {
    val context = LocalContext.current
    val view = LocalView.current
    val scheme = LocalKeyboardScheme.current
    val mem = NotepadMemory

    val text = remember {
        mem.loadOnce(context)
        mutableStateOf(mem.notes[mem.index.coerceIn(0, mem.notes.size - 1)])
    }

    // Keep the stored copy in step with the editor, saving shortly after typing stops.
    LaunchedEffect(text.value) {
        mem.setCurrent(text.value)
        delay(500L)
        context.setSetting(NotepadNotesSetting, mem.serialize())
    }
    DisposableEffect(Unit) {
        onDispose {
            mem.setCurrent(text.value)
            try { context.setSettingBlocking(NotepadNotesSetting.key, mem.serialize()) } catch (_: Exception) { }
        }
    }

    fun feedback() {
        manager.performHapticAndAudioFeedback(Constants.CODE_TAB, view)
    }

    fun show(i: Int) {
        mem.setCurrent(text.value)
        mem.index = i.coerceIn(0, mem.notes.size - 1)
        text.value = mem.notes[mem.index]
    }

    val buttonShape = RoundedCornerShape(10.dp)
    val buttonHeight: Dp = 40.dp

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(buttonHeight), verticalAlignment = Alignment.CenterVertically) {
            CalculatorButton("‹", Modifier.size(44.dp, buttonHeight), buttonShape, false) {
                feedback()
                if (mem.index > 0) show(mem.index - 1)
            }
            Text(
                text = "${mem.index + 1} / ${mem.notes.size}",
                fontSize = 15.sp,
                color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            CalculatorButton("›", Modifier.size(44.dp, buttonHeight), buttonShape, false) {
                feedback()
                if (mem.index < mem.notes.size - 1) show(mem.index + 1)
            }

            Spacer(modifier = Modifier.weight(1f))

            CalculatorButton("New", Modifier.size(56.dp, buttonHeight), buttonShape, false) {
                feedback()
                mem.setCurrent(text.value)
                // Reuse an empty note rather than piling up blanks.
                val blank = mem.notes.indexOfFirst { it.isBlank() }
                if (blank >= 0) {
                    show(blank)
                } else {
                    mem.notes = mem.notes + ""
                    show(mem.notes.size - 1)
                }
            }
            CalculatorButton("Delete", Modifier.size(68.dp, buttonHeight), buttonShape, false) {
                feedback()
                val remove = {
                    val list = mem.notes.toMutableList()
                    if (mem.index in list.indices) list.removeAt(mem.index)
                    mem.notes = if (list.isEmpty()) listOf("") else list
                    mem.index = mem.index.coerceIn(0, mem.notes.size - 1)
                    text.value = mem.notes[mem.index]
                }
                if (text.value.isBlank()) {
                    remove()
                } else {
                    manager.requestDialog(
                        "Delete this note?",
                        listOf(
                            DialogRequestItem("Cancel") { },
                            DialogRequestItem("Delete") { remove() }
                        ),
                        { }
                    )
                }
            }
            CalculatorButton("Insert", Modifier.size(68.dp, buttonHeight), buttonShape, true) {
                feedback()
                if (text.value.isNotEmpty()) manager.typeText(text.value)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 4.dp),
            shape = RoundedCornerShape(10.dp),
            color = scheme.keyboardContainer,
            contentColor = scheme.onKeyboardContainer
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                ActionTextEditor(
                    text = text,
                    multiline = true,
                    autocorrect = true,
                    placeholder = "Write a note"
                )
            }
        }
    }
}

val NotepadAction = Action(
    icon = R.drawable.notepad,
    name = R.string.action_notepad_title,
    simplePressImpl = null,
    persistentState = null,
    canShowKeyboard = true,
    windowImpl = { manager, _ ->
        object : ActionWindow() {
            override val onlyShowAboveKeyboard: Boolean
                get() = true

            override val fixedWindowHeight: Dp?
                get() = 190.dp

            @Composable
            override fun windowName(): String {
                return stringResource(R.string.action_notepad_title)
            }

            @Composable
            override fun WindowContents(keyboardShown: Boolean) {
                NotepadScreen(manager)
            }
        }
    }
)
