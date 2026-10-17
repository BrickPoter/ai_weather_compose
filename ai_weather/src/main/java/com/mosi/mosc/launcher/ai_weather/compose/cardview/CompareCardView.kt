package com.mosi.mosc.launcher.ai_weather.compose.cardview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import com.mosi.mosc.launcher.ai_weather.compose.ui.BaseInfoView
import com.mosi.mosc.launcher.ai_weather.compose.ui.ExtraInfoList
import com.mosi.mosc.launcher.ai_weather.compose.ui.Header
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import com.mosi.mosc.launcher.ai_weather.utils.getUIData

@Composable
fun CompareCardView(
    modifier: Modifier = Modifier,
    cardUIDataList: List<CardUIData>,
) {
    // 基准天气信息
    val baselineData = cardUIDataList[0]
    val baselineBaseInfo = baselineData.baseInfo
    val baselineExtraInfo = baselineData.extraInfo
    val baselineBgImageId = Utils.getTopBackgroundResourceId(
        Utils.getWeatherType(baselineBaseInfo.icon)
    )

    // 比较天气信息
    val targetData = cardUIDataList[1]
    val targetBaseInfo = targetData.baseInfo
    val targetExtraInfo = targetData.extraInfo
    val targetBgImageId = Utils.getBottomBackgroundResourceId(
        Utils.getWeatherType(targetBaseInfo.icon)
    )

    Box(
        modifier = modifier.size(730.dp, 1064.dp)
    ) {
        // 背景设置
        Image(
            painter = painterResource(baselineBgImageId),
            contentDescription = "${baselineBaseInfo.icon}",
            modifier = Modifier.size(730.dp, 532.dp),
            contentScale = ContentScale.Crop
        )
        Image(
            painter = painterResource(targetBgImageId),
            contentDescription = "${targetBaseInfo.icon}",
            modifier = Modifier
                .padding(top = 532.dp)
                .size(730.dp, 532.dp),
            contentScale = ContentScale.Crop
        )

        // 基准天气布局
        Header(
            modifier = Modifier.padding(start = 40.dp, top = 47.dp),
            role = null,
            location = baselineBaseInfo.location,
            date = baselineBaseInfo.showTime,
        )

        BaseInfoView(
            modifier = Modifier.padding(start = 54.dp, top = 140.dp),
            currentTemp = baselineBaseInfo.currentTemp,
            highTemp = baselineBaseInfo.highTemp,
            lowTemp = baselineBaseInfo.lowTemp,
            icon = baselineBaseInfo.icon,
            condition = baselineBaseInfo.tempInfo
        )

        ExtraInfoList(
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp, top = 372.dp)
                .background(
                    color = Color(0x80FFFFFF),
                    shape = RoundedCornerShape(36.dp)
                )
                .size(650.dp, 120.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            extraInfoList = baselineExtraInfo,
            mode = "inline",
        )

        // 中间分隔线
        Spacer(
            modifier = Modifier.padding(top = 532.dp, start = 40.dp).size(650.dp, 1.dp).background(
                color = Color.White
            )
        )

        // 对比天气布局
        Header(
            modifier = Modifier.padding(start = 40.dp, top = 579.dp),
            role = null,
            location = targetBaseInfo.location,
            date = targetBaseInfo.showTime,
        )

        BaseInfoView(
            modifier = Modifier.padding(start = 54.dp, top = 672.dp),
            currentTemp = targetBaseInfo.currentTemp,
            highTemp = targetBaseInfo.highTemp,
            lowTemp = targetBaseInfo.lowTemp,
            icon = targetBaseInfo.icon,
            condition = targetBaseInfo.tempInfo
        )

        ExtraInfoList(
            modifier = Modifier
                .padding(start = 40.dp, end = 40.dp, top = 904.dp)
                .background(
                    color = Color(0x80FFFFFF),
                    shape = RoundedCornerShape(36.dp)
                )
                .size(650.dp, 120.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            extraInfoList = targetExtraInfo,
            mode = "inline",
        )

    }


}

@LargeCardPreview
@Composable
fun CompareCardViewPreview() {
    CompareCardView(
        cardUIDataList = getUIData("COMPARE")
    )
}