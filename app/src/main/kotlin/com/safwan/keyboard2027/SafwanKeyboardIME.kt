package com.safwan.keyboard2027

import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout

class SafwanKeyboardIME : InputMethodService() {

    private var webView: WebView? = null

    override fun onCreateInputView(): View {
        val container = FrameLayout(this)
        container.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.keyboard_height)
        )

        val wv = WebView(this)
        webView = wv

        val settings: WebSettings = wv.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        wv.addJavascriptInterface(KeyboardBridge(), "AndroidBridge")
        wv.setBackgroundColor(0x00000000)
        wv.loadUrl("file:///android_asset/keyboard/index.html")

        container.addView(
            wv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        return container
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        webView?.evaluateJavascript("window.SafwanKeyboard && window.SafwanKeyboard.setImeAction($action)", null)
    }

    inner class KeyboardBridge {

        @JavascriptInterface
        fun commitText(text: String) {
            currentInputConnection?.commitText(text, 1)
        }

        @JavascriptInterface
        fun deleteBackward() {
            val ic: InputConnection? = currentInputConnection
            val selected = ic?.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                ic?.deleteSurroundingText(1, 0)
            }
        }

        @JavascriptInterface
        fun sendEnterOrAction() {
            val ic = currentInputConnection ?: return
            val info = currentInputEditorInfo
            val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
            if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action)
            } else {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }

        @JavascriptInterface
        fun moveCursor(offset: Int) {
            val ic = currentInputConnection ?: return
            val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            val pos = (extracted?.selectionStart ?: 0) + offset
            ic.setSelection(pos, pos)
        }

        @JavascriptInterface
        fun switchToNextKeyboard() {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToNextInputMethod(false)
            } else {
                @Suppress("DEPRECATION")
                imm.switchToNextInputMethod(window.window?.attributes?.token, false)
            }
        }

        @JavascriptInterface
        fun hapticFeedback() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
            }
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
