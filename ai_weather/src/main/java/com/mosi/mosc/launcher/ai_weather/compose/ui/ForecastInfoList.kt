package com.mosi.mosc.launcher.ai_weather.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mosi.mosc.ai_weather.weather_srv.CardForecastInfo
import com.mosi.mosc.launcher.ai_weather.compose.preview.LargeCardPreview
import com.mosi.mosc.launcher.ai_weather.ui.theme.AppTextStyles
import com.mosi.mosc.launcher.ai_weather.ui.theme.Probability
import com.mosi.mosc.launcher.ai_weather.utils.Utils
import kotlin.text.replace

@Composable
fun ForecastInfoList(
    modifier: Modifier = Modifier,
    forecastItems: List<CardForecastInfo>,
    verticalPadding: Dp = 28.dp,
    ) {

    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(verticalPadding)
    ) {
        items(forecastItems) { item ->

            ForecastInfoItemView(
                modifier = Modifier.fillMaxWidth(),
                forecastInfo = item
            )
        }
    }
}

@Composable
fun ForecastInfoItemView(
    modifier: Modifier = Modifier,
    forecastInfo: CardForecastInfo,
    ) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = forecastInfo.time ?: "null",
            style = AppTextStyles.ContentChinese_34,
            modifier = Modifier.width(104.dp).alpha(0.6F)
        )

        Spacer(Modifier.width(12.dp))

        val probabilityValue = getProbabilityValue(forecastInfo.probability)
        if (probabilityValue > 0) {
            Text(
                text = "${probabilityValue}%",
                style = AppTextStyles.ContentChinese_34,
                color = Probability,
                textAlign = TextAlign.End,
                modifier = Modifier.width(88.dp)
            )
        } else {
            Spacer(Modifier.width(88.dp))
        }

        Spacer(Modifier.width(8.dp))

        Image(
            painter = painterResource(
                Utils.getWeatherIconResourceId(forecastInfo.icon)
            ),
            contentDescription = forecastInfo.condition,
            modifier = Modifier.size(108.dp, 80.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = forecastInfo.condition ?: "null",
            style = AppTextStyles.ContentChinese_34,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1F)
                .alpha(0.6F)
//                .basicMarquee()
        )

        if (forecastInfo.lowTemp == null) {
            Text(
                text = "${forecastInfo.highTemp}°",
                style = AppTextStyles.ContentChinese_34,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(128.dp)
                    .alpha(0.6F)
            )
        } else {
            Text(
                text = "${forecastInfo.highTemp}°/${forecastInfo.lowTemp}°",
                style = AppTextStyles.ContentChinese_34,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .width(128.dp)
                    .alpha(0.6F)
            )
        }
    }

}

fun getProbabilityValue(str: String?): Int  {
    if (str.isNullOrBlank()) return 0
    return str.replace("%", "")
        .trim()
        .toDouble()
        .toInt()
}

@LargeCardPreview
@Composable
fun ForecastInfoItemViewPreview() {
    ForecastInfoItemView(
        modifier = Modifier.size(618.dp, 80.dp),
        forecastInfo = CardForecastInfo(
            time = "12/28",
            probability = "60%",
            icon = "雷阵雨",
            condition = "阴转雷阵雨",
            highTemp = "24",
            lowTemp = "16",
        )
    )
}

@LargeCardPreview
@Composable
fun ForecastInfoListViewPreview() {
    val forecastInfoList = mutableListOf<CardForecastInfo>()
    val forecastInfo = CardForecastInfo(
        time = "12/28",
        probability = "60%",
        icon = "雷阵雨",
        condition = "阴转雷阵雨",
        highTemp = "24",
        lowTemp = "16"
    )
    repeat(7) {
        val newItem = forecastInfo.copy()
        forecastInfoList.add(newItem)
    }

    ForecastInfoList(
        modifier = Modifier.size(618.dp, 728.dp),
        forecastItems = forecastInfoList
    )
}