package com.izplay.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.data.model.ProviderConfig
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.UiState
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.PanelBlack
import com.izplay.tv.ui.theme.PanelElevated
import com.izplay.tv.ui.theme.TextPrimary
import com.izplay.tv.ui.theme.TextSecondary

/** Tela de Configurações do IZ Play (Android). Mostra a conta atual, favoritos,
 *  info do app e o botão de trocar conta / sair. Só leitura + logout. */
@Composable
fun SettingsScreen(vm: MainViewModel, state: UiState) {
    val ctx = LocalContext.current
    val versionName = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull() ?: "—"
    }
    val cfg = state.config

    Column(
        Modifier
            .fillMaxSize()
            .background(PanelBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 26.dp)
    ) {
        Text("CONFIGURAÇÕES", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Conta, favoritos e informações do aplicativo.",
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        SettingsCard(title = "Conta / Provedor", icon = Icons.Filled.AccountCircle) {
            when {
                cfg == null -> InfoRow("Status", "Não configurado")
                cfg.mode == ProviderConfig.Mode.XTREAM -> {
                    InfoRow("Modo", "Xtream Codes")
                    InfoRow("Servidor", cfg.xtreamHost.ifBlank { "—" })
                    InfoRow("Usuário", cfg.xtreamUser.ifBlank { "—" })
                    InfoRow(
                        "Senha",
                        if (cfg.xtreamPass.isBlank()) "—"
                        else "•".repeat(cfg.xtreamPass.length.coerceAtMost(10))
                    )
                    if (cfg.epgUrl.isNotBlank()) InfoRow("EPG", cfg.epgUrl)
                }
                else -> {
                    InfoRow("Modo", "Lista M3U")
                    InfoRow("URL", cfg.m3uUrl.ifBlank { "—" })
                    if (cfg.epgUrl.isNotBlank()) InfoRow("EPG", cfg.epgUrl)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsCard(title = "Aplicativo", icon = Icons.Filled.Info) {
            InfoRow("Canais favoritos", state.favorites.size.toString())
            InfoRow("Canais carregados", state.allChannels.size.toString())
            InfoRow("Versão", "IZ Play • $versionName")
        }

        Spacer(Modifier.height(26.dp))

        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(IzRed)
                .clickable { vm.logout() }
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ExitToApp, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Trocar conta / Sair", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelElevated)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = IzRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                title.uppercase(),
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.width(16.dp))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
