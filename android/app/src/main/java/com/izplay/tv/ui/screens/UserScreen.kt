package com.izplay.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.data.model.ProviderConfig
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.UiState
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.theme.Divider
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.IzRedDark
import com.izplay.tv.ui.theme.Muted
import com.izplay.tv.ui.theme.PanelBlack
import com.izplay.tv.ui.theme.PanelElevated
import com.izplay.tv.ui.theme.TextPrimary
import com.izplay.tv.ui.theme.TextSecondary

@Composable
fun UserScreen(vm: MainViewModel, state: UiState) {
    val ctx = LocalContext.current
    val versionName = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull() ?: "-"
    }
    val cfg = state.config
    val accountCode = when {
        cfg?.mode == ProviderConfig.Mode.XTREAM -> cfg.xtreamUser.ifBlank { "-" }
        cfg?.mode == ProviderConfig.Mode.M3U -> "LISTA M3U"
        else -> "-"
    }
    val profileName = state.activeProfile?.name ?: "Nenhum perfil"

    Column(
        Modifier
            .fillMaxSize()
            .background(PanelBlack)
            .padding(horizontal = 48.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(accountCode, color = TextPrimary, fontSize = 54.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text("CÓDIGO DA CONTA", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.widthIn(max = 780.dp)) {
            AccountCard("Perfil", profileName, Modifier.weight(1f))
            AccountCard("Dispositivo", "Android TV", Modifier.weight(1f))
            AccountCard("Versao", versionName, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.widthIn(max = 780.dp)) {
            AccountCard("Status", if (state.configured) "Ativa" else "Sem login", Modifier.weight(1f))
            AccountCard("Canais", state.allChannels.size.toString(), Modifier.weight(1f))
            AccountCard("Favoritos", state.favorites.size.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            UserAction(Icons.Filled.Refresh, "Atualizar conteudo") {
                cfg?.let(vm::saveAndLoad)
            }
            UserAction(Icons.Filled.SwitchAccount, "Trocar perfil") {
                vm.openProfilePicker()
            }
            UserAction(Icons.Filled.ExitToApp, "Sair da conta", danger = true) {
                vm.logout()
            }
        }

        Spacer(Modifier.height(30.dp))
        Text("Perfis desta conta", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 25.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Cada perfil mantém favoritos, histórico, progresso e recomendações separados.",
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(22.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            state.profiles.forEach { profile ->
                ProfileChoiceCard(
                    profile = profile,
                    selected = profile.id == state.activeProfileId,
                    onClick = { vm.selectProfile(profile) }
                )
            }
            AddProfileCard(onClick = vm::openProfilePicker)
        }
    }
}

@Composable
private fun AccountCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(86.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF10141C))
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label.uppercase(), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun UserAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val bg = if (danger) IzRed else PanelElevated
    val border = if (danger) IzRed else Divider
    TvCard(onClick = onClick, shape = RoundedCornerShape(9.dp), focusScale = 1.03f) {
        Row(
            Modifier
                .height(48.dp)
                .widthIn(min = 176.dp)
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(9.dp))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text(label, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ProfileCard(name: String, selected: Boolean) {
    TvCard(onClick = {}, shape = RoundedCornerShape(10.dp), focusScale = 1.04f) {
        Column(
            Modifier
                .width(160.dp)
                .height(170.dp)
                .background(PanelElevated)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Avatar(selected)
            Spacer(Modifier.height(10.dp))
            Text(name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(10.dp), focusScale = 1.04f) {
        Column(
            Modifier
                .width(160.dp)
                .height(170.dp)
                .background(PanelElevated)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, IzRed, RoundedCornerShape(16.dp))
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, null, tint = IzRed, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Adicionar", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Avatar(selected: Boolean) {
    Box(
        Modifier
            .size(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFC9162A), IzRedDark)))
            .then(if (selected) Modifier.border(2.dp, IzRed, RoundedCornerShape(16.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text("IZ", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(IzRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Edit, null, tint = TextPrimary, modifier = Modifier.size(15.dp))
        }
    }
}
