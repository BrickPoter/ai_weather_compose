package com.mosi.mosc.launcher.ai_weather.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutExtraInfoItemBinding
import com.mosi.mosc.launcher.ai_weather.model.ExtraInfoItemData

class ExtraInfo(context: Context, attributeSet: AttributeSet) :
    RecyclerView(context, attributeSet) {

    private val adapter: ExtraInfoAdapter = ExtraInfoAdapter()

    init {
        super.adapter = adapter
//        super.layoutManager = GridLayoutManager(context, 2)
        super.layoutManager = LinearLayoutManager(context)
    }

    fun setDataList(list: List<ExtraInfoItemData>) {
        adapter.setData(list)
    }
}

class ExtraInfoAdapter:
    RecyclerView.Adapter<ExtraInfoAdapter.ExtraInfoViewHolder>() {

    private val dataList = mutableListOf<ExtraInfoItemData>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<ExtraInfoItemData>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExtraInfoViewHolder {
        val binding = LayoutExtraInfoItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExtraInfoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ExtraInfoViewHolder,
        position: Int
    ) {
        holder.bindData(dataList[position], position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ExtraInfoViewHolder(
        private val itemBinding: LayoutExtraInfoItemBinding
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindData(data: ExtraInfoItemData, position: Int) = with(itemBinding) {
            itemTitle.text = data.title
            itemContent.text = data.content

            val params = itemView.layoutParams as RecyclerView.LayoutParams
            params.topMargin = if (position < 2) 0 else 50
            params.marginStart = if (position % 2 == 0) 0 else 22
            itemView.layoutParams = params
        }
    }
}