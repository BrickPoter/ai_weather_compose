package com.mosi.mosc.launcher.ai_weather.compose.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mosi.mosc.launcher.ai_weather.R
import com.mosi.mosc.launcher.ai_weather.compose.preview.SmallCardPreview
import com.mosi.mosc.launcher.ai_weather.ui.theme.AppTextStyles

@Composable
fun Header(
    modifier: Modifier = Modifier,
    role: String?,
    location: String?,
    date: String?
           ) {
    Row(
        modifier = modifier.height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var startPadding = 8.dp
        if (role.isNullOrBlank()) {
            startPadding = 16.dp
        } else {
            Icon(
                modifier = Modifier.size(64.dp).alpha(0.6F),
                painter = painterResource(getIcon(role)),
                contentDescription = "图标"
            )
        }

        Text(
            modifier = Modifier.padding(start = startPadding).alpha(0.6F),
            text = location ?: "null",
            style = AppTextStyles.ContentChinese_34
        )
        if (date != null) {
            Text(
                modifier = Modifier.alpha(0.6F),
                text = "·$date",
                style = AppTextStyles.ContentChinese_34
            )
        }
    }
}


private fun getIcon(role: String?): Int = when(role) {
    "target" -> R.drawable.icon_navigation_target
    else  -> R.drawable.icon_navigation_location
}

@SmallCardPreview
@Composable
fun HeaderPreview() {
    Header(
        role = null,
        location = "成都市",
        date = "10月15日"
    )
}