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
import com.mosi.mosc.launcher.ai_weather.compose.ui.Header
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import com.mosi.mosc.launcher.ai_weather.utils.getUIData

@Composable
fun ExpandCardView(
    modifier: Modifier = Modifier,
    cardUIDataList : List<CardUIData>
) {
    val data = cardUIDataList[0]
    val baseInfo = data.baseInfo
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
            role = null,
            location = baseInfo.location,
            date = baseInfo.showTime,
        )

        BaseInfoView(
            modifier = Modifier.padding(start = 54.dp, top = 144.dp),
            currentTemp = baseInfo.currentTemp,
            highTemp = baseInfo.highTemp,
            lowTemp = baseInfo.lowTemp,
            icon = baseInfo.icon,
            condition = baseInfo.tempInfo
        )

        if (alertInfo.isEmpty()) {
            ExtraInfoList(
                modifier = Modifier
                    .padding(start = 40.dp, end = 40.dp, top = 392.dp)
                    .background(
                        color = Color(0x80FFFFFF),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .size(650.dp, 632.dp)
                    .padding(horizontal = 57.dp, vertical = 48.dp),
                extraInfoList = extraInfo,
                mode = "stack",
            )
        } else {
            AlertInfoList(
                modifier = Modifier
                    .padding(top = 380.dp, start = 48.dp, end = 48.dp),
                alertInfoList = alertInfo
            )

            ExtraInfoList(
                modifier = Modifier
                    .padding(start = 40.dp, end = 40.dp, top = 524.dp)
                    .background(
                        color = Color(0x80FFFFFF),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .size(650.dp, 500.dp)
                    .padding(horizontal = 57.dp, vertical = 64.dp),
                extraInfoList = extraInfo,
                verticalPadding = 80.dp,
                mode = "stack",
            )
        }

    }
}

@LargeCardPreview
@Composable
fun ExpandCardViewPreview() {
    ExpandCardView(
        cardUIDataList = getUIData(mode = "EXPAND")
    )
}

@LargeCardPreview
@Composable
fun ExpandAlertCardViewPreview() {
    ExpandCardView(
        cardUIDataList = getUIData(mode = "EXPAND_ALERT")
    )
}