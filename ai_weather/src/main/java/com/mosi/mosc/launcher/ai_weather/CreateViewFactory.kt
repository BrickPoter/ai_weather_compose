package com.mosi.mosc.launcher.ai_weather

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.fawvw.hmi.ailauncher.pluginlib.CardSize
import com.mosi.mosc.ai_weather.been.CardBaseInfo

@SuppressLint("StaticFieldLeak")
object CreateViewFactory {
    private var cardView: View? = null

    fun createView(@SuppressLint("StaticFieldLeak") cardSize: CardSize, context: Context?): View{
        return  createCardView(context)
    }

    /**
     * 设置卡片数据
     */
    fun showBaseCard(baseInfo: CardBaseInfo?){
        cardView?.apply {
            baseInfo?.let {
                findViewById<TextView>(R.id.text_location).text = it.location

            }
        }
    }
    private fun createCardView( context: Context?): View {
        if (cardView == null) cardView = LayoutInflater.from(context).inflate(R.layout.layout_one_third,null)
        return cardView!!
    }

    fun destroyView(){
        if (cardView!=null) cardView =null
    }

    interface CreateViewListener{

    }
}
