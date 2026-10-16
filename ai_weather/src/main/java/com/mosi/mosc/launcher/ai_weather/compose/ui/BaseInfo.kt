package com.mosi.mosc.launcher.ai_weather.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mosi.mosc.launcher.ai_weather.R
import com.mosi.mosc.launcher.ai_weather.compose.preview.SmallCardPreview
import com.mosi.mosc.launcher.ai_weather.ui.theme.AppTextStyles
import com.mosi.mosc.launcher.ai_weather.utils.Utils


@Composable
fun BaseInfoView(
    modifier: Modifier = Modifier,
    currentTemp: String?,
    highTemp: String?,
    lowTemp: String?,
    icon: String?,
    condition: String?
) {
    Box(
        modifier = modifier
    ) {
        if (currentTemp != null) {
            BaseInfoCurrent(
                currentTemp = currentTemp,
                highTemp = highTemp,
                lowTemp = lowTemp,
                condition = condition
            )

        } else {
            BaseInfoRange(
                highTemp = highTemp,
                lowTemp = lowTemp,
                condition = condition
            )
        }

        Image(
            painter = painterResource(Utils.getWeatherIconResourceId(icon)),
            contentDescription = "天气图标 $icon",
            modifier = Modifier.padding(start = 372.dp)
                .size(width = 268.dp, height = 200.dp)
        )
    }
}

@Composable
fun BaseInfoCurrent(
    modifier: Modifier = Modifier,
    currentTemp: String?,
    highTemp: String?,
    lowTemp: String?,
    condition: String?
) {
    Row(
        modifier = modifier.padding(top = 18.dp)
            .height(160.dp)
    ){
        Text(
            text = "$currentTemp",
            style = AppTextStyles.TitleCurrentTemp,
        )

        Icon(
            painter = painterResource(R.drawable.symbol_temp),
            contentDescription = "symbol_temp",
            modifier = Modifier
                .padding(4.dp,29.dp,8.dp,0.dp)
                .size(19.dp)
        )

        Column {
            Text(
                text = "$condition",
                modifier = Modifier.padding(top = 22.dp)
                    .size(240.dp, 56.dp)
                    .alpha(0.6F),
                style = AppTextStyles.ContentChinese_36
            )
            Text(
                text = "${highTemp}°/${lowTemp}°",
                modifier = Modifier.padding(0.dp,4.dp,0.dp,0.dp)
                    .size(240.dp, 60.dp)
                    .alpha(0.6F),
                style = AppTextStyles.ContentTemp
            )
        }
    }
}

@Composable
fun BaseInfoRange(
    modifier: Modifier = Modifier,
    highTemp: String?,
    lowTemp: String?,
    condition: String?
) {
    Column(
        modifier = modifier
            .height(192.dp)
    ) {
        Row {
            Text(
                text = "$highTemp",
                style = AppTextStyles.TitleTemp
            )
            Icon(
                painter = painterResource(R.drawable.symbol_temp),
                contentDescription = "symbol_temp",
                modifier = Modifier
                    .padding(4.dp,17.dp,4.dp,0.dp)
                    .size(19.dp)
            )
            Text(
                text = "/$lowTemp",
                modifier = Modifier.alpha(0.6F),
                style = AppTextStyles.TitleTemp
            )
            Icon(
                painter = painterResource(R.drawable.symbol_temp),
                contentDescription = "symbol_temp",
                modifier = Modifier
                    .padding(4.dp,17.dp,4.dp,0.dp)
                    .size(19.dp)
                    .alpha(0.6F)
            )
        }

        Text(
            text = "$condition",
            modifier = Modifier
                .size(240.dp, 56.dp)
                .alpha(0.6F),
            style = AppTextStyles.ContentChinese_36
        )
    }
}



@SmallCardPreview
@Composable
fun BaseCurrentPreview() {
    BaseInfoView(
        currentTemp = "27",
        highTemp = "29",
        lowTemp = "23",
        icon = "多云",
        condition = "晴转多云"
    )
}

@SmallCardPreview
@Composable
fun BaseRangePreview() {
    BaseInfoView(
        currentTemp = null,
        highTemp = "28",
        lowTemp = "20",
        icon = "多云",
        condition = "晴转多云"
    )
}