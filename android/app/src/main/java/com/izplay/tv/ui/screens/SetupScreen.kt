package com.izplay.tv.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.izplay.tv.ui.theme.*

/**
 * Tela de login (setup) no layout dividido do IZ Play: marca à esquerda + formulário à direita,
 * igual ao web/desktop. Xtream (servidor/usuário/senha) ou lista M3U.
 */
@Composable
fun SetupScreen(onLogin: (String, String) -> Unit, onM3u: (String) -> Unit) {
    var mode by remember { mutableStateOf(ProviderConfig.Mode.XTREAM) }
    var m3u by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    // O host do provedor vem do Painel Admin (via config remota) — o usuário só digita
    // usuário e senha, igual ao web/desktop.
    val canSubmit = if (mode == ProviderConfig.Mode.XTREAM) user.isNotBlank() && pass.isNotBlank()
    else m3u.isNotBlank()

    Row(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    0f to IzRedDeep.copy(alpha = 0.5f),
                    0.45f to PanelBlack,
                    1f to PanelBlack
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── ESQUERDA: marca ──
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(36.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.izplay_logo_login),
                    contentDescription = "IZ Play",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )
                Spacer(Modifier.height(20.dp))
                Row {
                    Text(
                        "ENTRETENIMENTO ",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 4.sp
                    )
                    Text(
                        "SEM LIMITES",
                        color = IzRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 4.sp
                    )
                }
            }
        }

        // ── DIVISOR vermelho central ──
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight(0.62f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, IzRed, Color.Transparent)))
        )

        // ── DIREITA: formulário ──
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
            ) {
                if (mode == ProviderConfig.Mode.XTREAM) {
                    LoginField("Usuário", user, Icons.Filled.Person) { user = it }
                    Spacer(Modifier.height(14.dp))
                    LoginField(
                        "Senha", pass, Icons.Filled.Lock,
                        isPassword = true, passVisible = showPass,
                        onTogglePass = { showPass = !showPass }
                    ) { pass = it }
                } else {
                    LoginField("URL da lista M3U/M3U8", m3u, Icons.Filled.Storage) { m3u = it }
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSubmit) IzRed else IzRedDeep)
                        .clickable(enabled = canSubmit) {
                            if (mode == ProviderConfig.Mode.XTREAM) onLogin(user, pass) else onM3u(m3u)
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ENTRAR",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.ArrowForward, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    if (mode == ProviderConfig.Mode.XTREAM) "Usar lista M3U" else "Usar login Xtream",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            mode = if (mode == ProviderConfig.Mode.XTREAM)
                                ProviderConfig.Mode.M3U else ProviderConfig.Mode.XTREAM
                        }
                )
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
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelElevated)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(IzRed),
            visualTransformation = if (isPassword && !passVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = TextSecondary, fontSize = 16.sp)
                inner()
            },
            modifier = Modifier.weight(1f)
        )
        if (isPassword && onTogglePass != null) {
            Icon(
                if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = "Mostrar senha",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp).clickable { onTogglePass() }
            )
        }
    }
}
