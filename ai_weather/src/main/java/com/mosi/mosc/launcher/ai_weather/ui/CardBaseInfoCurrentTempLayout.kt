package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutBaseCurrentTempBinding

class CardBaseInfoCurrentTempLayout(context: Context, attributeSet: AttributeSet):
ConstraintLayout(context, attributeSet){

    val binding = LayoutBaseCurrentTempBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun bindData(
        currentTemp: String?,
        highestTemp: String?,
        lowestTemp: String?,
        condition: String?,
    ){
        binding.textCurrentTemp.text = "${currentTemp}°"
        binding.textHighestLowestTemp.text = "${highestTemp}°/${lowestTemp}°"
        binding.textWeatherStatus.text = condition
    }
}