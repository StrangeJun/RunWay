package com.runway.wear.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class RunningVoiceGuide(context: Context) : TextToSpeech.OnInitListener {
    private val pending = ConcurrentLinkedQueue<String>()
    private val textToSpeech = TextToSpeech(
        context.applicationContext,
        this,
        GOOGLE_TTS_PACKAGE,
    )
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

    fun start() = speak("러닝을 시작합니다. 오늘도 힘차게 달려볼까요?")

    fun finish() = speak("러닝을 종료합니다. 수고하셨습니다.")

    fun cancel() = speak("러닝을 취소합니다.")

    fun autoPaused() = speak("움직임이 멈춰 자동으로 일시정지합니다.")

    fun autoResumed() = speak("움직임이 감지되어 러닝을 다시 시작합니다.")

    fun gpsWeak() = speak("GPS 신호가 약합니다. 시야가 트인 곳으로 이동해 주세요.")

    fun gpsRecovered() = speak("GPS 신호가 안정되었습니다.")

    fun offCourse() = speak("코스를 이탈했습니다.")

    fun backOnCourse() = speak("코스로 복귀했습니다.")

    fun nearCourseFinish() = speak("거의 다 왔습니다.")

    fun goalCompleted(continueRecording: Boolean) = speak(
        if (continueRecording) {
            "목표를 달성했습니다. 기록을 계속합니다."
        } else {
            "목표를 달성했습니다. 러닝을 일시정지합니다."
        },
    )

    fun intervalChanged(isWork: Boolean, step: Int) = speak(
        if (isWork) "${step}세트 운동을 시작합니다." else "회복 구간을 시작합니다.",
    )

    fun kilometer(
        kilometers: Int,
        elapsedSeconds: Long,
        heartRateBpm: Int?,
    ) = speak(RunningVoiceText.kilometer(kilometers, elapsedSeconds, heartRateBpm))

    fun shutdown() {
        pending.clear()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    fun stopGuidance() {
        pending.clear()
        textToSpeech.stop()
    }

    private fun speak(message: String) {
        if (!ready) {
            pending.offer(message)
            return
        }
        textToSpeech.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "runway-watch-${System.nanoTime()}",
        )
    }

    private fun selectKoreanFemaleVoice(voices: Set<Voice>?): Voice? {
        val korean = voices.orEmpty().filter { it.locale.language == Locale.KOREAN.language }
        val offline = korean.filter { !it.isNetworkConnectionRequired }
        // Google TTS: "ko-kr-x-kof-local", Samsung TTS: "ko-KR-SMTf00" 등 f 패턴 우선
        return offline.firstOrNull { isFemale(it.name) }
            ?: offline.firstOrNull { !isMale(it.name) }
            ?: offline.firstOrNull()
            ?: korean.firstOrNull { isFemale(it.name) }
            ?: korean.firstOrNull()
    }

    private fun isFemale(name: String) =
        name.contains("female", ignoreCase = true) ||
            name.contains("SMTf", ignoreCase = false) ||
            FEMALE_PATTERN.containsMatchIn(name)

    private fun isMale(name: String) =
        name.contains("male", ignoreCase = true) ||
            name.contains("SMTm", ignoreCase = false) ||
            MALE_PATTERN.containsMatchIn(name)

    private companion object {
        const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        val FEMALE_PATTERN = Regex("[^a-zA-Z]f\\d")
        val MALE_PATTERN = Regex("[^a-zA-Z]m\\d")
    }
}

internal object RunningVoiceText {
    fun kilometer(
        kilometers: Int,
        elapsedSeconds: Long,
        heartRateBpm: Int?,
    ): String {
        val averagePace = if (kilometers > 0) elapsedSeconds / kilometers else 0
        return buildString {
            append("${kilometers}킬로미터 완료. ")
            append("시간 ${duration(elapsedSeconds)}. ")
            append("평균 페이스 ${pace(averagePace)}. ")
            if (heartRateBpm != null && heartRateBpm > 0) {
                append("현재 심박수 ${heartRateBpm}.")
            } else {
                append("심박수는 측정 중입니다.")
            }
        }
    }

    private fun duration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return buildList {
            if (hours > 0) add("${hours}시간")
            if (minutes > 0) add("${minutes}분")
            if (seconds > 0 || isEmpty()) add("${seconds}초")
        }.joinToString(" ")
    }

    private fun pace(secondsPerKilometer: Long): String {
        return "${secondsPerKilometer / 60}분 ${secondsPerKilometer % 60}초"
    }
}
