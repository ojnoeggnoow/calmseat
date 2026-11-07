package com.calmseat.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

// 한 문장 말하기. 새 문장 오면 하던 거 끊음
interface Speaker {
    val engineLabel: String
    fun speak(text: String)
    fun shutdown() {}
}

// Pleos TTS 안 되는 데서 쓰는 기본 TTS
class AndroidSpeaker(context: Context) : Speaker {

    override val engineLabel: String = "기기 음성"

    private var ready = false
    private var pendingText: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { tts.language = Locale.KOREAN }
            pendingText?.let { speakNow(it) }
            pendingText = null
        } else {
            Log.w(TAG, "안드로이드 TTS 초기화 실패")
        }
    }

    override fun speak(text: String) {
        if (ready) speakNow(text) else pendingText = text
    }

    private fun speakNow(text: String) {
        runCatching { tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "calmseat") }
    }

    override fun shutdown() {
        runCatching { tts.shutdown() }
    }

    private companion object {
        const val TAG = "AndroidSpeaker"
    }
}
