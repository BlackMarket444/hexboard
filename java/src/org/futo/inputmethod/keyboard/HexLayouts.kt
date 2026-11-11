package org.futo.inputmethod.keyboard

import android.content.Context
import android.os.SystemClock
import org.futo.inputmethod.latin.uix.settings.pages.getCustomLayouts

/**
 * Hexboard modification: decides which layouts are drawn with hexagonal keys.
 *
 * Bundled layouts opt in through their file name (`hex*.yaml`). Layouts written in the in-app YAML
 * editor are stored as `custom0`, `custom1`, ... so they opt in through their `name:` line instead:
 * a name starting with "Hex" (any capitalisation) turns hexagons on.
 */
object HexLayouts {
    private const val CACHE_MS = 1500L
    private val cache = HashMap<String, Pair<Long, Boolean>>()

    @JvmStatic
    fun isHexLayout(context: Context, layoutName: String?): Boolean {
        if (layoutName == null) return false
        if (layoutName.startsWith("hex")) return true
        if (!layoutName.startsWith("custom")) return false

        val now = SystemClock.uptimeMillis()
        synchronized(cache) {
            cache[layoutName]?.let { (time, value) -> if (now - time < CACHE_MS) return value }
        }

        val result = try {
            val index = layoutName.removePrefix("custom").toInt()
            val yaml = getCustomLayouts(context)[index].layoutYaml
            val nameLine = yaml.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("name:") }
            nameLine != null && nameLine.removePrefix("name:").trim().trim('"', '\'')
                .lowercase().startsWith("hex")
        } catch (_: Exception) {
            false
        }

        synchronized(cache) { cache[layoutName] = Pair(now, result) }
        return result
    }
}
