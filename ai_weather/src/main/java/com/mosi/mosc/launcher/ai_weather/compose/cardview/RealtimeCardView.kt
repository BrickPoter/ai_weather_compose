package com.mosi.mosc.launcher.ai_weather.compose.cardview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.compose.preview.LargeCardPreview
import com.mosi.mosc.launcher.ai_weather.compose.ui.AlertInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.BaseInfoView
import com.mosi.mosc.launcher.ai_weather.compose.ui.ExtraInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.ForecastInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.Header
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import com.mosi.mosc.launcher.ai_weather.utils.getUIData

@Composable
fun RealtimeCardView(
    modifier: Modifier = Modifier,
    cardUIDataList : List<CardUIData>
) {
    val data = cardUIDataList[0]
    val baseInfo = data.baseInfo
    val forecastInfo = data.forecastInfo
    val extraInfo = data.extraInfo
    val alertInfo = data.warnInfo
    val bgImageId = Utils.getFullCardBackgroundResourceId(
        Utils.getWeatherType(baseInfo.icon)
    )

    Box(
        modifier = modifier.size(730.dp, 1064.dp)
    ) {
        // 背景图片设置
        Image(
            painter = painterResource(bgImageId),
            contentDescription = "${baseInfo.icon}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Header(
            modifier = Modifier.padding(start = 40.dp, top = 40.dp),
            role = "locate",
            location = baseInfo.location,
            date = null,
        )

        BaseInfoView(
            modifier = Modifier.padding(start = 54.dp, top = 144.dp),
            currentTemp = baseInfo.currentTemp,
            highTemp = baseInfo.highTemp,
            lowTemp = baseInfo.lowTemp,
            icon = baseInfo.icon,
            condition = baseInfo.tempInfo
        )

        if ( alertInfo.isEmpty()) {
            ForecastInfoList(
                modifier = Modifier
                    .padding(top = 398.dp, start = 56.dp, end = 56.dp)
                    .size(618.dp, 440.dp),
                forecastItems = forecastInfo,
                verticalPadding = 40.dp
            )
        } else {
            AlertInfoList(
                modifier = Modifier
                    .padding(top = 380.dp, start = 48.dp, end = 48.dp),
                alertInfoList = alertInfo
            )

            ForecastInfoList(
                modifier = Modifier
                    .padding(top = 544.dp, start = 56.dp, end = 56.dp)
                    .size(618.dp, 296.dp),
                forecastItems = forecastInfo
            )
        }

        ExtraInfoList(
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp, top = 904.dp)
                .background(
                    color = Color(0x80FFFFFF),
                    shape = RoundedCornerShape(36.dp)
                )
                .size(650.dp, 120.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            extraInfoList = extraInfo,
            mode = "inline",
        )

    }
}

@LargeCardPreview
@Composable
fun RealtimeCardViewPreview() {
    RealtimeCardView(
        cardUIDataList = getUIData(mode = "REALTIME")
    )
}

@LargeCardPreview
@Composable
fun RealtimeAlertCardViewPreview() {
    RealtimeCardView(
        cardUIDataList = getUIData(mode = "REALTIME_ALERT")
    )
}