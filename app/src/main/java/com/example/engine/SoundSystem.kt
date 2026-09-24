package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundSystem(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val sampleRate = 22050

    fun playPulseRifle() {
        vibrate(30, 180)
        playSoundAsync {
            generateSciFiPulse(durationMs = 90, startFreq = 880f, endFreq = 220f)
        }
    }

    fun playShotgun() {
        vibrate(70, 255)
        playSoundAsync {
            generateExplosion(durationMs = 180, decay = 0.94f)
        }
    }

    fun playRailgunCharge() {
        vibrate(100, 80)
        playSoundAsync {
            generateSciFiPulse(durationMs = 280, startFreq = 200f, endFreq = 1200f)
        }
    }

    fun playRailgunFire() {
        vibrate(120, 255)
        playSoundAsync {
            generateSupersonicCrack(durationMs = 250)
        }
    }

    fun playMissileLaunch() {
        vibrate(50, 160)
        playSoundAsync {
            generateMissileWhoosh(durationMs = 200)
        }
    }

    fun playHitMarker() {
        playSoundAsync {
            generateTone(durationMs = 40, freq = 1400f, volume = 0.6f)
        }
    }

    fun playHeadshotCrit() {
        vibrate(40, 220)
        playSoundAsync {
            val buf1 = generateTone(durationMs = 60, freq = 1800f, volume = 0.8f)
            val buf2 = generateTone(durationMs = 100, freq = 2400f, volume = 0.9f)
            buf1 + buf2
        }
    }

    fun playPlayerHit() {
        vibrate(80, 230)
        playSoundAsync {
            generateDullThud(durationMs = 120)
        }
    }

    fun playBulletTime() {
        vibrate(60, 120)
        playSoundAsync {
            generateSciFiPulse(durationMs = 350, startFreq = 600f, endFreq = 120f)
        }
    }

    fun playShieldBreak() {
        vibrate(140, 255)
        playSoundAsync {
            generateShieldBreak(durationMs = 260)
        }
    }

    fun playEnemyDeath() {
        playSoundAsync {
            generateExplosion(durationMs = 220, decay = 0.92f)
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int = 180) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    private fun playSoundAsync(generator: () -> ShortArray) {
        scope.launch {
            try {
                val samples = generator()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(samples.size * 2, minBufferSize)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                // Release after played
                kotlinx.coroutines.delay((samples.size.toFloat() / sampleRate * 1000).toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    private fun generateTone(durationMs: Int, freq: Float, volume: Float = 0.7f): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val envelope = 1f - (i.toFloat() / numSamples)
            val sample = sin(2.0 * Math.PI * freq * t) * envelope * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateSciFiPulse(durationMs: Int, startFreq: Float, endFreq: Float): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val currentFreq = startFreq + (endFreq - startFreq) * progress
            phase += 2.0 * Math.PI * currentFreq / sampleRate
            val envelope = (1f - progress) * (1f - progress)
            val sample = sin(phase) * envelope * 0.75f
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateExplosion(durationMs: Int, decay: Float = 0.95f): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var amp = 1.0f
        var last = 0.0f
        for (i in 0 until numSamples) {
            val whiteNoise = (Math.random() * 2.0 - 1.0).toFloat()
            // Low-pass filter for rumble
            last = last * 0.7f + whiteNoise * 0.3f
            val sample = last * amp * 0.85f
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            amp *= decay
        }
        return buffer
    }

    private fun generateSupersonicCrack(durationMs: Int): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val noise = (Math.random() * 2.0 - 1.0).toFloat()
            val sine = sin(2.0 * Math.PI * 1800.0 * (i.toFloat() / sampleRate)).toFloat()
            val mix = (noise * 0.6f + sine * 0.4f) * (1f - progress)
            buffer[i] = (mix * Short.MAX_VALUE * 0.9f).toInt().toShort()
        }
        return buffer
    }

    private fun generateMissileWhoosh(durationMs: Int): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val freq = 120f + 600f * progress
            phase += 2.0 * Math.PI * freq / sampleRate
            val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.3f
            val sample = (sin(phase).toFloat() * 0.7f + noise) * (1f - progress)
            buffer[i] = (sample * Short.MAX_VALUE * 0.7f).toInt().toShort()
        }
        return buffer
    }

    private fun generateDullThud(durationMs: Int): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val sample = sin(2.0 * Math.PI * 65.0 * (i.toFloat() / sampleRate)) * (1f - progress)
            buffer[i] = (sample * Short.MAX_VALUE * 0.85f).toInt().toShort()
        }
        return buffer
    }

    private fun generateShieldBreak(durationMs: Int): ShortArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            val chirp = sin(2.0 * Math.PI * (1600f - 1100f * progress) * (i.toFloat() / sampleRate))
            val noise = (Math.random() * 2.0 - 1.0).toFloat() * 0.2f
            val sample = (chirp.toFloat() + noise) * (1f - progress)
            buffer[i] = (sample * Short.MAX_VALUE * 0.8f).toInt().toShort()
        }
        return buffer
    }
}
