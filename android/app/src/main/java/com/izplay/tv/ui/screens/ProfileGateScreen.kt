package com.izplay.tv.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.data.model.AppProfile
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.UiState
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.components.TvSearchField
import com.izplay.tv.ui.theme.*

private val avatarColors = listOf(
    Color(0xFFD11B36), Color(0xFF2389B8), Color(0xFF7448A8), Color(0xFFB27C68),
    Color(0xFFD99428), Color(0xFF36A7A2), Color(0xFFE98779), Color(0xFF168FC5),
    Color(0xFFD05D3E), Color(0xFF4D5FA4), Color(0xFF8A43A5), Color(0xFF557C74)
)
private data class AvatarStyle(val skin: Color, val hair: Color, val shirt: Color)
private val avatarStyles = listOf(
    AvatarStyle(Color(0xFFF1B38E), Color(0xFF20252B), Color(0xFF17364A)),
    AvatarStyle(Color(0xFFD9976D), Color(0xFF713A20), Color(0xFFB33575)),
    AvatarStyle(Color(0xFFFFC294), Color(0xFFF0A000), Color(0xFF743A94)),
    AvatarStyle(Color(0xFFB97655), Color(0xFF15313D), Color(0xFF174B67)),
    AvatarStyle(Color(0xFF8B543E), Color(0xFF171A20), Color(0xFF243B68)),
    AvatarStyle(Color(0xFFD79B75), Color(0xFF142D35), Color(0xFF155B65)),
    AvatarStyle(Color(0xFFA9684D), Color(0xFF6D3527), Color(0xFFF5EEE8)),
    AvatarStyle(Color(0xFFC27E58), Color(0xFF61351F), Color(0xFFC5203B)),
    AvatarStyle(Color(0xFFC7835D), Color(0xFF7A432D), Color(0xFF28624E)),
    AvatarStyle(Color(0xFFF0B18B), Color(0xFF202430), Color(0xFFC51E3B)),
    AvatarStyle(Color(0xFFB9775E), Color(0xFF693247), Color(0xFF27614E)),
    AvatarStyle(Color(0xFFD8A17F), Color(0xFF694531), Color(0xFF47736D))
)
private val profileGenres = listOf(
    "Ação", "Comédia", "Drama", "Romance",
    "Suspense", "Terror", "Ficção científica", "Animação",
    "Documentários", "Novelas", "Esportes", "Nacional"
)

@Composable
fun ProfileGateScreen(vm: MainViewModel, state: UiState) {
    val incompleteProfile = state.activeProfile?.takeIf { it.genres.size < 3 }
    var creating by remember(state.profiles.isEmpty(), incompleteProfile?.id) {
        mutableStateOf(state.profiles.isEmpty() || incompleteProfile != null)
    }
    BackHandler(enabled = state.profiles.isNotEmpty() && incompleteProfile == null) {
        if (creating) creating = false else vm.closeProfilePicker()
    }

    Box(Modifier.fillMaxSize().background(PanelBlack), contentAlignment = Alignment.Center) {
        if (creating) {
            CreateProfilePanel(
                canCancel = state.profiles.isNotEmpty() && incompleteProfile == null,
                title = if (incompleteProfile != null) "Conte um pouco sobre você" else "Crie seu perfil",
                initialName = incompleteProfile?.name.orEmpty(),
                initialAvatar = incompleteProfile?.avatar ?: 0,
                onCancel = { creating = false },
                onCreate = { name, avatar, genres ->
                    if (incompleteProfile != null) vm.completeProfile(incompleteProfile, name, avatar, genres)
                    else vm.createProfile(name, avatar, genres)
                }
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Quem está assistindo?", color = TextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text("Escolha um perfil para continuar", color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(30.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    state.profiles.forEach { profile ->
                        ProfileChoiceCard(
                            profile = profile,
                            selected = profile.id == state.activeProfileId,
                            onClick = { vm.selectProfile(profile) }
                        )
                    }
                    AddProfileChoice(enabled = state.profiles.size < 6) { creating = true }
                }
            }
        }
    }
}

