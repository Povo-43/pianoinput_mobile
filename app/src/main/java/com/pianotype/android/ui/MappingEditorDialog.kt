package com.pianotype.android.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pianotype.android.config.Config
import com.pianotype.android.config.Mapping

@Composable
fun MappingEditorDialog(
    initial: Mapping,
    context: Context,
    config: Config,
    onDismiss: () -> Unit,
    onSave: (Mapping) -> Unit,
) {
    var keysText by remember { mutableStateOf(initial.keys.joinToString(",")) }
    var valueText by remember { mutableStateOf(initial.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.keys.isEmpty() && initial.value.isEmpty()) "マッピング追加" else "マッピング編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keysText,
                    onValueChange = { keysText = it },
                    label = { Text("keys (例: 60,64,67)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("value (例: あ)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val keys = keysText.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                onSave(Mapping(keys, valueText))
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}