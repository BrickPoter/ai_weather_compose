package com.mosi.mosc.launcher.ai_weather

import android.annotation.SuppressLint
import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.mosi.mosc.ai_weather.been.CardBaseInfo
import com.mosi.mosc.launcher.ai_weather.databinding.LayoutOneThirdBinding
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object CreateViewFactory {
    private var cardViewRef: WeakReference<View>? = null
    private var bindingRef: WeakReference<LayoutOneThirdBinding>? = null

    fun createView( context: Context?): View{
        return  createCardView(context)
    }

    /**
     * 设置j基础卡片数据
     */
    fun showBaseCard(baseInfo: CardBaseInfo?,context: Context?){
        if (bindingRef == null){
            createCardView(context)
        }
        val binding = bindingRef?.get() ?: return
        baseInfo ?: return
        binding.layoutBase2.baseLayout.visibility = View.GONE
        // 通过 include 的 id 精确访问各个子布局（这是 ViewBinding 的核心优势）
        // 上面的 layout_base
        binding.layoutBase.apply {
            textLocation.text = baseInfo.location
            textTime.text = baseInfo.showTime
            textTempDesc.text = baseInfo.tempInfo
        }
        // 下面的 layout_base_2
        binding.layoutBase2.apply {
            textLocation.text = baseInfo.location
            textTime.text = baseInfo.showTime
        }
    }

    fun showTodayHourCard(baseInfo: CardBaseInfo?){
        val binding = bindingRef?.get() ?: return
        baseInfo ?: return
        binding.layoutBase2.baseLayout.visibility = View.GONE
        binding.layoutBase.apply {
            textLocation.text = baseInfo.location
            textTime.text = baseInfo.showTime
        }
    }
    private fun createCardView( context: Context?): View {
        // 复用已有 View
        bindingRef?.get()?.let { return it.root }

        val ctx = context ?: return View(context)
        val binding = LayoutOneThirdBinding.inflate(LayoutInflater.from(ctx))
        bindingRef = WeakReference(binding)
        cardViewRef = WeakReference(binding.root)
        return binding.root
    }

    fun destroyView(){
        cardViewRef = null
        bindingRef = null
    }

    interface CreateViewListener{

    }
}
