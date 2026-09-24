package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutAlertInfoItemBinding
import com.mosi.mosc.launcher.ai_weather.model.AlertItem
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class AlertInfoRecycler(context: Context, attrs: AttributeSet) :
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
    fun setItems(items: List<AlertItem>) {
        // toList() 做防御性拷贝；submitList 内部走 DiffUtil
        alertAdapter.submitList(items.toList())
    }

    fun setOnItemClickListener(listener: (AlertItem) -> Unit) {
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

    private class AlertAdapter : ListAdapter<AlertItem, AlertAdapter.Holder>(DIFF){

        var onItemClick: ((AlertItem) -> Unit)? = null

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

            fun bind(data: AlertItem) {
                binding.itemImage.setImageResource(Utils.getAlertImageId(data.alertLevel))
                binding.itemText.text = data.text
            }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<AlertItem>() {
                override fun areItemsTheSame(oldItem: AlertItem, newItem: AlertItem): Boolean =
                    oldItem.alertLevel == newItem.alertLevel && oldItem.text == newItem.text

                override fun areContentsTheSame(oldItem: AlertItem, newItem: AlertItem): Boolean =
                    oldItem == newItem
            }
        }

    }
}