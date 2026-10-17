package com.mosi.mosc.launcher.ai_weather.compose.cardview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mosi.mosc.ai_weather.weather_srv.CardUIData
import com.mosi.mosc.launcher.ai_weather.compose.preview.LargeCardPreview
import com.mosi.mosc.launcher.ai_weather.compose.ui.AlertInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.ForecastInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.Header
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import com.mosi.mosc.launcher.ai_weather.utils.getUIData

@Composable
fun ForecastCardView(
    modifier: Modifier = Modifier,
    cardUIDataList : List<CardUIData>
) {

    val data = cardUIDataList[0]
    val baseInfo = data.baseInfo
    val forecastInfo = data.forecastInfo
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

        if ( alertInfo.isEmpty()) {
            ForecastInfoList(
                modifier = Modifier
                    .padding(top = 144.dp, start = 56.dp, end = 56.dp)
                    .size(618.dp, 836.dp),
                forecastItems = forecastInfo
            )
        } else {
            ForecastInfoList(
                modifier = Modifier
                    .padding(top = 144.dp, start = 56.dp, end = 56.dp)
                    .size(618.dp, 728.dp),
                forecastItems = forecastInfo
            )

            AlertInfoList(
                modifier = Modifier
                    .padding(top = 920.dp, start = 48.dp, end = 48.dp),
                alertInfoList = alertInfo
            )
        }

    }
}

@LargeCardPreview
@Composable
fun ForecastCardViewPreview() {
    ForecastCardView(
        cardUIDataList = getUIData(mode = "FORECAST")
    )
}

@LargeCardPreview
@Composable
fun ForecastAlertCardViewPreview() {
    ForecastCardView(
        cardUIDataList = getUIData(mode = "FORECAST_ALERT")
    )
}