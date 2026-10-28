package com.lifereset.core.designsystem.sound

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** 연출에 붙는 효과음 종류. */
enum class Sfx(val resName: String) {
    QUEST_CLEAR("sfx_quest_clear"),
    PERFECT_DAY("sfx_perfect_day"),
    LEVEL_UP("sfx_level_up"),
    RANK_UP("sfx_rank_up"),
    BOSS("sfx_boss"),
    NOTICE("sfx_notice"),
}

/**
 * 효과음 재생기.
 *
 * 소리 바꾸는 법: app/src/main/res/raw/ 에 [Sfx.resName] 과 같은 이름의 파일
 * (예: sfx_level_up.ogg) 을 넣으면 그 파일이 재생된다. 파일이 없으면 아래에서 합성한 기본음이 난다.
 *
 * 무음/진동 모드에서는 울리지 않는다. 설정에서 끌 수도 있다 (enabled).
 */
class FxSounds(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
    private val loaded = mutableMapOf<Sfx, Int>()

    @Volatile
    var enabled: Boolean = true

    init {
        Sfx.entries.forEach { sfx ->
            @SuppressLint("DiscouragedApi")
            val id = app.resources.getIdentifier(sfx.resName, "raw", app.packageName)
            if (id != 0) loaded[sfx] = pool.load(app, id, 1)
        }
    }

    fun play(sfx: Sfx) {
        if (!enabled || audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val soundId = loaded[sfx]
        if (soundId != null) {
            pool.play(soundId, 0.8f, 0.8f, 1, 0, 1f)
        } else {
            scope.launch { runCatching { playTones(Synth.notesFor(sfx)) } }
        }
    }

    fun release() {
        pool.release()
    }

    private fun playTones(notes: List<Synth.Note>) {
        val pcm = Synth.render(notes)
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(Synth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep((pcm.size * 1000L / Synth.SAMPLE_RATE) + 50)
        track.release()
    }
}

/** 파일 없이 쓰는 기본 효과음: 짧은 사인파 '띵' 소리를 이어 붙인다. */
internal object Synth {
    const val SAMPLE_RATE = 22050

    data class Note(val freq: Double, val millis: Int, val volume: Double = 0.35)

    fun notesFor(sfx: Sfx): List<Note> = when (sfx) {
        Sfx.QUEST_CLEAR -> listOf(Note(1046.5, 70), Note(1568.0, 120))                       // C6 G6
        Sfx.PERFECT_DAY -> listOf(Note(784.0, 90), Note(1046.5, 90), Note(1318.5, 90), Note(1568.0, 220))
        Sfx.LEVEL_UP -> listOf(Note(523.3, 90), Note(659.3, 90), Note(784.0, 90), Note(1046.5, 300))
        Sfx.RANK_UP -> listOf(Note(392.0, 120), Note(523.3, 120), Note(659.3, 120), Note(784.0, 120), Note(1046.5, 400))
        Sfx.BOSS -> listOf(Note(196.0, 140, 0.45), Note(261.6, 140, 0.45), Note(392.0, 300, 0.45))
        Sfx.NOTICE -> listOf(Note(880.0, 80), Note(1174.7, 160))
    }

    fun render(notes: List<Note>): ShortArray {
        val total = notes.sumOf { it.millis * SAMPLE_RATE / 1000 }
        val out = ShortArray(total)
        var offset = 0
        notes.forEach { note ->
            val length = note.millis * SAMPLE_RATE / 1000
            for (i in 0 until length) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = (i / (SAMPLE_RATE * 0.005)).coerceAtMost(1.0)
                val decay = exp(-t * 6.0)
                val wave = sin(2 * PI * note.freq * t) + 0.25 * sin(4 * PI * note.freq * t)
                out[offset + i] = (wave * attack * decay * note.volume * Short.MAX_VALUE / 1.25).toInt().toShort()
            }
            offset += length
        }
        return out
    }
}
