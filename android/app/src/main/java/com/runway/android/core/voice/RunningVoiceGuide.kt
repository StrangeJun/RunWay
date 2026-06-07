package com.runway.android.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
        selectKoreanFemaleVoice(textToSpeech.voices)?.let { textToSpeech.voice = it }
        ready = true
        while (true) {
            val message = pending.poll() ?: break
            speak(message)
        }
    }

    fun speak(message: String) {
        if (message.isBlank()) return
        if (!ready) {
            pending.offer(message)
            return
        }
        textToSpeech.speak(
            message,
            TextToSpeech.QUEUE_ADD,
            null,
            "pathfinder-${System.nanoTime()}",
        )
    }

    fun start() = speak("러닝을 시작합니다. 오늘도 힘차게 달려볼까요?")

    fun finish() = speak("러닝을 종료합니다. 수고하셨습니다.")

    fun kilometer(kilometers: Int, elapsedSeconds: Int) {
        speak(RunningVoiceText.kilometer(kilometers, elapsedSeconds))
    }

    fun offCourse() =
        speak("코스를 이탈했습니다. 안전하게 코스로 돌아와 주세요.")

    fun backOnCourse() =
        speak("코스로 복귀했습니다. 러닝을 계속합니다.")

    fun nearCourseFinish() =
        speak("코스의 90퍼센트를 완주했습니다. 조금만 더 힘내세요.")

    private fun selectKoreanFemaleVoice(voices: Set<Voice>?): Voice? {
        val korean = voices.orEmpty().filter { it.locale.language == Locale.KOREAN.language }
        return korean.firstOrNull {
            it.name.contains("female", ignoreCase = true) && !it.isNetworkConnectionRequired
        } ?: korean.firstOrNull {
            !it.name.contains("male", ignoreCase = true) && !it.isNetworkConnectionRequired
        } ?: korean.firstOrNull { !it.isNetworkConnectionRequired }
            ?: korean.firstOrNull()
    }
}

internal object RunningVoiceText {
    fun kilometer(kilometers: Int, elapsedSeconds: Int): String {
        val averagePace = if (kilometers > 0) elapsedSeconds / kilometers else 0
        return buildString {
            append("${kilometers}킬로미터 완료. ")
            append("시간 ${duration(elapsedSeconds)}. ")
            append("평균 페이스 ${pace(averagePace)}.")
        }
    }

    private fun duration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return buildList {
            if (hours > 0) add("${hours}시간")
            if (minutes > 0) add("${minutes}분")
            if (seconds > 0 || isEmpty()) add("${seconds}초")
        }.joinToString(" ")
    }

    private fun pace(secondsPerKilometer: Int): String {
        return "${secondsPerKilometer / 60}분 ${secondsPerKilometer % 60}초"
    }
}
