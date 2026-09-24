package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutWarnInfoBinding

class CardWarnInfoLayout(context: Context, attributeSet: AttributeSet):
ConstraintLayout(context, attributeSet){

    val binding = LayoutWarnInfoBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun bindData(
        title: String?,
        content: String?
    ) {
        binding.title.text = title
        binding.content.text = content
    }
}