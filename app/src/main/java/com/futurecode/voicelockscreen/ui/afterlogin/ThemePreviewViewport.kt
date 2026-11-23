package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.widget.FrameLayout
import kotlin.math.roundToInt

/** Fits the shared lock-screen content into the space between the preview overlays. */
internal class ThemePreviewViewport(context: Context) : FrameLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        getChildAt(0).measure(
            MeasureSpec.makeMeasureSpec((width - paddingLeft - paddingRight).coerceAtLeast(0), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val content = getChildAt(0)
        val availableHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        val scale = (availableHeight.toFloat() / content.measuredHeight.coerceAtLeast(1)).coerceAtMost(1f)
        val contentTop = paddingTop + ((availableHeight - content.measuredHeight * scale) / 2).roundToInt()
        content.layout(paddingLeft, contentTop, paddingLeft + content.measuredWidth, contentTop + content.measuredHeight)
        content.pivotX = content.measuredWidth / 2f
        content.pivotY = 0f
        content.scaleX = scale
        content.scaleY = scale
    }
}
