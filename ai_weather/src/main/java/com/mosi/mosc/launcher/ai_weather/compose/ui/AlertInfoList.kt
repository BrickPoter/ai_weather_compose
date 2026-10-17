package com.mosi.mosc.launcher.ai_weather.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mosi.mosc.ai_weather.weather_srv.CardWarnInfo
import com.mosi.mosc.launcher.ai_weather.compose.preview.LargeCardPreview
import com.mosi.mosc.launcher.ai_weather.compose.widget.PagerIndicator
import com.mosi.mosc.launcher.ai_weather.compose.theme.AppTextStyles
import com.mosi.mosc.launcher.ai_weather.utils.Utils

@Composable
fun AlertInfoList(
    modifier: Modifier = Modifier,
    alertInfoList: List<CardWarnInfo>
) {

    val pagerState = rememberPagerState( pageCount = { alertInfoList.size } )

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
        ) { page ->
            val bgColor = Utils.getAlertBgColor(alertInfoList[page].title)
            AlertInfo(
                modifier = Modifier
                    .size(634.dp, 96.dp)
                    .background(
                        color = bgColor,
                        shape = RoundedCornerShape(48.dp)
                    ),
                alertInfo = alertInfoList[page]
            )
        }

        if (alertInfoList.size > 1) {
            PagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.CenterHorizontally),
                activeColor = Color(0xA3000000),
                inactiveColor = Color(0x29000000),
                )
        }
    }
}

@Composable
fun AlertInfo(
    modifier: Modifier = Modifier,
    alertInfo: CardWarnInfo
) {
    Box(modifier.fillMaxSize()) {
        Image(
            modifier = Modifier
                .padding(start = 32.dp, top = 16.dp).size(72.dp),
            painter = painterResource(
                Utils.getAlertImageResourceId(alertInfo.title!!)
            ),
            contentDescription = alertInfo.title
        )

        Text(
            text = alertInfo.content ?: "null",
            style = AppTextStyles.ContentChinese_34,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 120.dp, top = 23.dp)
                .size(474.dp, 50.dp)
                .basicMarquee()
        )
    }
}

@LargeCardPreview
@Composable
fun AlertInfoPreview() {
    AlertInfo(
        modifier = Modifier.size(634.dp, 96.dp),
        alertInfo = CardWarnInfo(
            icon = "",
            title = "黄色",
            content = "这是一条黄色预警这是一条黄色预警"
        )
    )
}

@LargeCardPreview
@Composable
fun AlertInfoListPreview() {
    AlertInfoList(
        modifier = Modifier.width(634.dp),
        alertInfoList = listOf(
            CardWarnInfo(
                icon = "",
                title = "黄色",
                content = "这是一条黄色预警这是一条黄色预警"
            ),
            CardWarnInfo(
                icon = "",
                title = "红色",
                content = "这是一条红色预警"
            )
        )
    )
}