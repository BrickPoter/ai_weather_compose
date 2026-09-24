package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutBaseRangeTempBinding

class CardBaseInfoRangeTempLayout(context: Context, attributeSet: AttributeSet):
ConstraintLayout(context, attributeSet){

    val binding = LayoutBaseRangeTempBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun bindData(
        highestTemp: String?,
        lowestTemp: String?,
        condition: String?,
    ) {
        binding.textHighestTemp.text = "${highestTemp}°"
        binding.textLowestTemp.text = "/${lowestTemp}°"
        binding.textWeatherStatus.text = condition
    }
}