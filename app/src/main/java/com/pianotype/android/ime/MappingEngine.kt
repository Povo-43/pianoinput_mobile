package com.pianotype.android.ime

import com.pianotype.android.config.Config
import com.pianotype.android.config.Mapping

/**
 * PC版 pianotype の run() 内のロジックをそのまま移植。
 * スレッドセーフにしたいので synchronized で保護。
 */
class MappingEngine(
    private val configProvider: () -> Config,
    private val onToggle: (Boolean) -> Unit,
    private val onShiftArmed: () -> Unit,
    private val onAction: (value: String, shifted: Boolean) -> Unit,
) {
    private val held = HashSet<Int>()
    private val emitted = HashSet<List<Int>>()
    private var enabled = false
    private var shiftArmed = false

    @Synchronized
    fun onNoteOn(note: Int) {
        held.add(note)
        val current = held.sorted()
        if (current.isNotEmpty() && !emitted.contains(current)) {
            val mapping = findMapping(configProvider(), current)
            if (mapping != null) {
                emitted.add(current)
                val value = mapping.value
                when (value.uppercase()) {
                    "TOGGLE" -> {
                        enabled = !enabled
                        shiftArmed = false
                        onToggle(enabled)
                    }
                    "SHIFT" -> if (enabled) {
                        shiftArmed = true
                        onShiftArmed()
                    }
                    else -> if (enabled) {
                        val s = shiftArmed
                        shiftArmed = false
                        onAction(value, s)
                    }
                }
            }
        }
    }

    @Synchronized
    fun onNoteOff(note: Int) {
        held.remove(note)
        emitted.clear()
    }

    @Synchronized
    fun reset() {
        held.clear()
        emitted.clear()
        shiftArmed = false
    }

    @Synchronized
    fun isEnabled(): Boolean = enabled

    private fun findMapping(config: Config, heldNotes: List<Int>): Mapping? {
        val heldSet = heldNotes.toSet()
        return config.mapping.firstOrNull { m ->
            if (m.keys.toSet() != heldSet) return@firstOrNull false
            if (config.requireBothHands) {
                m.keys.any { it < config.handSplit } && m.keys.any { it >= config.handSplit }
            } else true
        }
    }
}