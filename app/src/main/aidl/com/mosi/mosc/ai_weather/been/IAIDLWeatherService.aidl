package com.mosi.mosc.ai_weather.been;

import com.mosi.mosc.ai_weather.been.IAIDLWeatherCallback;


interface IAIDLWeatherService {
    // 客户端 → 服务端：发送天气数据 同步的
    void sendWeatherData(in String data);

    // 客户端 → 服务端：异步请求数据
    oneway void requestWeatherData(in String requestId, in String data,in IAIDLWeatherCallback callback);
}