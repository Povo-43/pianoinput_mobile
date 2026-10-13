package com.pianotype.android.config

data class Mapping(
    val keys: List<Int>,
    val value: String,
)

data class Config(
    val midiDevice: String? = null,
    val midiChannel: Int? = null,   // 1..16, null = 全チャンネル
    val handSplit: Int = 60,
    val requireBothHands: Boolean = true,
    val mapping: List<Mapping> = emptyList(),
)