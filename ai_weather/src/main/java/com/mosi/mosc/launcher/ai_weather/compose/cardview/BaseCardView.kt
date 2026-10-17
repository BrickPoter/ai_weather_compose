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
import com.mosi.mosc.launcher.ai_weather.compose.preview.SmallCardPreview
import com.mosi.mosc.launcher.ai_weather.compose.ui.BaseInfoView
import com.mosi.mosc.launcher.ai_weather.compose.ui.ExtraInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.Header
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import com.mosi.mosc.launcher.ai_weather.utils.getUIData


@Composable
fun BaseCardView(
    modifier: Modifier = Modifier,
    cardUIDataList: List<CardUIData>
) {
    val data = cardUIDataList[0]
    val baseInfo = data.baseInfo
    val extraInfo = data.extraInfo
    val bgImageId = Utils.getHalfBackgroundResourceId(
        Utils.getWeatherType(baseInfo.icon)
    )

    Box(
        modifier = modifier.size(730.dp, 500.dp)
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

        BaseInfoView(
            modifier = Modifier.padding(start = 54.dp, top = 120.dp),
            currentTemp = baseInfo.currentTemp,
            highTemp = baseInfo.highTemp,
            lowTemp = baseInfo.lowTemp,
            icon = baseInfo.icon,
            condition = baseInfo.tempInfo
        )

        ExtraInfoList(
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp, top = 340.dp)
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

@SmallCardPreview
@Composable
fun BaseCardViewPreview() {
    BaseCardView(
        cardUIDataList = getUIData("BASE")
    )
}