package com.pianotype.android.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pianotype.android.config.Config
import com.pianotype.android.config.ConfigRepository
import com.pianotype.android.config.Mapping
import com.pianotype.android.config.TomlConfig
import com.pianotype.android.keyboard.ValueParser
import com.pianotype.android.midi.MidiDeviceScanner
import com.pianotype.android.midi.MidiEvent
import com.pianotype.android.midi.MidiListener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(context: Context, onOpenImeSettings: () -> Unit) {
    var config by remember { mutableStateOf(ConfigRepository.current) }
    var deviceList by remember { mutableStateOf(MidiDeviceScanner.list(context)) }
    var showTomlDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    // config listener
    DisposableEffect(Unit) {
        val l: (Config) -> Unit = { config = it }
        ConfigRepository.addListener(l)
        onDispose { ConfigRepository.removeListener(l) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pianotype 設定") },
                actions = {
                    TextButton(onClick = { onOpenImeSettings() }) {
                        Text("IME設定")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- MIDI device ----
            Text("MIDI 入力デバイス", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                var expanded by remember { mutableStateOf(false) }
                val label = config.midiDevice ?: "(最初に見つかったデバイス)"
                OutlinedButton(onClick = {
                    deviceList = MidiDeviceScanner.list(context)
                    expanded = true
                }) { Text(label) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("(auto / 先頭)") },
                        onClick = {
                            config = config.copy(midiDevice = null)
                            ConfigRepository.update(context, config)
                            expanded = false
                        }
                    )
                    deviceList.forEachIndexed { idx, item ->
                        DropdownMenuItem(
                            text = { Text("$idx: ${item.name}") },
                            onClick = {
                                config = config.copy(midiDevice = item.name)
                                ConfigRepository.update(context, config)
                                expanded = false
                            }
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { deviceList = MidiDeviceScanner.list(context) }) {
                    Text("更新")
                }
            }

            // ---- hand split ----
            Text("左手/右手の境界 (MIDI note, C4=60)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = config.handSplit.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { n ->
                        if (n in 0..127) {
                            config = config.copy(handSplit = n)
                            ConfigRepository.update(context, config)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // ---- require both hands ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = config.requireBothHands,
                    onCheckedChange = {
                        config = config.copy(requireBothHands = it)
                        ConfigRepository.update(context, config)
                    }
                )
                Text("両手で1つ以上の鍵を押すことを必須にする")
            }

            Divider()

            // ---- mappings ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("マッピング (${config.mapping.size})", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("追加")
                    }
                }
            }

            if (config.mapping.isEmpty()) {
                Text("(未登録)", style = MaterialTheme.typography.bodyMedium)
            } else {
                config.mapping.forEachIndexed { idx, m ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("keys: ${m.keys.joinToString(", ")}")
                                Text("value: ${m.value}")
                            }
                            IconButton(onClick = { editingIndex = idx }) {
                                Icon(Icons.Default.Edit, contentDescription = "edit")
                            }
                            IconButton(onClick = {
                                val newList = config.mapping.toMutableList().also { it.removeAt(idx) }
                                config = config.copy(mapping = newList)
                                ConfigRepository.update(context, config)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "delete")
                            }
                        }
                    }
                }
            }

            Divider()

            // ---- TOML paste ----
            Button(
                onClick = { showTomlDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TOML を貼り付けて読み込む")
            }
            OutlinedButton(
                onClick = {
                    // 現在の設定をクリップボードに出す代わりにダイアログへ
                    showTomlDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TOML を表示 / 編集")
            }

            // ---- status ----
            statusMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTomlDialog) {
        TomlEditorDialog(
            initial = TomlConfig.serialize(config),
            onDismiss = { showTomlDialog = false },
            onApply = { text ->
                try {
                    ConfigRepository.loadFromToml(context, text)
                    config = ConfigRepository.current
                    statusMsg = "TOML を読み込みました (${config.mapping.size} mappings)"
                    showTomlDialog = false
                } catch (e: Exception) {
                    statusMsg = "エラー: ${e.message}"
                }
            },
        )
    }

    if (showAddDialog || editingIndex != null) {
        MappingEditorDialog(
            initial = editingIndex?.let { config.mapping[it] } ?: Mapping(emptyList(), ""),
            context = context,
            config = config,
            onDismiss = { showAddDialog = false; editingIndex = null },
            onSave = { newMapping ->
                val list = config.mapping.toMutableList()
                val idx = editingIndex
                if (idx == null) list.add(newMapping) else list[idx] = newMapping
                config = config.copy(mapping = list)
                ConfigRepository.update(context, config)
                showAddDialog = false
                editingIndex = null
            },
        )
    }
}

@Composable
private fun TomlEditorDialog(
    initial: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TOML") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )
        },
        confirmButton = { TextButton(onClick = { onApply(text) }) { Text("読み込む") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

@Composable
private fun MappingEditorDialog(
    initial: Mapping,
    context: Context,
    config: Config,
    onDismiss: () -> Unit,
    onSave: (Mapping) -> Unit,
) {
    var keysText by remember { mutableStateOf(initial.keys.joinToString(" ")) }
    var value by remember { mutableStateOf(initial.value) }
    var capturing by remember { mutableStateOf(false) }
    var capturedNotes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var listener by remember { mutableStateOf<MidiListener?>(null) }

    // capture mode
    LaunchedEffect(capturing) {
        if (capturing) {
            capturedNotes = emptyList()
            val l = MidiListener(context, config.midiDevice) { ev ->
                when (ev) {
                    is MidiEvent.NoteOn -> {
                        capturedNotes = (capturedNotes + ev.note).distinct().sorted()
                    }
                    is MidiEvent.NoteOff -> {}
                }
            }.also { it.start() }
            listener = l
        } else {
            listener?.close()
            listener = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            listener?.close()
            listener = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.keys.isEmpty()) "マッピング追加" else "マッピング編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("keys (MIDI note 番号、スペース区切り)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = keysText,
                    onValueChange = { keysText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { capturing = !capturing },
                        colors = if (capturing) ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ) else ButtonDefaults.buttonColors()
                    ) {
                        Text(if (capturing) "停止" else "MIDI でキャプチャ")
                    }
                    if (capturing) {
                        Spacer(Modifier.width(8.dp))
                        Text("録音中...")
                    }
                }
                if (capturedNotes.isNotEmpty()) {
                    Text("取得中: ${capturedNotes.joinToString(", ")}")
                    TextButton(onClick = {
                        keysText = capturedNotes.joinToString(" ")
                    }) { Text("この和音を採用") }
                }
                Text("value (文字 / キー名 / TOGGLE / SHIFT / CTRL+F3 など)", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (value.isNotEmpty() && !ValueParser.isValid(value)) {
                    Text("無効な値です", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotEmpty() && ValueParser.isValid(value),
                onClick = {
                    val keys = keysText.split(" ", ",")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.toIntOrNull() }
                        .sorted()
                    onSave(Mapping(keys, value))
                }
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}