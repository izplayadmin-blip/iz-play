package com.izplay.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.izplay.tv.data.model.ProviderConfig
import com.izplay.tv.data.remote.AppDns
import com.izplay.tv.ui.MainViewModel
import com.izplay.tv.ui.UiState
import com.izplay.tv.ui.components.TvCard
import com.izplay.tv.ui.theme.Divider
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.Muted
import com.izplay.tv.ui.theme.PanelBlack
import com.izplay.tv.ui.theme.PanelDark
import com.izplay.tv.ui.theme.TextPrimary
import com.izplay.tv.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    state: UiState,
    onExitToSidebar: () -> Unit,
) {
    val ctx = LocalContext.current
    val versionName = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
            .getOrNull() ?: "-"
    }
    val now = remember { Date() }
    val clock = remember { SimpleDateFormat("HH:mm:ss", Locale("pt", "BR")).format(now) }
    val date = remember { SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(now) }
    val cfg = state.config
    val central = state.clientConfig
    val preferences = state.playbackPreferences
    var picker by remember { mutableStateOf<PickerSpec?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(PanelBlack)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    onExitToSidebar()
                    true
                } else {
                    false
                }
            }
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text("Configuracoes", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Column(horizontalAlignment = Alignment.End) {
                Text(clock, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(date, color = Muted, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(36.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSection("DNS", Modifier.weight(1f)) {
                SettingSelect("DNS do aplicativo", preferences.dnsProvider) {
                    picker = PickerSpec(
                        "Servidor DNS",
                        AppDns.options,
                        preferences.dnsProvider
                    ) { selected ->
                        vm.updatePlaybackPreferences { current ->
                            current.copy(dnsProvider = selected)
                        }
                    }
                }
                SettingLine(
                    "Config central",
                    "Gerenciada com segurança pelo aplicativo",
                    if (central != null) "Ativa" else "Local"
                )
                RedButton("Testar conexao") { vm.checkInfra() }
            }
            SettingsSection("Player", Modifier.weight(1f), height = 340.dp) {
                SettingSelect("Player padrao", preferences.player) {
                    picker = PickerSpec("Player padrão", listOf("Interno"), preferences.player) {
                        vm.updatePlaybackPreferences { current -> current.copy(player = it) }
                    }
                }
                SettingSelect("Qualidade preferida", preferences.quality) {
                    picker = PickerSpec(
                        "Qualidade preferida",
                        listOf("Automática", "Original"),
                        preferences.quality
                    ) { vm.updatePlaybackPreferences { current -> current.copy(quality = it) } }
                }
                SettingSelect("Tipo de stream", preferences.streamType) {
                    picker = PickerSpec(
                        "Tipo de stream",
                        listOf("Automático", "HLS", "MPEG-TS"),
                        preferences.streamType
                    ) { vm.updatePlaybackPreferences { current -> current.copy(streamType = it) } }
                }
                SettingSelect("Buffer (segundos)", "${preferences.bufferSeconds}s") {
                    picker = PickerSpec(
                        "Buffer",
                        listOf("5s", "10s", "15s", "30s"),
                        "${preferences.bufferSeconds}s"
                    ) { selected ->
                        vm.updatePlaybackPreferences { current ->
                            current.copy(bufferSeconds = selected.removeSuffix("s").toInt())
                        }
                    }
                }
                SettingToggle(
                    "Fallback automatico MPV",
                    "H.265/4K sem suporte",
                    preferences.fallbackMpv
                ) { checked ->
                    vm.updatePlaybackPreferences { current -> current.copy(fallbackMpv = checked) }
                }
            }
            SettingsSection("Controle Parental", Modifier.weight(1f)) {
                SettingToggle(
                    "Ativar controle parental",
                    "Protege conteúdo adulto",
                    preferences.parentalControl
                ) { checked ->
                    vm.updatePlaybackPreferences { current -> current.copy(parentalControl = checked) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSection("Teste de velocidade", Modifier.weight(1f)) {
                SettingLine(
                    "Cloudflare",
                    "Mede a conexao deste dispositivo",
                    if (state.connection.testing) "Testando" else "Pronto",
                    mutedBadge = true
                )
                RedButton(
                    if (state.connection.testing) "Testando..." else "Iniciar teste",
                    enabled = !state.connection.testing
                ) { vm.runSpeedTest() }
                ResultBox(connectionResult(state))
            }
            SettingsSection("Conteudo e cache", Modifier.weight(1f)) {
                Text("Atualizar conteudo", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("Recarrega canais, filmes e series", color = Muted, fontSize = 11.sp)
                RedButton("Atualizar conteudo") { cfg?.let(vm::saveAndLoad) }
                Spacer(Modifier.height(12.dp))
                Text("Limpar cache", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("Remove cache temporario sem apagar login", color = Muted, fontSize = 11.sp)
                DarkButton("Limpar cache") { vm.clearContentCache() }
            }
            SettingsSection("Conta", Modifier.weight(1f)) {
                when {
                    cfg == null -> SettingLine("Sessao atual", "Conta nao configurada", "Inativa", mutedBadge = true)
                    cfg.mode == ProviderConfig.Mode.XTREAM -> {
                        SettingLine("Sessao atual", "Troque o acesso na aba Usuario", "Ativa")
                        InfoValue("Usuario", maskAccount(cfg.xtreamUser))
                        InfoValue("Tipo de acesso", "Xtream")
                    }
                    else -> {
                        SettingLine("Sessao atual", "Lista M3U carregada", "Ativa")
                        InfoValue("Modo", "M3U")
                    }
                }
                InfoValue("Versao", versionName)
            }
        }
    }

    picker?.let { spec ->
        PickerDialog(
            spec = spec,
            onDismiss = { picker = null },
            onSelect = { value ->
                spec.onSelect(value)
                picker = null
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 270.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(PanelDark)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
    ) {
        Text(
            title,
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .border(0.dp, Divider)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        )
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingLine(title: String, subtitle: String, badge: String, mutedBadge: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Badge(badge, mutedBadge)
    }
}

@Composable
private fun Badge(text: String, muted: Boolean = false) {
    Text(
        text,
        color = if (muted) Muted else Color(0xFF20D46B),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (muted) Color.White.copy(alpha = 0.08f) else Color(0xFF0D3A21))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

@Composable
private fun SettingSelect(label: String, value: String, onClick: () -> Unit) {
    TvCard(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        focusScale = 1.03f
    ) { focused ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                value.ifBlank { " " },
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun SettingToggle(label: String, subtitle: String, on: Boolean, onChange: (Boolean) -> Unit) {
    TvCard(
        onClick = { onChange(!on) },
        shape = RoundedCornerShape(8.dp),
        focusScale = 1.03f
    ) { focused ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 11.sp, maxLines = 2)
            }
            Box(
                Modifier
                    .width(42.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) IzRed else Color.White.copy(alpha = 0.08f))
                    .padding(3.dp),
                contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.White))
            }
        }
    }
}

@Composable
private fun RedButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TvCard(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(8.dp),
        focusScale = 1.04f,
        focusBorderColor = Color.White
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(if (enabled) IzRed else IzRed.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DarkButton(label: String, onClick: () -> Unit) {
    TvCard(onClick = onClick, shape = RoundedCornerShape(8.dp), focusScale = 1.02f) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Color.Transparent)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = TextSecondary, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResultBox(text: String) {
    Text(
        text,
        color = Muted,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

@Composable
private fun InfoValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private data class PickerSpec(
    val title: String,
    val options: List<String>,
    val selected: String,
    val onSelect: (String) -> Unit
)

@Composable
private fun PickerDialog(spec: PickerSpec, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .widthIn(min = 360.dp, max = 520.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PanelDark)
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(spec.title, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
            spec.options.forEach { option ->
                TvCard(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(8.dp),
                    focusScale = 1.03f
                ) { focused ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    focused -> IzRed
                                    option == spec.selected -> Color.White.copy(alpha = 0.10f)
                                    else -> Color.Transparent
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option, color = TextPrimary, fontWeight = FontWeight.Bold)
                        if (option == spec.selected) Text("Atual", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun maskAccount(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) return "-"
    if (clean.length <= 4) return "••••"
    return clean.take(2) + "•".repeat((clean.length - 4).coerceAtMost(6)) + clean.takeLast(2)
}

private fun connectionResult(state: UiState): String {
    val result = state.connection
    if (result.testing) return "Medindo conexão..."
    if (result.lastTestAt == 0L) return "Nenhum teste executado."
    return "Download %.1f Mbps · Upload %.1f Mbps · Ping %.0f ms"
        .format(result.downloadMbps, result.uploadMbps, result.pingMs)
}
