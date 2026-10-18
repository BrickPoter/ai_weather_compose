package com.example.uidemo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.example.uidemo.databinding.ActivityMainBinding
import com.mosi.mosc.launcher.ai_weather.compose.cardview.BaseCarLargeView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.BaseCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.CompareCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.ExpandCardView
import com.mosi.mosc.launcher.ai_weather.compose.cardview.ForecastCardView

import com.mosi.mosc.launcher.ai_weather.compose.cardview.RealtimeCardView
import com.mosi.mosc.launcher.ai_weather.utils.getUIData


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var cardComposeView: ComposeView ?= null

    private fun switchMode(mode: String) {
        // 清空旧 View
        binding.card1.removeAllViews()

        // 1. 首先获取对应的 Composable 内容
        val content: @Composable () -> Unit = when (mode) {
            "REALTIME", "REALTIME_ALERT" -> { { RealtimeCardView(cardUIDataList = getUIData(mode)) } }
            "FORECAST", "FORECAST_ALERT" -> { { ForecastCardView(cardUIDataList = getUIData(mode)) } }
            "COMPARE" -> { { CompareCardView(cardUIDataList = getUIData(mode)) } }
            "EXPAND", "EXPAND_ALERT" -> { { ExpandCardView(cardUIDataList = getUIData(mode)) } }
            "BASE" -> { { BaseCardView(cardUIDataList = getUIData(mode)) } }
            "BASE_LARGE" -> { { BaseCarLargeView(cardUIDataList = getUIData(mode)) } }
            else -> { { BaseCardView(cardUIDataList = getUIData(mode)) } }
        }

        // 2. 统一创建 ComposeView 并设置内容
        val cardComposeView = ComposeView(this).apply {
            setContent { content() }
        }

        binding.card1.post {
            Log.d("Card", "screen: ${resources.displayMetrics.widthPixels} x ${resources.displayMetrics.heightPixels}")
            Log.d("Card", "density: ${resources.displayMetrics.density}")
            Log.d("Card", "card1: ${binding.card1.width} x ${binding.card1.height}")
            Log.d("Card", "root: ${cardComposeView.width} x ${cardComposeView.height}")
        }

        binding.card1.addView(cardComposeView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val mode = intent.getStringExtra("mode") ?: "BASE"
        switchMode(mode)


        binding.button1.setOnClickListener {
            switchMode("BASE")
        }

        binding.button2.setOnClickListener {
            switchMode("EXPAND")
        }

        binding.button3.setOnClickListener {
            switchMode("EXPAND_ALERT")
        }

        binding.button4.setOnClickListener {
            switchMode("REALTIME")
        }

        binding.button5.setOnClickListener {
            switchMode("REALTIME_ALERT")
        }

        binding.button6.setOnClickListener {
            switchMode("FORECAST")
        }

        binding.button9.setOnClickListener {
            switchMode("FORECAST_ALERT")
        }

        binding.button7.setOnClickListener {
            switchMode("COMPARE")
        }

        binding.button8.setOnClickListener {
            switchMode("BASE_LARGE")
        }

        binding.imgGone.setOnClickListener {
            binding.img.visibility = View.GONE
        }
        binding.imgInvisible.setOnClickListener {
            binding.img.visibility = View.INVISIBLE
        }
        binding.imgVisible.setOnClickListener {
            binding.img.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

