package love.yu.jaychou.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import love.yu.jaychou.R

/**
 * Branded launch animation. It is drawn over the real app until settings and the album catalog
 * are ready, so the first frame is never an empty window.
 */
@Composable
fun JaySplashScreen() {
    val background = colorResource(R.color.splash_background)
    val titleColor = colorResource(R.color.splash_text)
    val variantColor = colorResource(R.color.splash_variant)
    val iconScale = remember { Animatable(.9f) }
    val textAlpha = remember { Animatable(.55f) }

    LaunchedEffect(Unit) {
        launch { iconScale.animateTo(1f, spring(dampingRatio = .7f, stiffness = 230f)) }
        textAlpha.animateTo(1f, tween(260, delayMillis = 80))
    }

    Box(
        Modifier.fillMaxSize().background(background)
            .semantics { contentDescription = "杰作 · Jay Archive 正在启动" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(280.dp).background(
                Brush.radialGradient(
                    listOf(Color(0xFF4979F5).copy(alpha = .12f), Color.Transparent),
                ),
                CircleShape,
            ),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.splash_icon),
                contentDescription = null,
                modifier = Modifier.size(124.dp).graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                },
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(22.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
            ) {
                BasicText(
                    "杰作 · Jay Archive",
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = .2.sp,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    "音乐档案",
                    style = TextStyle(color = variantColor, fontSize = 12.sp, letterSpacing = 1.6.sp),
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.width(28.dp).height(2.dp)
                        .background(Color(0xFF4979F5), RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
