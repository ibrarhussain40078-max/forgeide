package com.example.ui.simulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CodeFile
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RoseError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

class SimulatorController {
    val backStack = mutableStateListOf<String>()
    var toastMessage by mutableStateOf<String?>(null)
    var resetSignal by mutableStateOf(0)

    fun navigateBack(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
            false
        }
    }

    fun navigateHome(startScreen: String) {
        backStack.clear()
        backStack.add(startScreen)
    }

    fun showToast(msg: String) {
        toastMessage = msg
    }
}

@Composable
fun AndroidUiSimulatorScreen(
    files: List<CodeFile>,
    refreshNonce: Long,
    controller: SimulatorController,
    modifier: Modifier = Modifier
) {
    val previewFile = files.firstOrNull { it.name.equals("ui-preview.json", ignoreCase = true) }

    val parsedConfig = remember(previewFile?.content, refreshNonce) {
        runCatching {
            previewFile?.content?.let { JSONObject(it) }
        }
    }

    val jsonRoot = parsedConfig.getOrNull()
    val parseError = parsedConfig.exceptionOrNull()

    if (jsonRoot == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianVoid)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = RoseError,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (previewFile == null) {
                        "Add ui-preview.json to configure live interactive preview."
                    } else {
                        "ui-preview.json syntax error:\n${parseError?.localizedMessage ?: "Invalid JSON"}"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val accentColor = remember(jsonRoot) {
        val hex = jsonRoot.optString("primaryColor", "#38BDF8")
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(CyanElectric)
    }
    val startScreen = remember(jsonRoot) {
        jsonRoot.optString("startScreen", "home").ifBlank { "home" }
    }

    val stateMap = remember(jsonRoot, controller.resetSignal) {
        mutableStateMapOf<String, Any>().apply {
            val initObj = jsonRoot.optJSONObject("initialState")
            if (initObj != null) {
                initObj.keys().forEach { key ->
                    put(key, initObj.get(key))
                }
            }
        }
    }

    val listMap = remember(jsonRoot, controller.resetSignal) {
        mutableStateMapOf<String, MutableList<String>>().apply {
            val initLists = jsonRoot.optJSONObject("initialLists")
            if (initLists != null) {
                initLists.keys().forEach { key ->
                    val arr = initLists.optJSONArray(key)
                    val items = mutableListOf<String>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            items.add(arr.optString(i))
                        }
                    }
                    put(key, items)
                }
            }
        }
    }

    LaunchedEffect(jsonRoot) {
        if (controller.backStack.isEmpty() || jsonRoot.optJSONObject("screens")?.has(controller.backStack.last()) != true) {
            controller.backStack.clear()
            controller.backStack.add(startScreen)
        }
    }

    LaunchedEffect(controller.toastMessage) {
        if (controller.toastMessage != null) {
            delay(2200L)
            controller.toastMessage = null
        }
    }

    val screensObj = jsonRoot.optJSONObject("screens")
    val currentScreenKey = controller.backStack.lastOrNull() ?: startScreen
    val screenObj = screensObj?.optJSONObject(currentScreenKey)
        ?: screensObj?.keys()?.let { if (it.hasNext()) screensObj.optJSONObject(it.next()) else null }

    fun resolveTemplate(raw: String): String {
        var out = raw
        val regex = Regex("""\{\{([a-zA-Z0-9_]+)\}\}""")
        regex.findAll(raw).forEach { m ->
            val key = m.groupValues[1]
            val value = stateMap[key]?.toString() ?: ""
            out = out.replace(m.value, value)
        }
        return out
    }

    fun handleAction(el: JSONObject) {
        when (el.optString("action")) {
            "increment" -> {
                val key = el.optString("key")
                val step = el.optDouble("step", 1.0)
                val curr = (stateMap[key] as? Number)?.toDouble() ?: stateMap[key]?.toString()?.toDoubleOrNull() ?: 0.0
                val next = curr + step
                stateMap[key] = if (next % 1.0 == 0.0) next.toInt() else next
            }
            "decrement" -> {
                val key = el.optString("key")
                val step = el.optDouble("step", 1.0)
                val curr = (stateMap[key] as? Number)?.toDouble() ?: stateMap[key]?.toString()?.toDoubleOrNull() ?: 0.0
                val next = (curr - step).coerceAtLeast(0.0)
                stateMap[key] = if (next % 1.0 == 0.0) next.toInt() else next
            }
            "set" -> {
                val key = el.optString("key")
                if (el.has("value")) {
                    stateMap[key] = el.get("value")
                }
            }
            "toggle" -> {
                val key = el.optString("key")
                val curr = stateMap[key] as? Boolean ?: false
                stateMap[key] = !curr
            }
            "addListItem" -> {
                val listKey = el.optString("listKey")
                val inputKey = el.optString("inputKey")
                val text = stateMap[inputKey]?.toString()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    val existing = listMap[listKey]?.toMutableList() ?: mutableListOf()
                    existing.add(0, text)
                    listMap[listKey] = existing
                    stateMap[inputKey] = ""
                    controller.showToast("Added: $text")
                } else {
                    controller.showToast("Enter text first")
                }
            }
            "navigate" -> {
                val target = el.optString("target")
                if (target.isNotBlank() && screensObj?.has(target) == true) {
                    controller.backStack.add(target)
                }
            }
            "back" -> {
                controller.navigateBack()
            }
            "toast" -> {
                val msg = resolveTemplate(el.optString("message", "Action triggered"))
                controller.showToast(msg)
            }
            "rollDice" -> {
                val roll = kotlin.random.Random.nextInt(1, 7)
                val diceKey = el.optString("key", "dice")
                stateMap[diceKey] = roll
                val scoreKey = el.optString("scoreKey")
                if (scoreKey.isNotBlank()) {
                    val currScore = (stateMap[scoreKey] as? Number)?.toInt()
                        ?: stateMap[scoreKey]?.toString()?.toIntOrNull() ?: 0
                    stateMap[scoreKey] = currScore + roll
                }
                val turnKey = el.optString("turnKey")
                val nextTurn = el.optString("nextTurn")
                if (turnKey.isNotBlank() && nextTurn.isNotBlank()) {
                    stateMap[turnKey] = nextTurn
                }
                val movesList = listMap["moves"]?.toMutableList() ?: mutableListOf()
                movesList.add(0, "Rolled a $roll (+$roll steps)")
                listMap["moves"] = movesList.take(12).toMutableList()
                controller.showToast("Rolled $roll!")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated App Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (controller.backStack.size > 1) {
                    IconButton(
                        onClick = { controller.navigateBack() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = screenObj?.optString("title").orEmpty()
                        .ifBlank { jsonRoot.optString("appName", "Android App") },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)

            // Simulated Screen Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val elements = screenObj?.optJSONArray("elements") ?: JSONArray()
                for (i in 0 until elements.length()) {
                    val el = elements.optJSONObject(i) ?: continue
                    SimulatorElement(
                        el = el,
                        accentColor = accentColor,
                        stateMap = stateMap,
                        listMap = listMap,
                        resolveTemplate = ::resolveTemplate,
                        onAction = ::handleAction,
                        onShowToast = controller::showToast
                    )
                }
            }
        }

        // Simulated Android Toast
        controller.toastMessage?.let { msg ->
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(msg, color = TextPrimary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun SimulatorElement(
    el: JSONObject,
    accentColor: Color,
    stateMap: MutableMap<String, Any>,
    listMap: MutableMap<String, MutableList<String>>,
    resolveTemplate: (String) -> String,
    onAction: (JSONObject) -> Unit,
    onShowToast: (String) -> Unit
) {
    when (el.optString("type")) {
        "heroCard" -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianPanel),
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val badge = resolveTemplate(el.optString("badge"))
                    if (badge.isNotBlank()) {
                        Text(
                            text = badge,
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = resolveTemplate(el.optString("title")),
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitle = resolveTemplate(el.optString("subtitle"))
                    if (subtitle.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        "text" -> {
            val styleName = el.optString("style", "body")
            val (size, color, weight) = when (styleName) {
                "headline" -> Triple(18.sp, TextPrimary, FontWeight.Bold)
                "title" -> Triple(14.sp, TextPrimary, FontWeight.SemiBold)
                "muted" -> Triple(11.sp, TextMuted, FontWeight.Normal)
                else -> Triple(13.sp, TextSecondary, FontWeight.Normal)
            }
            Text(
                text = resolveTemplate(el.optString("text")),
                fontSize = size,
                color = color,
                fontWeight = weight
            )
        }

        "metricRow" -> {
            val metrics = el.optJSONArray("metrics") ?: JSONArray()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (m in 0 until metrics.length()) {
                    val metric = metrics.optJSONObject(m) ?: continue
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = resolveTemplate(metric.optString("label")),
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = resolveTemplate(metric.optString("value")),
                                color = accentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        "row" -> {
            val children = el.optJSONArray("children") ?: JSONArray()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until children.length()) {
                    val child = children.optJSONObject(c) ?: continue
                    Box(modifier = Modifier.weight(1f)) {
                        SimulatorElement(
                            el = child,
                            accentColor = accentColor,
                            stateMap = stateMap,
                            listMap = listMap,
                            resolveTemplate = resolveTemplate,
                            onAction = onAction,
                            onShowToast = onShowToast
                        )
                    }
                }
            }
        }

        "button" -> {
            Button(
                onClick = { onAction(el) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = ObsidianVoid
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resolveTemplate(el.optString("label", "Action")),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        "outlinedButton" -> {
            OutlinedButton(
                onClick = { onAction(el) },
                border = BorderStroke(1.dp, ObsidianBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resolveTemplate(el.optString("label", "Option")),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        "switchRow" -> {
            val key = el.optString("key")
            val checked = stateMap[key] as? Boolean ?: false
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .clickable { stateMap[key] = !checked }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = resolveTemplate(el.optString("label")),
                    color = TextPrimary,
                    fontSize = 12.sp
                )
                Switch(
                    checked = checked,
                    onCheckedChange = { stateMap[key] = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ObsidianVoid,
                        checkedTrackColor = accentColor
                    )
                )
            }
        }

        "slider" -> {
            val key = el.optString("key")
            val min = el.optDouble("min", 0.0).toFloat()
            val max = el.optDouble("max", 100.0).toFloat()
            val curr = (stateMap[key] as? Number)?.toFloat() ?: min
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(resolveTemplate(el.optString("label")), color = TextPrimary, fontSize = 12.sp)
                    Text("${curr.toInt()}", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = curr.coerceIn(min, max),
                    onValueChange = { stateMap[key] = it.toInt() },
                    valueRange = min..max,
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }
        }

        "textField" -> {
            val key = el.optString("key")
            val text = stateMap[key]?.toString() ?: ""
            OutlinedTextField(
                value = text,
                onValueChange = { stateMap[key] = it },
                placeholder = {
                    Text(el.optString("placeholder", "Enter value..."), fontSize = 12.sp, color = TextMuted)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        "list" -> {
            val listKey = el.optString("listKey")
            val title = el.optString("title")
            val items = listMap[listKey] ?: emptyList()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (title.isNotBlank()) {
                    Text(title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                if (items.isEmpty()) {
                    Text("No items yet", color = TextMuted, fontSize = 11.sp)
                } else {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val updated = items.toMutableList().apply { removeAt(index) }
                                    listMap[listKey] = updated
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove item",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        "calculatorDisplay" -> {
            val exprKey = el.optString("expressionKey", "expr")
            val resKey = el.optString("resultKey", "result")
            val expr = stateMap[exprKey]?.toString().orEmpty().ifBlank { "0" }
            val result = stateMap[resKey]?.toString().orEmpty().ifBlank { "0" }
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianPanel),
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expr,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = accentColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        "keypad" -> {
            val exprKey = el.optString("exprKey", "expr")
            val resKey = el.optString("resultKey", "result")
            val rows = el.optJSONArray("rows") ?: JSONArray()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows.length()) {
                    val rowArr = rows.optJSONArray(r) ?: continue
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until rowArr.length()) {
                            val token = rowArr.optString(c)
                            val isEquals = token == "="
                            val isClear = token == "C" || token == "AC"
                            val bg = when {
                                isEquals -> accentColor
                                isClear -> RoseError.copy(alpha = 0.2f)
                                else -> ObsidianElevated
                            }
                            val fg = when {
                                isEquals -> ObsidianVoid
                                isClear -> RoseError
                                else -> TextPrimary
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bg)
                                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val curr = stateMap[exprKey]?.toString().orEmpty()
                                        when (token) {
                                            "C", "AC" -> {
                                                stateMap[exprKey] = ""
                                                stateMap[resKey] = "0"
                                            }
                                            "DEL", "⌫" -> {
                                                stateMap[exprKey] = curr.dropLast(1)
                                            }
                                            "=" -> {
                                                val evaluated = evaluateMathExpression(curr)
                                                stateMap[resKey] = evaluated
                                            }
                                            "sin", "cos", "tan", "sqrt", "log", "ln" -> {
                                                stateMap[exprKey] = "$curr$token("
                                            }
                                            else -> {
                                                stateMap[exprKey] = curr + token
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = token,
                                    color = fg,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        "productCard" -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianPanel),
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resolveTemplate(el.optString("title")),
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = resolveTemplate(el.optString("subtitle")),
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = resolveTemplate(el.optString("price")),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Button(
                        onClick = {
                            onAction(el)
                            onShowToast("Updated cart")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = ObsidianVoid
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(el.optString("buttonLabel", "Add"), fontSize = 11.sp)
                    }
                }
            }
        }

        "divider" -> HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
    }
}

private fun evaluateMathExpression(expr: String): String {
    if (expr.isBlank()) return "0"
    return try {
        val cleaned = expr.replace("×", "*").replace("÷", "/").replace("π", Math.PI.toString())
        val parser = object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < cleaned.length) cleaned[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < cleaned.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    x = when {
                        eat('+'.code) -> x + parseTerm()
                        eat('-'.code) -> x - parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    x = when {
                        eat('*'.code) -> x * parseFactor()
                        eat('/'.code) -> x / parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    x = cleaned.substring(startPos, pos).toDouble()
                } else if (ch in 'a'.code..'z'.code) {
                    while (ch in 'a'.code..'z'.code) nextChar()
                    val func = cleaned.substring(startPos, pos)
                    x = if (func == "pi") Math.PI else {
                        val arg = parseFactor()
                        when (func) {
                            "sqrt" -> sqrt(arg)
                            "sin" -> sin(Math.toRadians(arg))
                            "cos" -> cos(Math.toRadians(arg))
                            "tan" -> tan(Math.toRadians(arg))
                            "log" -> log10(arg)
                            "ln" -> ln(arg)
                            else -> throw RuntimeException("Unknown func: $func")
                        }
                    }
                } else {
                    throw RuntimeException("Unexpected token")
                }
                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }
        }
        val value = parser.parse()
        if (value.isNaN() || value.isInfinite()) "Error"
        else if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    } catch (_: Exception) {
        "Error"
    }
}
