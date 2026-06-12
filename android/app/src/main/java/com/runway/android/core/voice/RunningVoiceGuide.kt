package com.runway.android.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.runway.android.core.datastore.VoiceGuideDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class RunningVoiceGuide @Inject constructor(
    @ApplicationContext context: Context,
    private val voiceGuideDataStore: VoiceGuideDataStore,
    @Named("appScope") private val appScope: CoroutineScope,
) : TextToSpeech.OnInitListener {
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

    fun speak(message: String) {
        if (message.isBlank()) return
        appScope.launch {
            if (voiceGuideDataStore.enabledFlow.first()) {
                speakEnabled(message)
            }
        }
    }

    private fun speakEnabled(message: String) {
        if (!ready) {
            pending.offer(message)
            return
        }
        textToSpeech.speak(
            message,
            TextToSpeech.QUEUE_ADD,
            null,
            "runway-phone-${System.nanoTime()}",
        )
    }

    fun start() = speak("러닝을 시작합니다. 오늘도 힘차게 달려볼까요?")

    fun finish() = speak("러닝을 종료합니다. 수고하셨습니다.")

    fun kilometer(kilometers: Int, elapsedSeconds: Int) {
        speak(RunningVoiceText.kilometer(kilometers, elapsedSeconds))
    }

    fun autoPaused() = speak("움직임이 멈춰 자동으로 일시정지합니다.")

    fun autoResumed() = speak("움직임이 감지되어 러닝을 다시 시작합니다.")

    fun offCourse() = speak("코스를 이탈했습니다.")

    fun backOnCourse() = speak("코스로 복귀했습니다.")

    fun nearCourseFinish() = speak("거의 다 왔습니다.")

    private fun selectKoreanFemaleVoice(voices: Set<Voice>?): Voice? {
        val korean = voices.orEmpty().filter { it.locale.language == Locale.KOREAN.language }
        val offline = korean.filter { !it.isNetworkConnectionRequired }
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
    fun kilometer(kilometers: Int, elapsedSeconds: Int): String {
        val averagePace = if (kilometers > 0) elapsedSeconds / kilometers else 0
        return "${kilometers}킬로미터 완료. 시간 ${duration(elapsedSeconds)}. " +
            "평균 페이스 ${pace(averagePace)}."
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

    private fun pace(secondsPerKilometer: Int): String =
        "${secondsPerKilometer / 60}분 ${secondsPerKilometer % 60}초"
}
