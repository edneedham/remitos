package com.remitos.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.remitos.app.ui.theme.Tokens

/* Animated shimmer placeholder that mirrors `animate-pulse` from
 * website/.../PanelSkeletons.tsx. Use it as a sized stand-in for content that
 * is loading. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    radius: Dp = Tokens.Radius.sm,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-alpha",
    )
    val base = Tokens.Color.n200
    val color = base.copy(alpha = alpha)

    val sized = if (width != null) {
        modifier.width(width).height(height)
    } else {
        modifier.fillMaxWidth().height(height)
    }

    Box(
        modifier = sized
            .semantics { } // hint: decorative
            .clip(RoundedCornerShape(radius))
            .background(color),
    )
}

@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 12.dp,
) {
    ShimmerBox(modifier = modifier, height = height, width = width)
}

/** A common "list row" placeholder. */
@Composable
fun ListRowSkeleton(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerBox(
            modifier = Modifier.size(40.dp),
            height = 40.dp,
            width = 40.dp,
            radius = Tokens.Radius.sm,
        )
        Column(
            modifier = Modifier
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShimmerLine(width = 180.dp, height = 14.dp)
            ShimmerLine(width = 120.dp, height = 10.dp)
        }
    }
}

/** A vertical stack of `count` list-row skeletons, separated by dividers. */
@Composable
fun ListSkeleton(
    count: Int = 6,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(count) {
            ListRowSkeleton()
            if (it < count - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x14000000)),
                )
            }
        }
    }
}
