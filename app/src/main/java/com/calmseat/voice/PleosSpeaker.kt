package com.calmseat.voice

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.util.Log
import java.util.Locale

// Pleos TTS. ON_DEVICE면 네트워크 없이 됨. 초기화 끝나기 전에 온 문장은 마지막 거 하나만 기억
class PleosSpeaker(context: Context, private val fallback: Speaker) : Speaker {

    override val engineLabel: String
        get() = if (useFallback) fallback.engineLabel else "Pleos 음성"

    private var engine: TextToSpeech? = null
    private var ready = false
    private var useFallback = false
    private var pendingText: String? = null

    init {
        val candidate = runCatching { TextToSpeech(context.applicationContext, Mode.ON_DEVICE) }
            .onFailure { Log.w(TAG, "Pleos TTS 생성 실패 → 기기 음성 사용", it) }
            .getOrNull()
        if (candidate == null) {
            useFallback = true
        } else {
            engine = candidate
            candidate.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    runCatching {
                        candidate.setLocale(Locale.KOREA)
                        candidate.addEventListener(object : EventListener {
                            override fun onReady() = Unit
                            override fun onStart() = Unit
                            override fun onDone() = Unit
                            override fun onStop() = Unit
                            override fun onUpdatedRms(rms: Double) = Unit
                            override fun onError(errorType: ErrorType) {
                                Log.w(TAG, "Pleos TTS 오류: $errorType")
                            }
                        })
                    }
                    ready = true
                    pendingText?.let { speak(it) }
                    pendingText = null
                }

                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "Pleos TTS 초기화 실패($errorType) → 기기 음성 사용")
                    useFallback = true
                    pendingText?.let { fallback.speak(it) }
                    pendingText = null
                }
            })
        }
    }

    override fun speak(text: String) {
        when {
            useFallback -> fallback.speak(text)
            !ready -> pendingText = text
            else -> {
                val e = engine ?: return fallback.speak(text)
                runCatching {
                    e.stop()
                    e.speak(text)
                }.onFailure {
                    Log.w(TAG, "Pleos TTS 발화 실패 → 기기 음성", it)
                    fallback.speak(text)
                }
            }
        }
    }

    override fun shutdown() {
        runCatching { engine?.stop() }
        fallback.shutdown()
    }

    private companion object {
        const val TAG = "PleosSpeaker"
    }
}
