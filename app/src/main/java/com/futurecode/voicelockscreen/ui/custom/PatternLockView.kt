package com.futurecode.voicelockscreen.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

class PatternLockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onPatternComplete: ((String) -> Unit)? = null

    private val selected = mutableListOf<Int>()
    private val centers = MutableList(9) { PointF() }
    private var currentX = 0f
    private var currentY = 0f
    private var drawing = false

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2498FF")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DDE8F4")
        style = Paint.Style.FILL
    }
    private val selectedDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2475FF")
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A9D9FF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val cell = w / 3f
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                centers[row * 3 + col].set(cell * col + cell / 2f, cell * row + cell / 2f)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawLines(canvas)
        centers.forEachIndexed { index, point ->
            canvas.drawCircle(point.x, point.y, 22f, ringPaint)
            canvas.drawCircle(point.x, point.y, if (index in selected) 15f else 10f, if (index in selected) selectedDotPaint else dotPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selected.clear()
                drawing = true
                currentX = event.x
                currentY = event.y
                addTouchedDot(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                addTouchedDot(event.x, event.y)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawing = false
                invalidate()
                if (selected.isNotEmpty()) {
                    onPatternComplete?.invoke(selected.joinToString(separator = "-"))
                }
            }
        }
        return true
    }

    fun clearPattern() {
        selected.clear()
        drawing = false
        invalidate()
    }

    private fun drawLines(canvas: Canvas) {
        if (selected.size < 2 && !drawing) return
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
        val hitRadius = width / 8f
        centers.forEachIndexed { index, point ->
            if (index !in selected && hypot(x - point.x, y - point.y) <= hitRadius) {
                selected.add(index)
                return
            }
        }
    }
}
