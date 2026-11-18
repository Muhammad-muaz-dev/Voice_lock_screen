package com.futurecode.voicelockscreen.model

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val country: String = "",
    val variants: List<LanguageOption> = emptyList()
)
