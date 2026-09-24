package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.launcher.ai_weather.R
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

class Indicator(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    companion object {
        /** 最多同时显示的段数；页数更多时只显示包含当前页的窗口 */
        private const val MAX_PAGE = 7
    }
    /** 分段指示条主画笔 */
    private val paintSegment = Paint(Paint.ANTI_ALIAS_FLAG)
    /** 指示条高度 px */
    private var indicatorHeight: Int
    /** 选中的指示条宽度 px */
    private var selectedWidth: Int
    /** 未选中的指示条宽度 px */
    private var unselectedWidth: Int
    /** 选中的指示条颜色 */
    private var selectedColor : Int
    /** 未选中的指示条颜色 */
    private var unselectedColor : Int
    /** 指示条之间的间隙 px */
    private var padding: Int
    /** 指示条圆角半径 px */
    private var cornerRadius: Int
    /** 需要绘制的指示条数量 px */
    private var count = MAX_PAGE
    /** 当前位置（页）*/
    private var currentIndex = 0
    /** 当前页 -> 下一页的进度， 0 ~ 1 */
    private var currentFraction = 0F
    /** 绑定的 RecyclerView */
    private var attachedRecyclerView: RecyclerView? = null


    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.Indicator)
        try {
            // 尺寸用 getDimensionPixelSize：24dp 这类字面量和 @dimen/xxx 都能取到。
            indicatorHeight = typedArray.getDimensionPixelSize(
                R.styleable.Indicator_height,
                context.resources.getDimensionPixelSize(R.dimen.Indicator_Item_Height)
            )
            unselectedWidth = typedArray.getDimensionPixelSize(
                R.styleable.Indicator_unSelectedWidth,
                context.resources.getDimensionPixelSize(R.dimen.Indicator_Item_Width)
            )
            selectedWidth = typedArray.getDimensionPixelSize(
                R.styleable.Indicator_selectedWidth,
                context.resources.getDimensionPixelSize(R.dimen.Indicator_Item_Width_Selected)
            )

            // 颜色用 getColor：#A3000000 这类字面量和 @color/xxx 引用都能取到
            unselectedColor = typedArray.getColor(
                R.styleable.Indicator_UnselectedColor,
                context.getColor(R.color.Indicator_Unselected)
            )
            selectedColor = typedArray.getColor(
                R.styleable.Indicator_selectedColor,
                context.getColor(R.color.Indicator_Selected)
            )
        } finally {
            typedArray.recycle()
        }
        padding = indicatorHeight
        cornerRadius = indicatorHeight / 2
    }

    // ========= 对外 API =========
    fun setCount(count: Int) {
        val newCount = count.coerceAtLeast(0)
        if (this.count == newCount) return
        this.count = newCount
        currentIndex = currentIndex.coerceAtMost((newCount - 1).coerceAtLeast(0))
        requestLayout()
        invalidate()
    }

    fun setColorSelected(customColor: Int) {
        this.selectedColor = customColor
        invalidate()
    }

    fun setColorUnselected(customColor: Int) {
        this.unselectedColor = customColor
        invalidate()
    }

    fun setCurrentPosition(index: Int, fraction: Float = 0F) {
        val newIndex = index.coerceIn(0, (count - 1).coerceAtLeast(0))
        // 最后一页没有 “下一页”，忽略残留的滚动进度
        val newFraction = if (newIndex == count - 1) 0F else fraction.coerceIn(0F, 1F)
        if (newIndex == currentIndex && newFraction == currentFraction) return
        currentIndex = newIndex
        currentFraction = newFraction
        invalidate()
    }


    // =========== 跟随 RecyclerView ==========
    private val scrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            syncFromRecyclerView(recyclerView)
        }
    }

    private val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            syncAttached()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            syncAttached()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            syncAttached()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            syncAttached()
        }
    }

    /** 让指示器跟随一个横向 RecyclerView；数据变化和滚动都会自动同步 */
    fun attachToRecyclerView(recyclerView: RecyclerView) {
        if (attachedRecyclerView === recyclerView) {
            syncFromRecyclerView(recyclerView)
            return
        }
        detachFromRecyclerView()
        attachedRecyclerView = recyclerView
        recyclerView.addOnScrollListener(scrollListener)
        recyclerView.adapter?.registerAdapterDataObserver(dataObserver)
        syncFromRecyclerView(recyclerView)
    }

    /** 解除跟随 */
    fun detachFromRecyclerView() {
        attachedRecyclerView?.let { recyclerView ->
            recyclerView.removeOnScrollListener(scrollListener)
            recyclerView.adapter?.unregisterAdapterDataObserver(dataObserver)
        }
        attachedRecyclerView = null
    }

    private fun syncAttached() {
        attachedRecyclerView?.let { syncFromRecyclerView(it) }
    }

    private fun syncFromRecyclerView(recyclerView: RecyclerView) {
        val itemCount = recyclerView.adapter?.itemCount ?: 0
        setCount(itemCount)
        if (itemCount <= 0) return

        val layoutManager = recyclerView.layoutManager
        if (layoutManager !is LinearLayoutManager) {
            setCurrentPosition(0)
            return
        }

        val firstPosition = layoutManager.findFirstVisibleItemPosition()
        if (firstPosition == RecyclerView.NO_POSITION) return

        val child = layoutManager.findViewByPosition(firstPosition)
        val fraction = if (child != null && child.width > 0) {
            (layoutManager.paddingLeft - child.left).toFloat() / child.width
        } else {
            0f
        }
        setCurrentPosition(firstPosition, fraction)
    }


    // ========== 绘制 ============
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val contentWidth  = selectedWidth + (count - 1) * (unselectedWidth + padding) + paddingLeft + paddingRight
        val contentHeight = indicatorHeight + paddingTop + paddingBottom

        val width = resolveSize(contentWidth, widthMeasureSpec)
        val height = resolveSize(contentHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    protected override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (indicatorHeight <= 0) return
        val count = this.count
        // 定义画板大小
        val left = paddingLeft
        val right = width - paddingRight
        if (right - left <= 0) return
        val bottom = height - paddingBottom
        val top = paddingTop
        drawSegment(canvas, left, right, top, bottom, count)
    }

    private fun drawSegment(canvas: Canvas, left: Int, right: Int, top: Int, bottom: Int, count: Int) {
        // 页数超过最大显示页数时，只绘制包含当前页的窗口，宽度不在随页数变化
        val windowSize = min(count, MAX_PAGE)
        val windowStart = (currentIndex - windowSize / 2).coerceIn(0, count - windowSize)

        val totalWidth = selectedWidth + (count - 1F) * (unselectedWidth + padding)

        // 当前页在窗口内的连续位置：0 ~ windowSize-1
        // 把 0~1 的滚动进度映射成 0 ~ (count-1) 的连续位置
        val exactPos = (currentIndex + currentFraction - windowStart).coerceIn(0F, windowSize - 1F)
        val index = floor(exactPos).toInt()
        val fraction = exactPos - index

        var startWidth = left + ((right - left) - totalWidth) / 2F
        val startHeight = top + ((bottom - top) - indicatorHeight) / 2F
        for (i in 0..< windowSize) {
            var width: Float
            var color: Int
            if (index == i) {
                // 正在离开的当前段：由宽变窄、由深变浅
                width = selectedWidth + fraction * (unselectedWidth - selectedWidth)
//                color = ResourcesCompat.getColor(context.resources, R.color.Indicator_Unselected, context.theme)
                color = blenderColor(selectedColor, unselectedColor, fraction)
            } else if (i == (index + 1)) {
                // 正在进入的下一段：由窄变宽、由浅变深
                width = unselectedWidth + fraction * (selectedWidth - unselectedWidth)
                color = blenderColor(unselectedColor, selectedColor, fraction)
            } else {
                width = unselectedWidth.toFloat()
                color = unselectedColor
            }
            paintSegment.color = color
            canvas.drawRoundRect(startWidth, startHeight, startWidth + width, startHeight + indicatorHeight,
                cornerRadius.toFloat(), cornerRadius.toFloat(), paintSegment)
            startWidth += (width + padding)
        }


    }

    // ==================== 工具 ====================
    private fun blenderColor(from: Int, to: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0F, 1F)
        return Color.argb(
            (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t).roundToInt(),
            (Color.red(from) + (Color.red(to) - Color.red(from)) * t).roundToInt(),
            (Color.green(from) + (Color.green(to) - Color.green(from)) * t).roundToInt(),
            (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).roundToInt()
        )
    }
}