package com.mosi.mosc.ai_weather.been;

import com.mosi.mosc.ai_weather.been.CardBaseInfo;
import com.mosi.mosc.ai_weather.been.CardUIData;

//服务端 → 客户端
interface IAIDLWeatherCallback {
    // 服务端 → 客户端：返回基础卡片信息
//    void onCardBaseInfoResilt(in CardBaseInfo result);
    // 服务端 → 客户端：返回卡片UI显示所需要的所有数据
    void onCardUIDateResult(in CardUIData cardUIData);
}