package com.mosi.mosc.launcher.ai_weather.compose.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mosi.mosc.ai_weather.weather_srv.CardExtraInfo
import com.mosi.mosc.launcher.ai_weather.compose.preview.LargeCardPreview
import com.mosi.mosc.launcher.ai_weather.compose.preview.SmallCardPreview
import com.mosi.mosc.launcher.ai_weather.compose.theme.AppTextStyles
import com.mosi.mosc.launcher.ai_weather.utils.Utils


@Composable
fun ExtraInfoList(
    modifier: Modifier = Modifier,
    extraInfoList: List<CardExtraInfo>,
    mode: String,
    horizontalPadding: Dp = 24.dp,
    verticalPadding: Dp = 48.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(verticalPadding)
    ) {

        items(extraInfoList) { item ->

            if (mode == "inline") {

                ExtraInfoInlineItem(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            } else {

                ExtraInfoStackItem(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ExtraInfoInlineItem(
    item: CardExtraInfo,
    modifier: Modifier = Modifier
) {
    val imageId = Utils.getExtraInfoImageResourceId(item.title)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = item.title,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.content ?: "null",
            style = AppTextStyles.ContentChinese_34,
            modifier = Modifier
                .size(180.dp, 50.dp)
                .alpha(0.9F),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExtraInfoStackItem(
    item: CardExtraInfo,
    modifier: Modifier = Modifier
) {
    val imageId = Utils.getExtraInfoImageResourceId(item.title)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = item.title,
            modifier = Modifier.size(80.dp).background(
                color = Color(0x1F4A83FF),
                shape = CircleShape
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = item.content ?: "null",
            style = AppTextStyles.ContentChinese_34,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(180.dp, 50.dp)
                .alpha(0.9F),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@SmallCardPreview
@Composable
fun ExtraInfoInlineItemPreview() {
    ExtraInfoInlineItem(
        item = CardExtraInfo("紫外线", "紫外线中等")
    )
}

@SmallCardPreview
@Composable
fun ExtraInfoStackItemPreview() {
    ExtraInfoStackItem(
        item = CardExtraInfo("紫外线", "紫外线中等")
    )
}

@SmallCardPreview
@Composable
fun ExtraInfoListPreview() {
    ExtraInfoList(
        extraInfoList = listOf(
            CardExtraInfo("紫外线", "紫外线中等"),
            CardExtraInfo("穿衣指数", "薄外套")
        ),
        mode = "inline",
        modifier = Modifier
            .background(
                color = Color(0x80FFFFFF),
                shape = RoundedCornerShape(36.dp)
            )
            .size(650.dp, 120.dp).padding(horizontal = 24.dp, vertical = 20.dp)
    )
}

@LargeCardPreview
@Composable
fun ExtraInfoStackListPreview() {
    ExtraInfoList(
        extraInfoList = listOf(
            CardExtraInfo("紫外线", "紫外线中等"),
            CardExtraInfo("穿衣指数", "薄外套"),
            CardExtraInfo("紫外线", "紫外线中等"),
            CardExtraInfo("穿衣指数", "薄外套"),
            CardExtraInfo("紫外线", "紫外线中等"),
            CardExtraInfo("穿衣指数", "薄外套")
        ),
        mode = "stack",
        modifier = Modifier
            .background(
                color = Color(0x80FFFFFF),
                shape = RoundedCornerShape(36.dp)
            )
            .size(650.dp, 632.dp).padding(horizontal = 57.dp, vertical = 48.dp)
    )
}