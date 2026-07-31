package com.izplay.tv.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.rememberTvFocus
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.TextPrimary
import com.izplay.tv.ui.theme.TextSecondary

/** Login oficial IZ Play para Android TV. A autenticação permanece Xtream;
 * somente a apresentação e a navegação por foco são responsabilidade desta tela. */
@Composable
fun SetupScreen(
    onLogin: (String, String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onM3u: (String) -> Unit,
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    val userFocusRequester = remember { FocusRequester() }
    val canSubmit = user.isNotBlank() && pass.isNotBlank()

    LaunchedEffect(Unit) {
        runCatching { userFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A0A0A), Color.Black, Color(0xFF050505), Color.Black),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        LoginAtmosphere()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 64.dp, vertical = 42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.iz_sidebar_brand_official),
                    contentDescription = "IZ Play",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.66f),
                )
                Spacer(Modifier.height(26.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.72f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, IzRed, Color.Transparent),
                            ),
                        ),
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "ENTRETENIMENTO",
                        color = TextPrimary.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 5.sp,
                    )
                    Text(
                        "SEM LIMITES",
                        color = IzRed,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 5.sp,
                    )
                }
            }

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.88f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, IzRed.copy(alpha = 0.88f), Color.Transparent),
                        ),
                    ),
            )

            Box(
                modifier = Modifier
                    .padding(start = 54.dp)
                    .width(560.dp)
                    .heightIn(min = 470.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xEC1B1B1B), Color(0xF20B0B0B), Color(0xFA020202)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 38.dp, vertical = 70.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    LoginField(
                        placeholder = "Usuário",
                        value = user,
                        icon = Icons.Filled.Person,
                        modifier = Modifier.focusRequester(userFocusRequester),
                        onChange = { user = it },
                    )
                    Spacer(Modifier.height(22.dp))
                    LoginField(
                        placeholder = "Senha",
                        value = pass,
                        icon = Icons.Filled.Lock,
                        isPassword = true,
                        passVisible = showPass,
                        onTogglePass = { showPass = !showPass },
                        onChange = { pass = it },
                    )
                    Spacer(Modifier.height(40.dp))

                    TvCard(
                        onClick = { if (canSubmit) onLogin(user, pass) },
                        shape = RoundedCornerShape(12.dp),
                        focusScale = 1.015f,
                    ) { focused ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF121212), Color(0xFF020202)),
                                    ),
                                )
                                .border(
                                    width = if (focused) 2.dp else 1.dp,
                                    color = if (focused) Color.White
                                    else IzRed.copy(alpha = if (canSubmit) 0.94f else 0.38f),
                                    shape = RoundedCornerShape(12.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "ENTRAR",
                                color = TextPrimary.copy(alpha = if (canSubmit) 1f else 0.46f),
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp,
                                letterSpacing = 3.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginAtmosphere() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.08f, 0f),
                radius = size.width * 0.42f,
            ),
        )
        val sweep = Path().apply {
            moveTo(0f, size.height * 0.68f)
            cubicTo(
                size.width * 0.18f,
                size.height * 0.77f,
                size.width * 0.31f,
                size.height * 0.90f,
                size.width * 0.46f,
                size.height * 0.82f,
            )
        }
        drawPath(
            path = sweep,
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.16f),
                    IzRed.copy(alpha = 0.46f),
                    Color.Transparent,
                ),
            ),
            style = Stroke(width = 2.2f),
        )
    }
}

@Composable
private fun LoginField(
    placeholder: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passVisible: Boolean = false,
    onTogglePass: (() -> Unit)? = null,
    onChange: (String) -> Unit,
) {
    val fieldFocus = rememberTvFocus()
    Row(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(listOf(Color(0xF0202020), Color(0xEA131313))),
            )
            .border(
                width = if (fieldFocus.focused) 2.dp else 1.dp,
                color = if (fieldFocus.focused) IzRed else Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = TextPrimary.copy(alpha = 0.76f), modifier = Modifier.size(27.dp))
        Spacer(Modifier.width(18.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            interactionSource = fieldFocus.source,
            textStyle = TextStyle(color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(IzRed),
            visualTransformation = if (isPassword && !passVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = TextSecondary, fontSize = 17.sp)
                inner()
            },
            modifier = Modifier.weight(1f),
        )
        if (isPassword && onTogglePass != null) {
            val eyeFocus = rememberTvFocus()
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (eyeFocus.focused) IzRed else Color.Transparent)
                    .clickable(interactionSource = eyeFocus.source, indication = null) { onTogglePass() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Mostrar senha",
                    tint = if (eyeFocus.focused) Color.White else TextPrimary.copy(alpha = 0.76f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
