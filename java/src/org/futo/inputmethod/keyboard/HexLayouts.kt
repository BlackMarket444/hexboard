package org.futo.inputmethod.keyboard

import androidx.datastore.preferences.core.booleanPreferencesKey
import org.futo.inputmethod.latin.uix.SettingsKey

/**
 * Hexboard modification: when this setting is on, letter and symbol layouts are re-flowed onto a
 * hexagonal grid by [org.futo.inputmethod.v2keyboard.LayoutEngine] and drawn with hexagonal keys by
 * [KeyboardView]. It works for any layout, whatever the number of keys in each row.
 */
val HexKeysSetting = SettingsKey(booleanPreferencesKey("hexboard_hex_keys"), true)
