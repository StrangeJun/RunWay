package com.runway.android.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.runway.android.core.datastore.ThemeDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

data class VoiceOption(
    val name: String,
    val displayName: String,
    val requiresNetwork: Boolean,
)

@Singleton
class RunningVoiceGuide @Inject constructor(
    @ApplicationContext context: Context,
    private val themeDataStore: ThemeDataStore,
    @Named("appScope") private val appScope: CoroutineScope,
) : TextToSpeech.OnInitListener {
    private val pending = ConcurrentLinkedQueue<String>()
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    @Volatile private var ready = false

    /** 초기화 후 사용 가능한 한국어 음성 목록 */
    var availableVoices: List<VoiceOption> = emptyList()
        private set

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        textToSpeech.language = Locale.KOREA
        textToSpeech.setSpeechRate(1.0f)
        textToSpeech.setPitch(1.0f)

        val koreanVoices = textToSpeech.voices
            .orEmpty()
            .filter { it.locale.language == Locale.KOREAN.language }
            .sortedWith(compareBy({ it.isNetworkConnectionRequired }, { it.quality * -1 }))

        availableVoices = koreanVoices.mapIndexed { i, v ->
            VoiceOption(
                name = v.name,
                displayName = "음성 ${i + 1}${if (v.isNetworkConnectionRequired) " (온라인)" else ""}",
                requiresNetwork = v.isNetworkConnectionRequired,
            )
        }

        // 저장된 선택이 있으면 복원, 없으면 오프라인 첫 번째 음성
        appScope.launch {
            val saved = themeDataStore.voiceNameFlow.firstOrNull()
            val target = saved?.let { name -> koreanVoices.firstOrNull { it.name == name } }
                ?: koreanVoices.firstOrNull { !it.isNetworkConnectionRequired }
            target?.let { textToSpeech.voice = it }
            ready = true
            while (true) { speak(pending.poll() ?: break) }
        }
    }

    /** 설정 화면에서 호출 — 선택한 음성을 즉시 적용 */
    fun applyVoice(voiceName: String) {
        if (!ready) return
        textToSpeech.voices
            ?.firstOrNull { it.name == voiceName }
            ?.let { textToSpeech.voice = it }
    }

    /** 미리 듣기용 — 짧은 샘플 재생 */
    fun preview() = speak("러닝을 시작합니다.")

    fun speak(message: String) {
        if (message.isBlank()) return
        if (!ready) { pending.offer(message); return }
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "rw-${System.nanoTime()}")
    }

    // ── 간결해진 대사 ──────────────────────────────────────────────

    fun start() = speak("러닝을 시작합니다.")

    fun finish() = speak("수고하셨습니다.")

    fun kilometer(kilometers: Int, elapsedSeconds: Int) {
        val pace = if (kilometers > 0) elapsedSeconds / kilometers else 0
        speak("${kilometers}킬로미터. 페이스 ${pace / 60}분 ${pace % 60}초.")
    }

    fun autoPaused() = speak("일시정지.")

    fun autoResumed() = speak("재개합니다.")

    fun offCourse() = speak("코스 이탈.")

    fun backOnCourse() = speak("코스 복귀.")

    fun nearCourseFinish() = speak("거의 다 왔습니다.")
}
