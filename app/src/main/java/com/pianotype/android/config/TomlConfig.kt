package com.pianotype.android.config

import org.tomlj.Toml

object TomlConfig {

    /** PC版 pianotype.toml と互換の TOML をパースする。 */
    fun parse(text: String): Config {
        val result = Toml.parse(text)
        if (result.hasErrors()) {
            val msgs = result.errors().joinToString("\n") { it.toString() }
            throw IllegalArgumentException("Invalid TOML:\n$msgs")
        }
        val device = result.getString("midi_device")
        val handSplit = (result.getLong("hand_split") ?: 60L).toInt()
        val bothHands = result.getBoolean("require_both_hands") ?: true

        val mappings = mutableListOf<Mapping>()
        val arr = result.getArray("mapping")
        if (arr != null) {
            for (i in 0 until arr.size()) {
                val t = arr.getTable(i) ?: continue
                val keysArr = t.getArray("keys") ?: continue
                val keys = (0 until keysArr.size()).mapNotNull { j ->
                    (keysArr.get(j) as? Long)?.toInt()
                }.sorted()
                val value = t.getString("value") ?: continue
                mappings.add(Mapping(keys, value))
            }
        }
        return Config(device, handSplit, bothHands, mappings)
    }

    /** 逆に Config から TOML 文字列を生成する。 */
    fun serialize(config: Config): String = buildString {
        appendLine("# pianotype Android configuration")
        config.midiDevice?.let { appendLine("midi_device = \"${escape(it)}\"") }
        appendLine("hand_split = ${config.handSplit}")
        appendLine("require_both_hands = ${config.requireBothHands}")
        appendLine()
        for (m in config.mapping) {
            appendLine("[[mapping]]")
            appendLine("keys = [${m.keys.joinToString(", ")}]")
            appendLine("value = \"${escape(m.value)}\"")
            appendLine()
        }
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
}