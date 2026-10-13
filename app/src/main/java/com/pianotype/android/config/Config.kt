package com.pianotype.android.config

data class Mapping(
    val keys: List<Int>,
    val value: String,
)

data class Config(
    val midiDevice: String? = null,
    val handSplit: Int = 60,
    val requireBothHands: Boolean = true,
    val mapping: List<Mapping> = emptyList(),
)