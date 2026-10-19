package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(private val context: Context) {

    private val sampleRate = 22050
    private val audioScope = CoroutineScope(Dispatchers.Default)

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    // Fix 5: Using SoundPool instead of per-sound AudioTrack creation
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = ConcurrentHashMap<String, Int>()

    init {
        // Pre-render sound waves and load into SoundPool
        audioScope.launch {
            loadSound("slash", generateSlashSound())
            loadSound("clang", generateClangSound())
            loadSound("hit", generateHitSound())
            loadSound("kick", generateKickSound())
            loadSound("shadow", generateShadowSound())
            loadSound("gong", generateGongSound())
            loadSound("shuriken", generateShurikenSound())
            loadSound("victory", generateVictorySound())
        }
    }

    private fun loadSound(name: String, samples: ShortArray) {
        try {
            val file = File(context.cacheDir, "snd_$name.wav")
            writeWavFile(file, samples, sampleRate)
            val id = soundPool.load(file.absolutePath, 1)
            soundIds[name] = id
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    private fun play(name: String, volume: Float = 1.0f) {
        if (!isSoundEnabled) return
        val id = soundIds[name] ?: return
        soundPool.play(id, volume, volume, 1, 0, 1.0f)
    }

    fun playSlash() {
        play("slash", 0.9f)
    }

    fun playClang() {
        play("clang", 1.0f)
        vibrateLight()
    }

    fun playHit() {
        play("hit", 1.0f)
        vibrateMedium()
    }

    fun playKick() {
        play("kick", 1.0f)
        vibrateMedium()
    }

    fun playShadow() {
        play("shadow", 1.0f)
        vibrateHeavy()
    }

    fun playGong() {
        play("gong", 1.0f)
        vibrateLight()
    }

    fun playShuriken() {
        play("shuriken", 0.8f)
    }

    fun playVictory() {
        play("victory", 1.0f)
    }

    // Fix 5: Release SoundPool resources in onCleared
    fun release() {
        try {
            soundPool.release()
        } catch (_: Exception) {}
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val totalAudioLen = samples.size * 2
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * 2

        FileOutputStream(file).use { out ->
            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0 // PCM format
            header[22] = 1; header[23] = 0 // Mono
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0 // Block align
            header[34] = 16; header[35] = 0 // Bits per sample
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
            out.write(header)

            val byteBuffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                byteBuffer.putShort(s)
            }
            out.write(byteBuffer.array())
        }
    }

    // --- Procedural Sound Waveform Generators ---

    private fun generateSlashSound(): ShortArray {
        val durationMs = 160
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 800.0 - progress * 500.0
            val envelope = (1.0 - progress) * sin(progress * PI)
            val noise = (Random.nextDouble() - 0.5) * 0.4
            val sample = (sin(2 * PI * freq * t) * 0.6 + noise) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateClangSound(): ShortArray {
        val durationMs = 280
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-t * 22.0)
            val f1 = sin(2 * PI * 1840.0 * t)
            val f2 = sin(2 * PI * 2760.0 * t) * 0.7
            val f3 = sin(2 * PI * 3420.0 * t) * 0.4
            val sample = (f1 + f2 + f3) * decay
            buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHitSound(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 220.0 * (1.0 - progress * 0.6)
            val decay = exp(-t * 25.0)
            val noise = if (i < 200) (Random.nextDouble() - 0.5) * 0.8 else 0.0
            val sample = (sin(2 * PI * freq * t) * 0.8 + noise) * decay
            buffer[i] = (sample * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateKickSound(): ShortArray {
        val durationMs = 220
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val freq = 160.0 * (1.0 - progress * 0.7)
            val decay = exp(-t * 18.0)
            val sample = sin(2 * PI * freq * t) * decay
            buffer[i] = (sample * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateShadowSound(): ShortArray {
        val durationMs = 500
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val subBass = sin(2 * PI * (60.0 + progress * 80.0) * t) * 0.6
            val darkSweep = sin(2 * PI * (150.0 + progress * 600.0) * t) * 0.4
            val env = sin(progress * PI) * exp(-progress * 2.0)
            val sample = (subBass + darkSweep) * env
            buffer[i] = (sample * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateGongSound(): ShortArray {
        val durationMs = 1200
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val f1 = sin(2 * PI * 180.0 * t) * exp(-t * 2.5)
            val f2 = sin(2 * PI * 290.0 * t) * 0.6 * exp(-t * 3.2)
            val f3 = sin(2 * PI * 420.0 * t) * 0.3 * exp(-t * 4.0)
            val sample = (f1 + f2 + f3) * 0.8
            buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateShurikenSound(): ShortArray {
        val durationMs = 180
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val mod = sin(2 * PI * 45.0 * t)
            val freq = 1200.0 + mod * 400.0
            val decay = (1.0 - (i.toDouble() / numSamples))
            val sample = sin(2 * PI * freq * t) * decay * 0.5
            buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateVictorySound(): ShortArray {
        val durationMs = 600
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val third = numSamples / 3
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val freq = when {
                i < third -> 440.0
                i < 2 * third -> 554.37
                else -> 659.25
            }
            val subIndex = i % third
            val env = exp(-(subIndex.toDouble() / third) * 4.0)
            val sample = sin(2 * PI * freq * t) * env
            buffer[i] = (sample * Short.MAX_VALUE * 0.65).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    // --- Haptics ---

    fun vibrateLight() {
        if (!isHapticsEnabled) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(25)
            }
        }
    }

    fun vibrateMedium() {
        if (!isHapticsEnabled) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(55)
            }
        }
    }

    fun vibrateHeavy() {
        if (!isHapticsEnabled) return
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 70, 40, 90)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(120)
            }
        }
    }
}
