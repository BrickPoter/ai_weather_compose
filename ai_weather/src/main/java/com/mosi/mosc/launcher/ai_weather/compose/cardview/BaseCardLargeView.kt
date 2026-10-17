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
import com.mosi.mosc.launcher.ai_weather.compose.ui.BaseInfoCurrent
import com.mosi.mosc.launcher.ai_weather.compose.ui.BaseInfoRange
import com.mosi.mosc.launcher.ai_weather.compose.ui.ExtraInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.Header
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import com.mosi.mosc.launcher.ai_weather.utils.getUIData

@Composable
fun BaseCarLargeView(
    modifier: Modifier = Modifier,
    cardUIDataList: List<CardUIData>
) {
    val data = cardUIDataList[0]
    val baseInfo = data.baseInfo
    val extraInfo = data.extraInfo
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
            role = null,
            location = baseInfo.location,
            date = baseInfo.showTime,
        )

        Image(
            painter = painterResource(Utils.getWeatherIconResourceId(baseInfo.icon)),
            contentDescription = "天气图标 ${baseInfo.icon}",
            modifier = Modifier.padding(start = 104.dp, top = 132.dp)
                .size(width = 482.dp, height = 360.dp)
        )

        if (baseInfo.currentTemp != null) {
            BaseInfoCurrent(
                modifier = Modifier.padding(start = 180.dp, top = 492.dp),
                currentTemp = baseInfo.currentTemp,
                highTemp = baseInfo.highTemp,
                lowTemp = baseInfo.lowTemp,
                condition = baseInfo.tempInfo
            )
        } else {
            BaseInfoRange(
                modifier = Modifier.padding(start = 180.dp, top = 492.dp),
                highTemp = baseInfo.highTemp,
                lowTemp = baseInfo.lowTemp,
                condition = baseInfo.tempInfo
            )
        }

        ExtraInfoList(
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp, top = 720.dp)
                .background(
                    color = Color(0x80FFFFFF),
                    shape = RoundedCornerShape(36.dp)
                )
                .size(650.dp, 300.dp)
                .padding(start = 57.dp, top = 80.dp, end = 57.dp, bottom = 74.dp),
            extraInfoList = extraInfo,
            mode = "stack",
        )
    }

}

@LargeCardPreview
@Composable
fun BaseCardLargeViewPreview() {
    BaseCarLargeView(
        cardUIDataList = getUIData("BASE")
    )
}