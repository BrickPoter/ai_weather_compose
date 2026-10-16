package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutBaseRangeTempBinding
import com.mosi.mosc.launcher.ai_weather.utils.Utils

class CardBaseInfoRangeTempLayout(context: Context, attributeSet: AttributeSet):
ConstraintLayout(context, attributeSet){

    val binding = LayoutBaseRangeTempBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun bindData(
        highestTemp: String?,
        lowestTemp: String?,
        condition: String?,
        icon: String?
    ) {
        binding.textHighestTemp.text = "${highestTemp}°"
        binding.textLowestTemp.text = "/${lowestTemp}°"
        binding.textWeatherStatus.text = condition
        val imageId = Utils.getWeatherIconResourceId(icon)
        binding.imageWeather.setImageResource(imageId)
    }
}