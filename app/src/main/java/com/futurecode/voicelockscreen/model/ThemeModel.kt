package com.futurecode.voicelockscreen.model

import androidx.annotation.DrawableRes

data class ThemeModel(
    val id: Int,
    @DrawableRes val imageResId: Int,
    var isSelected: Boolean = false
)