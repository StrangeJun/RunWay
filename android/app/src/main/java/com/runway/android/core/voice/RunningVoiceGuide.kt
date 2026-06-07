package com.runway.android.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunningVoiceGuide @Inject constructor(
    @ApplicationContext context: Context,
) : TextToSpeech.OnInitListener {
    private val pending = ConcurrentLinkedQueue<String>()
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        textToSpeech.language = Locale.KOREA
        textToSpeech.setSpeechRate(1.08f)
        textToSpeech.setPitch(1.08f)

        // 오프라인 한국어 여성 음성 우선, 없으면 오프라인 한국어 음성으로 대체
        val koreanVoices = textToSpeech.voices
            .orEmpty()
            .filter { it.locale.language == Locale.KOREAN.language && !it.isNetworkConnectionRequired }

        val preferred = koreanVoices.firstOrNull { it.name.contains("female", ignoreCase = true) }
            ?: koreanVoices.firstOrNull()

        preferred?.let { textToSpeech.voice = it }

        ready = true
        while (true) { speak(pending.poll() ?: break) }
    }

    fun speak(message: String) {
        if (message.isBlank()) return
        if (!ready) { pending.offer(message); return }
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "rw-${System.nanoTime()}")
    }

    fun start() = speak("러닝을 시작합니다.")

    fun finish() = speak("러닝을 종료합니다. 수고하셨습니다.")

    fun kilometer(kilometers: Int, elapsedSeconds: Int) {
        val pace = if (kilometers > 0) elapsedSeconds / kilometers else 0
        speak("${kilometers}킬로미터 완료. 페이스 ${pace / 60}분 ${pace % 60}초.")
    }

    fun autoPaused() = speak("자동으로 일시정지합니다.")

    fun autoResumed() = speak("다시 시작합니다.")

    fun offCourse() = speak("코스를 이탈했습니다.")

    fun backOnCourse() = speak("코스로 복귀했습니다.")

    fun nearCourseFinish() = speak("거의 다 왔습니다.")
}
