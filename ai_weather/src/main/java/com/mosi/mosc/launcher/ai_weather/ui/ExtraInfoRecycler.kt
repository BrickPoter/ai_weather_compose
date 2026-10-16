package com.mosi.mosc.launcher.ai_weather.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.ai_weather.weather_srv.CardExtraInfo
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutExtraInfoItemInlineBinding
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutExtraInfoItemStackBinding
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class ExtraInfoRecycler(context: Context, attributeSet: AttributeSet) :
    RecyclerView(context, attributeSet) {

    companion object {
        /** title 与 content 同一行 */
        const val TYPE_INLINE = 0
        /** title 与 content 两行 */
        const val TYPE_STACK = 1
    }

    private val extraAdapter = ExtraInfoAdapter()

    init {
        adapter = extraAdapter
        super.layoutManager = GridLayoutManager(context, 2)
//        super.layoutManager = LinearLayoutManager(context)
        setHasFixedSize(true)
    }

    /** 切换布局类型：TYPE_INLINE（同行）或 TYPE_STACK（两行） */
    fun setLayoutType(type: Int) {
        extraAdapter.setLayoutType(type)
    }

    fun setItems(items: List<CardExtraInfo>) {
        extraAdapter.submitList(items.toList())
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setStackTopMargin(topMargin: Int) {
        extraAdapter.topMargin = topMargin
        extraAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setStackItemSpace(space: Int) {
        extraAdapter.itemSpace = space
        extraAdapter.notifyDataSetChanged()
    }

    private class ExtraInfoAdapter:
        ListAdapter<CardExtraInfo, ViewHolder>(DIFF) {
//    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<CardExtraInfo>() {
                override fun areItemsTheSame(oldItem: CardExtraInfo, newItem: CardExtraInfo): Boolean =
                    oldItem.title == newItem.title && oldItem.content == newItem.content

                override fun areContentsTheSame(oldItem: CardExtraInfo, newItem: CardExtraInfo): Boolean =
                    oldItem == newItem
            }
        }

        var itemSpace: Int = 0
        var topMargin: Int = 0
        private var layoutType: Int = TYPE_INLINE

        @SuppressLint("NotifyDataSetChanged")
        fun setLayoutType(type: Int) {
            if (layoutType == type) return
            layoutType = type
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int = layoutType

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when(viewType) {
                TYPE_INLINE  -> InlineViewHolder(
                    LayoutExtraInfoItemInlineBinding.inflate(inflater, parent, false)
                )
                else -> StackViewHolder(
                    LayoutExtraInfoItemStackBinding.inflate(inflater, parent, false)
                )
            }
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            when (holder) {
                is InlineViewHolder -> holder.bindData(getItem(position), position)
                is StackViewHolder -> holder.bindData(getItem(position), position, topMargin, itemSpace)
            }
        }

        class InlineViewHolder(
            private val itemBinding: LayoutExtraInfoItemInlineBinding
        ) : ViewHolder(itemBinding.root) {

            fun bindData(data: CardExtraInfo, position: Int) = with(itemBinding) {
                val imageId = Utils.getExtraInfoImageResourceId(data.title)
                itemExtraImage.setImageResource(imageId)
                itemContent.text = data.content

                val params = itemView.layoutParams as LayoutParams
                // 偶数列（第 1 列）：距 parent 左边 24dp
                // 奇数列（第 2 列）：距前一列 48dp
                params.marginStart = Utils.dp2px(itemBinding.root.context,
                    if (position % 2 == 0) 24 else 48)
                // 第一行与 parent 顶边距 topMargin，其余行与上一行间距 itemSpace（按需调整）
                itemView.layoutParams = params
            }
        }

        class StackViewHolder(
            private val itemBinding: LayoutExtraInfoItemStackBinding
        ): ViewHolder(itemBinding.root) {

            fun bindData(data: CardExtraInfo, position: Int, topMargin: Int, itemSpace: Int) = with(itemBinding) {
                val imageId = Utils.getExtraInfoImageResourceId(data.title)
                itemExtraImage.setImageResource(imageId)
                itemContent.text = data.content

                val params = itemView.layoutParams as RecyclerView.LayoutParams
                // 第一行与 parent 顶边距 topMargin，其余行与上一行间距 itemSpace（按需调整）
                params.topMargin = Utils.dp2px(itemBinding.root.context,if (position < 2) topMargin else itemSpace)
                // 左列距 parent 57dp，右列距左列 56dp
                params.marginStart = Utils.dp2px(itemBinding.root.context,if (position % 2 == 0) 57 else 56)
                itemView.layoutParams = params
            }
        }
    }
}