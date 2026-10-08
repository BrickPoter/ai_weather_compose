package com.mosi.mosc.launcher.ai_weather.drawable

// GradientCardBackground.kt
import android.graphics.*
import android.graphics.drawable.Drawable

class GradientCardBackground : Drawable() {

    private val linearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()

        // ===== 第一层：线性渐变 137.47deg =====
        // CSS 角度：0deg 指向正上方，顺时针增加
        // 137.47deg => 方向向量 (sin137.47, -cos137.47) ≈ (0.676, 0.737)
        // 从左上到右下方向
        val angleRad = Math.toRadians(137.47)
        val dx = Math.sin(angleRad).toFloat()
        val dy = -Math.cos(angleRad).toFloat()

        // 以中心为基准，计算起止点
        val cx = w / 2f
        val cy = h / 2f
        val halfLen = (Math.abs(dx) * w + Math.abs(dy) * h) / 2f

        val startX = cx - dx * halfLen
        val startY = cy - dy * halfLen
        val endX = cx + dx * halfLen
        val endY = cy + dy * halfLen

        linearPaint.shader = LinearGradient(
            startX, startY, endX, endY,
            intArrayOf(
                Color.argb((0.264 * 255).toInt(), 255, 255, 255), // 11.08%
                Color.argb((0.704 * 255).toInt(), 248, 250, 255), // 39.07%
                Color.argb((0.792 * 255).toInt(), 245, 248, 255), // 60.7%
                Color.argb((0.176 * 255).toInt(), 255, 255, 255)  // 95.9%
            ),
            floatArrayOf(0.1108f, 0.3907f, 0.607f, 0.959f),
            Shader.TileMode.CLAMP
        )

        // ===== 第二层：径向渐变 54.66% 116.51% at 92.19% 12.7% =====
        val centerX = w * 0.9219f
        val centerY = h * 0.127f
        // 半径取宽高的相对值，取较大者以保证椭圆覆盖
        val radius = Math.max(w * 0.5466f, h * 1.1651f)

        radialPaint.shader = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(
                Color.argb((0.16 * 255).toInt(), 255, 184, 18), // 0%
                Color.argb(0, 255, 255, 255)                     // 100%
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        canvas.drawRect(b, linearPaint)
        canvas.drawRect(b, radialPaint) // 叠加在上一层之上
    }

    override fun setAlpha(alpha: Int) {
        linearPaint.alpha = alpha
        radialPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        linearPaint.colorFilter = colorFilter
        radialPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}