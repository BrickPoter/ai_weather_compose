package com.mosi.mosc.ai_weather.been;

import com.mosi.mosc.ai_weather.been.CardBaseInfo;

interface IAIDLWeatherCallback {
    // 服务端 → 客户端：返回基础卡片信息
    void onCardBaseInfoResilt(in CardBaseInfo result);
}