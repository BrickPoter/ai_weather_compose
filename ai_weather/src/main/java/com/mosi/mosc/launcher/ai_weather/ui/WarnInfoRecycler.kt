package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.ai_weather.been.CardWarnInfo
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutAlertInfoItemBinding
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class WarnInfoRecycler(context: Context, attrs: AttributeSet) :
        RecyclerView(context, attrs) {

    private val alertAdapter = AlertAdapter()
    private var pagerMode = false
    private var pagerSnapHelper: PagerSnapHelper ?= null

    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        adapter = alertAdapter
        setHasFixedSize(true)
    }

    // ======== 对外 API ========
    fun setItems(items: List<CardWarnInfo>) {
        // toList() 做防御性拷贝；submitList 内部走 DiffUtil
        alertAdapter.submitList(items.toList())
    }

    fun setOnItemClickListener(listener: (CardWarnInfo) -> Unit) {
        alertAdapter.onItemClick = listener
    }

    /**
     * 分页模式：开启后一次滑动只翻一页，松手自动吸附对齐到 item 边界。
     * 配合 item 宽度 match_parent（等于控件可用宽度），即可「始终保持一整个 item 完整显示」。
     */
    fun setPagerMode(enabled: Boolean) {
        if (pagerMode == enabled) {
            return
        }
        pagerMode = enabled
        if (enabled) {
            if (pagerSnapHelper == null) {
                pagerSnapHelper = PagerSnapHelper()
            }
            pagerSnapHelper?.attachToRecyclerView(this)
            overScrollMode = OVER_SCROLL_NEVER
        } else if (pagerSnapHelper != null) {
            pagerSnapHelper?.attachToRecyclerView(null)
        }
    }

    fun isPagerMode() = pagerMode

    private class AlertAdapter : ListAdapter<CardWarnInfo, AlertAdapter.Holder>(DIFF){

        var onItemClick: ((CardWarnInfo) -> Unit)? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): Holder {
            val binding = LayoutAlertInfoItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            val holder = Holder(binding)
            holder.itemView.setOnClickListener {
                val position = holder.adapterPosition
                if (position != NO_POSITION) {
                    onItemClick?.invoke(getItem(position))
                }
            }
            return holder
        }

        override fun onBindViewHolder(
            holder: Holder,
            position: Int
        ) {
            holder.bind(getItem(position))
        }

        private class Holder(val binding: LayoutAlertInfoItemBinding) :
            ViewHolder(binding.root) {

            fun bind(data: CardWarnInfo) {
                if (data.title == null) return
                binding.itemImage.setImageResource(Utils.getAlertImageResourceId(data.title))
                binding.itemText.text = data.content
            }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<CardWarnInfo>() {
                override fun areItemsTheSame(oldItem: CardWarnInfo, newItem: CardWarnInfo): Boolean =
                    oldItem.title == newItem.title && oldItem.content == newItem.content

                override fun areContentsTheSame(oldItem: CardWarnInfo, newItem: CardWarnInfo): Boolean =
                    oldItem == newItem
            }
        }

    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        Log.d("WarnRV", "intercept action=${e.actionMasked}")
        return super.onInterceptTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val result = super.onTouchEvent(e)

        Log.d(
            "WarnRV",
            "action=${e.actionMasked}, " +
                    "x=${e.x}, y=${e.y}, " +
                    "scrollState=$scrollState, " +
                    "canScrollLeft=${canScrollHorizontally(-1)}, " +
                    "canScrollRight=${canScrollHorizontally(1)}, " +
                    "result=$result"
        )

        return result
    }


}