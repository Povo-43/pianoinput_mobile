package com.pianotype.android.ime

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import com.pianotype.android.R
import com.pianotype.android.config.Config
import com.pianotype.android.config.ConfigRepository
import com.pianotype.android.keyboard.OutputAction
import com.pianotype.android.keyboard.ValueParser
import com.pianotype.android.midi.MidiEvent
import com.pianotype.android.midi.MidiListener

class PianotypeImeService : InputMethodService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var midiListener: MidiListener? = null
    private var engine: MappingEngine? = null
    private var statusView: TextView? = null

    private val configListener: (Config) -> Unit = { cfg ->
        mainHandler.post { restartMidi(cfg) }
    }

    override fun onCreate() {
        super.onCreate()
        ConfigRepository.load(this)
        engine = MappingEngine(
            configProvider = { ConfigRepository.current },
            onToggle = { enabled ->
                mainHandler.post { updateStatus(enabled) }
            },
            onShiftArmed = {
                mainHandler.post { statusView?.text = "Pianotype: SHIFT armed" }
            },
            onAction = { value, shift ->
                mainHandler.post { handleAction(value, shift) }
            },
        )
        ConfigRepository.addListener(configListener)
    }

    override fun onCreateInputView(): View {
        val tv = TextView(this).apply {
            text = "Pianotype: OFF"
            textSize = 14f
            setPadding(24, 24, 24, 24)
        }
        statusView = tv
        engine?.let { updateStatus(it.isEnabled()) }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(tv)
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        engine?.reset()
        // restart MIDI in case config changed
        restartMidi(ConfigRepository.current)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        midiListener?.close()
        midiListener = null
    }

    override fun onDestroy() {
        ConfigRepository.removeListener(configListener)
        midiListener?.close()
        midiListener = null
        super.onDestroy()
    }

    private fun restartMidi(cfg: Config) {
        midiListener?.close()
        midiListener = MidiListener(this, cfg.midiDevice) { event ->
            val e = engine ?: return@MidiListener
            when (event) {
                is MidiEvent.NoteOn -> e.onNoteOn(event.note)
                is MidiEvent.NoteOff -> e.onNoteOff(event.note)
            }
        }.also { it.start() }
    }

    private fun updateStatus(enabled: Boolean) {
        statusView?.text = if (enabled) "Pianotype: ON" else "Pianotype: OFF"
    }

    private fun handleAction(value: String, shift: Boolean) {
        val ic = currentInputConnection ?: return
        val action = ValueParser.parse(value, shift) ?: return
        when (action) {
            is OutputAction.Text -> {
                ic.commitText(action.text, 1)
            }
            is OutputAction.Key -> {
                val meta = buildMetaState(action.ctrl, action.alt, action.shift)
                val down = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, action.keyCode, 0, meta)
                val up = KeyEvent(0, 0, KeyEvent.ACTION_UP, action.keyCode, 0, meta)
                ic.sendKeyEvent(down)
                ic.sendKeyEvent(up)
            }
        }
    }

    private fun buildMetaState(ctrl: Boolean, alt: Boolean, shift: Boolean): Int {
        var m = 0
        if (ctrl) m = m or KeyEvent.META_CTRL_ON
        if (alt) m = m or KeyEvent.META_ALT_ON
        if (shift) m = m or KeyEvent.META_SHIFT_ON
        return m
    }
}