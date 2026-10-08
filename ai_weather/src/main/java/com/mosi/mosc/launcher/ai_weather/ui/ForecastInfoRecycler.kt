package com.mosi.mosc.launcher.ai_weather.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mosi.mosc.ai_weather.been.CardForecastInfo
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutForecastItemBinding
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class ForecastInfoRecycler(context: Context, attributeSet: AttributeSet):
    RecyclerView(context, attributeSet) {

    private val forecastAdapter = ForecastInfoAdapter()

    init {
        adapter = forecastAdapter
        layoutManager = LinearLayoutManager(context, VERTICAL, false)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItemSpace(space: Int) {
        val marginPx = Utils.dp2px(context, space)
        forecastAdapter.itemSpace = marginPx
        forecastAdapter.notifyDataSetChanged()
    }

    fun setItems(items: List<CardForecastInfo>) {
        forecastAdapter.submitList(items.toList())
    }

    private class ForecastInfoAdapter:
        ListAdapter<CardForecastInfo, ForecastInfoAdapter.ForecastViewHolder>(DIFF) {

        companion object {
            private val DIFF = object: DiffUtil.ItemCallback<CardForecastInfo>() {
                override fun areItemsTheSame(
                    oldItem: CardForecastInfo,
                    newItem: CardForecastInfo
                ): Boolean =
                    oldItem.time == newItem.time
                            && oldItem.highTemp == newItem.highTemp
                            && oldItem.lowTemp == newItem.lowTemp
                            && oldItem.probability == newItem.probability
                            && oldItem.condition == newItem.condition

                override fun areContentsTheSame(
                    oldItem: CardForecastInfo,
                    newItem: CardForecastInfo
                ): Boolean = oldItem == newItem

            }
        }

        var itemSpace = 20

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
            holder.bind(getItem(position), position, itemSpace)
        }

        class ForecastViewHolder(
            private val itemBinding: LayoutForecastItemBinding
        ) : ViewHolder(itemBinding.root) {

            fun bind(data: CardForecastInfo, position: Int, space: Int) = with(itemBinding){
                val imageId = Utils.getWeatherIconResourceId(data.condition)
                itemImageIcon.setImageResource(imageId)
                itemTextTime.text = data.time
                if (data.lowTemp != null) {
                    itemTextTemp.text = "${data.highTemp}°/${data.lowTemp}°"
                } else {
                    itemTextTemp.text = "${data.highTemp}°"
                }
                itemTextCondition.text = data.condition
                if (!data.probability.isNullOrEmpty()) {
                    itemTextProbability.text = data.probability
                    itemTextProbability.visibility = View.VISIBLE
                }

                val params = itemView.layoutParams as LayoutParams
                params.topMargin = if (position == 0) 0 else space
                itemView.layoutParams = params
            }
        }
    }
}