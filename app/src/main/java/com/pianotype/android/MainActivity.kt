package com.pianotype.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pianotype.android.config.ConfigRepository
import com.pianotype.android.ui.PianotypeTheme
import com.pianotype.android.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigRepository.load(this)
        setContent {
            PianotypeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    MainScreen(
                        context = this,
                        onOpenImeSettings = { openImeSettings() },
                    )
                }
            }
        }
    }

    private fun openImeSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @Suppress("unused")
    private fun showImePicker(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.showInputMethodPicker()
    }
}