@Composable
private fun CreateProfilePanel(
    canCancel: Boolean,
    title: String,
    initialName: String,
    initialAvatar: Int,
    onCancel: () -> Unit,
    onCreate: (String, Int, List<String>) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var avatar by remember(initialAvatar) { mutableIntStateOf(initialAvatar) }
    var genres by remember { mutableStateOf(setOf<String>()) }
    Column(
        Modifier.width(620.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = TextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(
            "Seu perfil mantém favoritos, histórico, progresso e recomendações separados.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(PanelElevated).padding(24.dp)
        ) {
            Text("ESCOLHA SEU AVATAR", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(154.dp)
            ) {
                items((0..11).toList()) { index ->
                    TvCard(onClick = { avatar = index }, shape = RoundedCornerShape(12.dp), focusScale = 1.08f) {
                        ProfileAvatar(index, selected = avatar == index, size = 64)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("NOME DO PERFIL", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            TvSearchField(
                query = name,
                placeholder = "Como devemos chamar você?",
                onQuery = { name = it.take(24) }
            )
            Spacer(Modifier.height(18.dp))
            Text("O QUE VOCÊ GOSTA DE ASSISTIR?", color = IzRed, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Escolha pelo menos 3 opções", color = TextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))
            profileGenres.chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { genre ->
                        val selected = genre in genres
                        TvCard(
                            onClick = {
                                genres = if (selected) genres - genre
                                else if (genres.size < 6) genres + genre else genres
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            focusScale = 1.04f
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(34.dp)
                                    .background(if (selected) IzRed else PanelDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    genre,
                                    color = if (selected) Color.White else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (canCancel) {
                ProfileButton("VOLTAR", danger = false, onClick = onCancel)
            }
            ProfileButton("CRIAR PERFIL", danger = true, enabled = name.isNotBlank() && genres.size >= 3) {
                onCreate(name, avatar, genres.toList())
            }
        }
    }
}

@Composable
private fun ProfileButton(
    label: String,
    danger: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TvCard(onClick = { if (enabled) onClick() }, shape = RoundedCornerShape(10.dp), focusScale = 1.04f) {
        Box(
            Modifier.width(160.dp).height(48.dp)
                .background(if (danger && enabled) IzRed else PanelElevated),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = if (enabled) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ProfileChoiceCard(profile: AppProfile, selected: Boolean, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(14.dp), focusScale = 1.06f) {
        Column(
            Modifier.width(142.dp).height(164.dp).background(PanelElevated).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ProfileAvatar(profile.avatar, selected, 92)
            Spacer(Modifier.height(10.dp))
            Text(profile.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AddProfileChoice(enabled: Boolean, onClick: () -> Unit) {
    TvCard(onClick = { if (enabled) onClick() }, shape = RoundedCornerShape(14.dp), focusScale = 1.06f) {
        Column(
            Modifier.width(142.dp).height(164.dp).background(PanelElevated).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier.size(92.dp).clip(RoundedCornerShape(16.dp)).background(PanelDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = if (enabled) IzRed else TextSecondary, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Adicionar perfil", color = if (enabled) TextPrimary else TextSecondary,
                fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
fun ProfileAvatar(index: Int, selected: Boolean, size: Int) {
    val safeIndex = index.coerceIn(0, avatarStyles.lastIndex)
    val style = avatarStyles[safeIndex]
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size / 6).dp))
            .background(avatarColors[safeIndex])
            .then(if (selected) Modifier.border(2.dp, IzRed, RoundedCornerShape((size / 6).dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding((size * 0.08f).dp)) {
            val w = this.size.width
            val h = this.size.height

            // Cabelo ao fundo e rosto levemente ovalado.
            drawCircle(style.hair, radius = w * 0.245f, center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.37f))
            drawOval(
                style.skin,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.31f, h * 0.22f),
                size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.43f)
            )

            // Franja assimétrica para dar personalidade sem depender de imagens externas.
            val fringe = Path().apply {
                moveTo(w * 0.30f, h * 0.34f)
                quadraticBezierTo(w * 0.39f, h * 0.12f, w * 0.66f, h * 0.20f)
                quadraticBezierTo(w * 0.60f, h * 0.34f, w * 0.48f, h * 0.31f)
                quadraticBezierTo(w * 0.38f, h * 0.40f, w * 0.30f, h * 0.34f)
                close()
            }
            drawPath(fringe, style.hair)

            drawCircle(Color(0xFF202124), w * 0.018f, androidx.compose.ui.geometry.Offset(w * 0.43f, h * 0.42f))
            drawCircle(Color(0xFF202124), w * 0.018f, androidx.compose.ui.geometry.Offset(w * 0.57f, h * 0.42f))
            drawArc(
                color = Color(0xFF8E3B38),
                startAngle = 12f,
                sweepAngle = 156f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.47f),
                size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.08f)
            )

            // Pescoço, camisa e gola branca.
            drawRect(style.skin, androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.58f), androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.10f))
            val torso = Path().apply {
                moveTo(w * 0.30f, h)
                lineTo(w * 0.35f, h * 0.70f)
                lineTo(w * 0.46f, h * 0.63f)
                lineTo(w * 0.54f, h * 0.63f)
                lineTo(w * 0.65f, h * 0.70f)
                lineTo(w * 0.70f, h)
                close()
            }
            drawPath(torso, style.shirt)
            val collar = Path().apply {
                moveTo(w * 0.40f, h * 0.66f)
                lineTo(w * 0.50f, h * 0.78f)
                lineTo(w * 0.60f, h * 0.66f)
                lineTo(w * 0.55f, h * 0.63f)
                lineTo(w * 0.50f, h * 0.71f)
                lineTo(w * 0.45f, h * 0.63f)
                close()
            }
            drawPath(collar, Color.White.copy(alpha = 0.92f))
        }
    }
}
