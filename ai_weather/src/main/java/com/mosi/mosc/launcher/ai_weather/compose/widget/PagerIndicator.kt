package com.mosi.mosc.launcher.ai_weather.compose.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.LightGray,
    inactiveColor: Color = Color.DarkGray,
    selectedWidth: Dp = 24.dp,
    unselectedWidth: Dp = 16.dp,
    spacing: Dp = 10.dp
) {
    Row(
        modifier = modifier.height(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { index ->

            val isActive = pagerState.currentPage == index

            val color by animateColorAsState(
                targetValue = if (isActive) activeColor else inactiveColor,
                label = "indicatorColo"
            )

            val width by animateDpAsState(
                targetValue = if (isActive) selectedWidth else unselectedWidth,
                label = "indicatorSize"
            )

            val padding = if (index == 0) 0.dp else spacing

            Box(
                modifier = Modifier
                    .padding(start = padding)
                    .clip(CircleShape).size(width)
                    .background(color)
            )
        }
    }
}