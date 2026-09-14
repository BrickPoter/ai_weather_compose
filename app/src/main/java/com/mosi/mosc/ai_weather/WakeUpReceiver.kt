package com.mosi.mosc.ai_weather

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WakeUpReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "WakeUpReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到唤醒广播 action=${intent.action}, App 已脱离 stopped 状态")

        // 其实这里什么都不做也行 —— 只要 onReceive 被调用，
        // 系统就已经把 App 从 stopped 状态拉起来了。
        //
        // 但 onReceive 返回后进程可能很快被回收，为了保证
        // 紧接着的 bindService 一定能成功，可以在这里主动起一下 Service，
        // 让进程 "稳住" 一会儿。
        try {
            val serviceIntent = Intent(context, WeatherAIDLServices::class.java)
            context.startService(serviceIntent)
            Log.d(TAG, "已主动 startService，保证进程存活")
        } catch (e: Exception) {
            Log.e(TAG, "startService 失败: ${e.message}")
        }
    }
}