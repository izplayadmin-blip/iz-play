package com.izplay.tv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.izplay.tv.ui.theme.IzRed
import com.izplay.tv.ui.theme.PanelDark
import com.izplay.tv.ui.theme.TextPrimary
import com.izplay.tv.ui.theme.TextSecondary

/**
 * Busca no padrão de TV (10-foot): o campo NÃO abre o teclado virtual quando o
 * foco do D-pad só passa por ele — vira digitação apenas ao apertar OK.
 * Concluir/Voltar fecham o teclado e devolvem o foco para o campo.
 * Corrige o incômodo da TV box de o teclado abrir sozinho no meio da navegação.
 */
@Composable
fun TvSearchField(
    query: String,
    placeholder: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    val rowFocus = remember { FocusRequester() }
    val fieldFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val f = rememberTvFocus()

    fun stopEditing() {
        if (!editing) return
        editing = false
        keyboard?.hide()
        runCatching { rowFocus.requestFocus() }
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (f.focused && !editing) IzRed.copy(alpha = 0.30f) else PanelDark)
            .focusRequester(rowFocus)
            .clickable(interactionSource = f.source, indication = null) {
                if (!editing) editing = true
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = if (editing) IzRed else TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        if (editing) {
            BackHandler { stopEditing() }
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(placeholder, color = TextSecondary, fontSize = 13.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(IzRed),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { stopEditing() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(fieldFocus)
                )
            }
            LaunchedEffect(Unit) {
                runCatching { fieldFocus.requestFocus() }
                keyboard?.show()
            }
        } else {
            Text(
                if (query.isEmpty()) placeholder else query,
                color = if (query.isEmpty()) TextSecondary else TextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (query.isNotEmpty()) {
                Text(
                    "OK edita",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
