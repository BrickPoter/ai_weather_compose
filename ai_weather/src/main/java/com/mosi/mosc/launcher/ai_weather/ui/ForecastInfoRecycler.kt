package com.mosi.mosc.launcher.ai_weather.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.ai_weather.been.CardForecastInfo
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutForecastItemBinding
import com.mosi.mosc.launcher.ai_weather.model.ForecastInfoItemData

class ForecastInfo(context: Context, attributeSet: AttributeSet):
    RecyclerView(context, attributeSet) {

    private val adapter : ForecastInfoAdapter = ForecastInfoAdapter()

    init {
        super.adapter = adapter
        super.layoutManager = LinearLayoutManager(context)
    }

    fun setDataList(list: List<CardForecastInfo>) {
        adapter.setData(list)
    }
}

class ForecastInfoAdapter: RecyclerView.Adapter<ForecastInfoAdapter.ForecastViewHolder>() {

    var dataList = mutableListOf<CardForecastInfo>()

//    @SuppressLint("NotifyDataSetChanged")
//    fun setData(list: List<ForecastInfoItemData>) {
//        dataList.clear()
//        dataList.addAll(list)
//        notifyDataSetChanged()
//    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(list: List<CardForecastInfo>) {
        dataList.clear()
        dataList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ForecastViewHolder {
        val binding = LayoutForecastItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ForecastViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ForecastViewHolder,
        position: Int
    ) {
        holder.bindData(dataList[position], position)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ForecastViewHolder(
        private val itemBinding: LayoutForecastItemBinding
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bindData(data: ForecastInfoItemData, position: Int) = with(itemBinding) {
            itemTextTime.text = data.time
            itemTextTemp.text = data.highTemp +"/" +  data.lowTemp
            itemTextCondition.text = data.condition
            if (! data.probability.isBlank()) {
                itemTextProbability.text = data.probability
                itemTextProbability.visibility = View.VISIBLE
            }
            // 设置图片资源
            //            itemImageIcon.setImageResource(data.imageId)
        }

        fun bindData(data: CardForecastInfo, position: Int) = with(itemBinding) {
            itemTextTime.text = data.time
            itemTextTemp.text = "${data.highTemp}°/${data.lowTemp}° "
            itemTextCondition.text = data.condition
            if (!data.probability.isNullOrEmpty()) {
                itemTextProbability.text = data.probability
                itemTextProbability.visibility = View.VISIBLE
            }
        }
    }
}