package com.futurecode.voicelockscreen.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.futurecode.voicelockscreen.R
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.abs
import android.graphics.RectF

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onPatternComplete: ((String) -> Unit)? = null

    private var setupAppearance = false
    private var onWallpaper = false
    private val selected = mutableListOf<Int>()
    private val centers = MutableList(9) { PointF() }
    private var currentX = 0f
    private var currentY = 0f
    private var drawing = false
    private var isErrorState = false

    // Dimensions
    private var outerCircleRadius = 0f
    private var innerDotRadius = 0f
    private var activeDotRadius = 0f
    private var cardCornerRadius = 0f
    private val cardRect = RectF()

    private val setupRingColor = ContextCompat.getColor(context, R.color.textTertiary)
    private val setupDotColor = ContextCompat.getColor(context, R.color.primary)
    private val wallpaperRingColor = ContextCompat.getColor(context, R.color.white)
    private val wallpaperNodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.scrim)
    }

    // Default appearance
    private val cardBgColor = Color.parseColor("#FFFFFF")
    private val outerFillColor = ContextCompat.getColor(context, R.color.primary_light)
    private val outerStrokeColor = ContextCompat.getColor(context, R.color.primaryOutline)
    private val innerDotColor = ContextCompat.getColor(context, R.color.primary)
    private val activeLineColor = ContextCompat.getColor(context, R.color.primary)
    private val errorColor = Color.parseColor("#EF4444")

    // Paints
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = cardBgColor
        style = Paint.Style.FILL
    }

    private val outerCircleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outerFillColor
        style = Paint.Style.FILL
    }

    private val outerCircleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = outerStrokeColor
        style = Paint.Style.STROKE
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerDotColor
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeLineColor
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = w.toFloat()

        cardRect.set(0f, 0f, size, size)
        cardCornerRadius = size * 0.10f

        val padding = size * if (setupAppearance) 0f else 0.10f
        val usableSize = size - (padding * 2)
        val cell = usableSize / 3f

        outerCircleRadius = cell * if (setupAppearance) 0.275f else 0.36f
        innerDotRadius = cell * if (setupAppearance) 0.105f else 0.10f
        activeDotRadius = cell * 0.12f

        outerCircleStrokePaint.strokeWidth = if (setupAppearance) resources.displayMetrics.density else cell * 0.035f
        linePaint.strokeWidth = cell * 0.07f

        for (row in 0 until 3) {
            for (col in 0 until 3) {
                centers[row * 3 + col].set(
                    padding + cell * col + cell / 2f,
                    padding + cell * row + cell / 2f
                )
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!setupAppearance) {
            canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardPaint)
        }

        val activeColor = if (isErrorState) errorColor else activeLineColor
        linePaint.color = activeColor
        drawLines(canvas)

        centers.forEachIndexed { index, point ->
            val isSelected = index in selected
            if (!setupAppearance || isSelected) {
                canvas.drawCircle(point.x, point.y, outerCircleRadius, outerCircleFillPaint)
            }
            outerCircleStrokePaint.color = when {
                !setupAppearance -> outerStrokeColor
                isSelected -> activeColor
                onWallpaper -> wallpaperRingColor
                else -> setupRingColor
            }
            if (onWallpaper && !isSelected) {
                canvas.drawCircle(point.x, point.y, outerCircleRadius, wallpaperNodePaint)
            }
            canvas.drawCircle(point.x, point.y, outerCircleRadius, outerCircleStrokePaint)

            dotPaint.color = if (setupAppearance && !isSelected) setupDotColor else activeColor
            val radius = if (isSelected) activeDotRadius else innerDotRadius
            canvas.drawCircle(point.x, point.y, radius, dotPaint)
        }
    }

    fun useSetupAppearance(onWallpaper: Boolean = false) {
        setupAppearance = true
        this.onWallpaper = onWallpaper
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetPattern()
                drawing = true
                currentX = event.x
                currentY = event.y
                addTouchedDot(event.x, event.y)
                if (setupAppearance) {
                    if (selected.isEmpty()) {
                        resetPattern()
                        invalidate()
                        return false
                    }
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (drawing) {
                    currentX = event.x
                    currentY = event.y
                    addTouchedDot(event.x, event.y)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (setupAppearance) parent.requestDisallowInterceptTouchEvent(false)
                if (drawing) {
                    drawing = false
                    invalidate()
                    if (selected.isNotEmpty()) {
                        onPatternComplete?.invoke(selected.joinToString(separator = "-"))
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (setupAppearance) parent.requestDisallowInterceptTouchEvent(false)
                clearPattern()
            }
        }
        return true
    }

    fun clearPattern() {
        resetPattern()
        invalidate()
    }

    fun showError() {
        isErrorState = true
        invalidate()
        postDelayed({ clearPattern() }, 1000)
    }

    private fun resetPattern() {
        selected.clear()
        drawing = false
        isErrorState = false
    }

    private fun drawLines(canvas: Canvas) {
        if (selected.isEmpty()) return

        for (i in 0 until selected.lastIndex) {
            val start = centers[selected[i]]
            val end = centers[selected[i + 1]]
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint)
        }

        if (drawing && selected.isNotEmpty()) {
            val last = centers[selected.last()]
            canvas.drawLine(last.x, last.y, currentX, currentY, linePaint)
        }
    }

    private fun addTouchedDot(x: Float, y: Float) {
        val hitRadius = if (setupAppearance) outerCircleRadius else width / 6f
        centers.forEachIndexed { index, point ->
            if (index !in selected && hypot(x - point.x, y - point.y) <= hitRadius) {
                if (selected.isNotEmpty()) {
                    val last = selected.last()
                    val intermediate = getIntermediateDot(last, index)
                    if (intermediate != -1 && intermediate !in selected) {
                        selected.add(intermediate)
                    }
                }
                selected.add(index)
                return
            }
        }
    }

    private fun getIntermediateDot(start: Int, end: Int): Int {
        val startRow = start / 3
        val startCol = start % 3
        val endRow = end / 3
        val endCol = end % 3

        val isSameRow = startRow == endRow
        val isSameCol = startCol == endCol
        val isDiagonal = abs(startRow - endRow) == 2 && abs(startCol - endCol) == 2

        if ((isSameRow && abs(startCol - endCol) == 2) ||
            (isSameCol && abs(startRow - endRow) == 2) ||
            isDiagonal
        ) {
            val midRow = (startRow + endRow) / 2
            val midCol = (startCol + endCol) / 2
            return midRow * 3 + midCol
        }
        return -1
    }
}
