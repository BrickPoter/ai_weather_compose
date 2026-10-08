package com.mosi.mosc.launcher.ai_weather.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutCardHeaderBinding

class CardHeaderLayout(context: Context, attributeSet: AttributeSet):
    LinearLayout(context, attributeSet){

    // 暴露内部 binding
    val binding = LayoutCardHeaderBinding.inflate(
        LayoutInflater.from(context), this, true)

    /** 对外提供设置数据的方法 */
    fun bindData(location: String?, time: String?) {
        binding.textLocation.text = location
        if (time != null) {
            binding.textTime.text = "·$time"
        }
    }

    /** 如果外部想单独访问控件 */
    fun setLocation(text: String?) {
        binding.textLocation.text = text
    }
    fun setTime(text: String?) {
        binding.textTime.text = text
    }
}