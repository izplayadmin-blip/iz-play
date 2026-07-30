package com.izplay.tv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.R
import com.izplay.tv.data.model.ProviderConfig
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.rememberTvFocus
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.IzRedDark
import com.izplay.tv.ui.theme.IzRedDeep
import com.izplay.tv.ui.theme.PanelBlack
import com.izplay.tv.ui.theme.TextPrimary
import com.izplay.tv.ui.theme.TextSecondary

@Composable
fun SetupScreen(onLogin: (String, String) -> Unit, onM3u: (String) -> Unit) {
    var mode by remember { mutableStateOf(ProviderConfig.Mode.XTREAM) }
    var m3u by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val canSubmit = if (mode == ProviderConfig.Mode.XTREAM)
        user.isNotBlank() && pass.isNotBlank()
    else m3u.isNotBlank()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color.Black, IzRedDeep.copy(alpha = 0.34f), PanelBlack, Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.izplay_logo_login),
                    contentDescription = "IZ Play",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.68f)
                )
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ENTRETENIMENTO", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 6.sp)
                    Text("SEM LIMITES", color = IzRed, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 6.sp)
                }
            }

            Box(
                Modifier
                    .width(2.dp)
                    .height(520.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, IzRed, Color.Transparent)))
            )

            Column(
                Modifier
                    .width(480.dp)
                    .padding(start = 70.dp)
            ) {
                if (mode == ProviderConfig.Mode.XTREAM) {
                    LoginField("Usuario", user, Icons.Filled.Person) { user = it }
                    Spacer(Modifier.height(18.dp))
                    LoginField(
                        "Senha",
                        pass,
                        Icons.Filled.Lock,
                        isPassword = true,
                        passVisible = showPass,
                        onTogglePass = { showPass = !showPass }
                    ) { pass = it }
                } else {
                    LoginField("URL da lista M3U/M3U8", m3u, Icons.Filled.Storage) { m3u = it }
                }

                Spacer(Modifier.height(22.dp))

                // Botão vermelho sobre fundo vermelho: o foco ganha borda BRANCA
                // (a borda vermelha padrão do TvCard sumiria) + leve zoom.
                TvCard(
                    onClick = {
                        if (canSubmit) {
                            if (mode == ProviderConfig.Mode.XTREAM) onLogin(user, pass)
                            else onM3u(m3u)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    focusScale = 1.02f
                ) { focused ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(if (canSubmit) Brush.verticalGradient(listOf(Color(0xFFE8222C), Color(0xFFC8121B))) else Brush.verticalGradient(listOf(IzRedDark, IzRedDark)))
                            .then(if (focused) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ENTRAR", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 1.sp)
                        Spacer(Modifier.width(14.dp))
                        Icon(Icons.Filled.ArrowForward, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun LoginField(
    placeholder: String,
    value: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passVisible: Boolean = false,
    onTogglePass: (() -> Unit)? = null,
    onChange: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC181A1E))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(IzRed),
            visualTransformation = if (isPassword && !passVisible) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = TextSecondary, fontSize = 16.sp)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
        if (isPassword && onTogglePass != null) {
            val eyeFocus = rememberTvFocus()
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (eyeFocus.focused) IzRed else Color.Transparent)
                    .clickable(interactionSource = eyeFocus.source, indication = null) { onTogglePass() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Mostrar senha",
                    tint = if (eyeFocus.focused) Color.White else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
