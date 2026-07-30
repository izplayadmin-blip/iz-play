package com.izplay.tv.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.R
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.IzRedDeep
import com.izplay.tv.ui.theme.TextSecondary

@Composable
fun LoadingScreen(
    message: String = "Preparando o IZ Play...",
    progress: Float = 0f,
    onRetry: (() -> Unit)? = null,
    onChangeAccess: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320),
        label = "startupProgress"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(IzRedDeep.copy(alpha = 0.28f), Color.Black), radius = 920f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.izplay_logo_login),
                contentDescription = "IZ Play",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(300.dp)
            )
            Spacer(Modifier.height(34.dp))
            Box(
                Modifier
                    .width(260.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.13f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceAtLeast(0.03f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(IzRed, Color(0xFFFF5A62))))
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(message, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(
                "${(animatedProgress * 100).toInt()}%",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (onRetry != null) {
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvCard(onClick = onRetry, shape = RoundedCornerShape(10.dp), focusScale = 1.05f) {
                        Box(
                            Modifier.width(180.dp).height(48.dp).background(IzRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("TENTAR NOVAMENTE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    if (onChangeAccess != null) {
                        TvCard(onClick = onChangeAccess, shape = RoundedCornerShape(10.dp), focusScale = 1.05f) {
                            Box(
                                Modifier.width(180.dp).height(48.dp).background(Color(0xFF252525)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("ALTERAR ACESSO", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
