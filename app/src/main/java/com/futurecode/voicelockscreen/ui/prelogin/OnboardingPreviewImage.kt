package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.MeasureSpec
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ViewHomeVoiceSectionBinding

class OnboardingPreviewImage(context: Context, attrs: AttributeSet?) :
    AppCompatImageView(context, attrs) {

    private var voicePreview: ViewHomeVoiceSectionBinding? = null
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface)
    }
    private val cameraTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        typeface = ResourcesCompat.getFont(context, R.font.sora_bold)
        textSize = 23f
    }
    private val featureTilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface)
    }
    private val featureTileOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.border)
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val featureBadges by lazy {
        listOf(
            badge(R.drawable.bg_feature_tile_camera, R.drawable.ic_feature_camera, 121, 674),
            badge(R.drawable.bg_feature_tile_pin, R.drawable.ic_feature_pin, 367, 674),
            badge(R.drawable.bg_feature_tile_pattern, R.drawable.ic_feature_pattern, 121, 881),
            badge(R.drawable.bg_feature_tile_fingerprint, R.drawable.ic_feature_fingerprint, 367, 881)
        )
    }

    fun bindArtwork(@DrawableRes artwork: Int) {
        voicePreview = if (artwork == R.drawable.img_onboard_1) {
            voicePreview ?: createVoicePreview()
        } else {
            null
        }
        setImageResource(artwork)
        invalidate()
    }

    private fun createVoicePreview() =
        ViewHomeVoiceSectionBinding.inflate(LayoutInflater.from(context)).apply {
            tvVoiceSubtitle.setText(R.string.home_voice_status_line_off)
            tvVoiceTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
            val width = (340 * resources.displayMetrics.density).toInt()
            root.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val preview = voicePreview?.root ?: return
        val artwork = drawable ?: return
        val bounds = artwork.bounds
        if (bounds.isEmpty) return

        // Overlay coordinates refer to the original 676 × 1312 artwork.
        val saved = canvas.save()
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        canvas.concat(imageMatrix)
        canvas.scale(bounds.width() / 676f, bounds.height() / 1312f)
        val navigationLayer = canvas.save()
        // Widen just the baked-in navigation strip, staying inside the phone bezel.
        canvas.clipRect(84f, 1054f, 592f, 1207f)
        canvas.scale(1.045f, 1f, 338f, 0f)
        canvas.scale(676f / bounds.width(), 1312f / bounds.height())
        artwork.draw(canvas)
        canvas.restoreToCount(navigationLayer)
        // Clear the original top-row badges before redrawing them below the voice card.
        canvas.drawRect(121f, 656f, 182f, 674f, cardPaint)
        canvas.drawRect(367f, 656f, 428f, 674f, cardPaint)
        featureBadges.forEach { (background, icon) ->
            // The source already has colored tiles and icons. Cover both before drawing Home's badge.
            val tile = background.bounds
            canvas.drawRect(
                tile.left.toFloat(), tile.top.toFloat(),
                tile.right.toFloat(), tile.bottom.toFloat(), cardPaint
            )
            background.draw(canvas)
            icon.draw(canvas)
        }
        // Replace both lines of the title baked into the camera tile.
        canvas.drawRect(119f, 735f, 313f, 800f, cardPaint)
        canvas.drawText(context.getString(R.string.onboard_camera_tile_title), 121f, 759f, cameraTitlePaint)
        canvas.drawRoundRect(47.3f, 236f, 628.7f, 671f, 43f, 43f, cardPaint)
        val scale = minOf(570f / preview.width, 390f / preview.height)
        val previewLayer = canvas.save()
        canvas.translate(338f - preview.width * scale / 2, 442f - preview.height * scale / 2)
        canvas.scale(scale, scale)
        preview.draw(canvas)
        canvas.restoreToCount(previewLayer)
        drawFeatureTileTopEdges(canvas)
        canvas.restoreToCount(saved)
    }

    private fun drawFeatureTileTopEdges(canvas: Canvas) {
        val tileTop = 650f
        val tileBottom = 842f
        val edgeClipBottom = 673f
        val tileRadius = 24f
        listOf(101f to 328f, 348f to 575f).forEach { (left, right) ->
            val tile = canvas.save()
            canvas.clipRect(left, tileTop, right, edgeClipBottom)
            canvas.drawRoundRect(
                left,
                tileTop,
                right,
                tileBottom,
                tileRadius,
                tileRadius,
                featureTilePaint
            )
            canvas.drawRoundRect(
                left,
                tileTop,
                right,
                tileBottom,
                tileRadius,
                tileRadius,
                featureTileOutlinePaint
            )
            canvas.restoreToCount(tile)
        }
    }

    private fun badge(@DrawableRes background: Int, @DrawableRes icon: Int, left: Int, top: Int) =
        requireNotNull(AppCompatResources.getDrawable(context, background)).apply {
            setBounds(left, top, left + 61, top + 61)
        } to requireNotNull(AppCompatResources.getDrawable(context, icon)).apply {
            setBounds(left + 15, top + 15, left + 46, top + 46)
        }
}
