package com.example.service

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AlarmToneType
import com.example.data.model.AssistantSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class AudioAlarmController(private val context: Context) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var mediaPlayer: MediaPlayer? = null
    private var audioTrack: AudioTrack? = null
    private var isAlarmActive = false

    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging.asStateFlow()

    private var originalAlarmVol = -1
    private var originalMusicVol = -1

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var alarmLoopJob: Job? = null
    private var flashlightJob: Job? = null

    private var cameraManager: CameraManager? = null
    private var torchCameraId: String? = null

    init {
        textToSpeech = TextToSpeech(context, this)
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            torchCameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager?.getCameraCharacteristics(id)
                chars?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.w("AudioAlarmController", "Flashlight init error", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.setSpeechRate(1.0f)
            textToSpeech?.setPitch(1.0f)
        }
    }

    fun startAlarm(settings: AssistantSettings) {
        if (isAlarmActive) return
        isAlarmActive = true
        _isAlarmRinging.value = true

        // 1. Boost Volume if enabled
        if (settings.maxVolumeOverride) {
            runCatching {
                originalAlarmVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
                originalMusicVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
            }
        }

        // 2. Start Flashlight Strobe
        if (settings.flashlightStrobe) {
            startFlashlightStrobe()
        }

        // 3. Start Vibration
        if (settings.vibrationEnabled) {
            startVibration()
        }

        // 4. Start Tone & Voice Reply Loop
        alarmLoopJob = coroutineScope.launch {
            while (isActive && isAlarmActive) {
                // Spoken reply first or interleaved
                if (settings.voiceReplyEnabled) {
                    val replyText = if (settings.hindiEnabled) {
                        settings.voiceReplyText
                    } else {
                        settings.voiceReplyEnglish
                    }
                    speakReply(replyText, settings)
                    // Wait for speech to complete or short delay
                    delay(3000)
                }

                if (!isAlarmActive) break

                // Play alarm tone
                playToneForDuration(settings.alarmTone, 4000)
                delay(500)
            }
        }
    }

    fun stopAlarm() {
        if (!isAlarmActive) return
        isAlarmActive = false
        _isAlarmRinging.value = false

        alarmLoopJob?.cancel()
        alarmLoopJob = null

        flashlightJob?.cancel()
        flashlightJob = null
        turnOffTorch()

        stopVibration()
        stopAudioPlayback()

        // Restore Volume
        if (originalAlarmVol >= 0) {
            runCatching {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalAlarmVol, 0)
                originalAlarmVol = -1
            }
        }
        if (originalMusicVol >= 0) {
            runCatching {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVol, 0)
                originalMusicVol = -1
            }
        }
    }

    fun speakPreview(text: String, pitch: Float = 1.0f, speed: Float = 1.0f, isHindi: Boolean = true) {
        if (!isTtsReady || textToSpeech == null) return
        val locale = if (isHindi) Locale("hi", "IN") else Locale.US
        val res = textToSpeech?.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech?.setLanguage(Locale.US)
        }
        textToSpeech?.setPitch(pitch)
        textToSpeech?.setSpeechRate(speed)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PREVIEW_${System.currentTimeMillis()}")
    }

    private fun speakReply(text: String, settings: AssistantSettings) {
        if (!isTtsReady || textToSpeech == null) return
        val locale = if (settings.hindiEnabled) Locale("hi", "IN") else Locale.US
        val res = textToSpeech?.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            textToSpeech?.setLanguage(Locale.US)
        }
        textToSpeech?.setPitch(settings.ttsPitch)
        textToSpeech?.setSpeechRate(settings.ttsSpeed)

        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "REPLY_${System.currentTimeMillis()}")
    }

    private suspend fun playToneForDuration(toneType: AlarmToneType, durationMs: Long) {
        when (toneType) {
            AlarmToneType.SIREN -> playSynthesizedSiren(durationMs)
            AlarmToneType.CHIME -> playSynthesizedChime(durationMs)
            AlarmToneType.PULSE_BEEP -> playSynthesizedBeeps(durationMs)
            AlarmToneType.DEFAULT_ALARM -> playSystemUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), durationMs)
            AlarmToneType.SYSTEM_RINGTONE -> playSystemUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), durationMs)
        }
    }

    private suspend fun playSystemUri(uri: Uri?, durationMs: Long) {
        try {
            stopAudioPlayback()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                isLooping = true
                prepare()
                start()
            }
            delay(durationMs)
            stopAudioPlayback()
        } catch (e: Exception) {
            Log.w("AudioAlarmController", "Failed system ringtone, fallback to siren", e)
            playSynthesizedSiren(durationMs)
        }
    }

    private suspend fun playSynthesizedSiren(durationMs: Long) {
        stopAudioPlayback()
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            val chunkSamples = 1024
            val buffer = ShortArray(chunkSamples)
            var currentPhase = 0.0
            val startTime = System.currentTimeMillis()

            while (isAlarmActive && (System.currentTimeMillis() - startTime < durationMs)) {
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
                // Frequency sweeps between 650Hz and 1300Hz every 0.6 seconds
                val sweepFreq = 950.0 + 350.0 * sin(2.0 * Math.PI * elapsedSec * 1.6)

                for (i in 0 until chunkSamples) {
                    val sample = (sin(currentPhase) * 32000.0).toInt().toShort()
                    buffer[i] = sample
                    currentPhase += 2.0 * Math.PI * sweepFreq / sampleRate
                    if (currentPhase > 2.0 * Math.PI) {
                        currentPhase -= 2.0 * Math.PI
                    }
                }
                audioTrack?.write(buffer, 0, chunkSamples)
            }
        } catch (e: Exception) {
            Log.e("AudioAlarmController", "Siren playback error", e)
        } finally {
            stopAudioPlayback()
        }
    }

    private suspend fun playSynthesizedChime(durationMs: Long) {
        stopAudioPlayback()
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            val chunkSamples = 1024
            val buffer = ShortArray(chunkSamples)
            val startTime = System.currentTimeMillis()
            var phase1 = 0.0
            var phase2 = 0.0

            while (isAlarmActive && (System.currentTimeMillis() - startTime < durationMs)) {
                val cycleSec = ((System.currentTimeMillis() - startTime) % 1000) / 1000.0
                val decay = (1.0 - cycleSec).coerceIn(0.1, 1.0)
                val freq1 = if (cycleSec < 0.5) 880.0 else 1174.66
                val freq2 = freq1 * 1.5

                for (i in 0 until chunkSamples) {
                    val s1 = sin(phase1) * 0.7
                    val s2 = sin(phase2) * 0.3
                    val sample = ((s1 + s2) * decay * 32000.0).toInt().toShort()
                    buffer[i] = sample

                    phase1 += 2.0 * Math.PI * freq1 / sampleRate
                    if (phase1 > 2.0 * Math.PI) phase1 -= 2.0 * Math.PI
                    phase2 += 2.0 * Math.PI * freq2 / sampleRate
                    if (phase2 > 2.0 * Math.PI) phase2 -= 2.0 * Math.PI
                }
                audioTrack?.write(buffer, 0, chunkSamples)
            }
        } catch (e: Exception) {
            Log.e("AudioAlarmController", "Chime playback error", e)
        } finally {
            stopAudioPlayback()
        }
    }

    private suspend fun playSynthesizedBeeps(durationMs: Long) {
        stopAudioPlayback()
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            val chunkSamples = 1024
            val buffer = ShortArray(chunkSamples)
            val startTime = System.currentTimeMillis()
            var phase = 0.0
            val beepFreq = 2200.0

            while (isAlarmActive && (System.currentTimeMillis() - startTime < durationMs)) {
                // Beep 150ms on, 150ms off
                val isBeepOn = ((System.currentTimeMillis() - startTime) % 300) < 150
                for (i in 0 until chunkSamples) {
                    if (isBeepOn) {
                        buffer[i] = (sin(phase) * 32000.0).toInt().toShort()
                        phase += 2.0 * Math.PI * beepFreq / sampleRate
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    } else {
                        buffer[i] = 0
                    }
                }
                audioTrack?.write(buffer, 0, chunkSamples)
            }
        } catch (e: Exception) {
            Log.e("AudioAlarmController", "Beep error", e)
        } finally {
            stopAudioPlayback()
        }
    }

    private fun stopAudioPlayback() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        runCatching {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        }
    }

    private fun startFlashlightStrobe() {
        val cam = cameraManager ?: return
        val id = torchCameraId ?: return

        flashlightJob = coroutineScope.launch {
            var torchOn = false
            while (isActive && isAlarmActive) {
                runCatching {
                    torchOn = !torchOn
                    cam.setTorchMode(id, torchOn)
                }
                delay(200)
            }
            turnOffTorch()
        }
    }

    private fun turnOffTorch() {
        val cam = cameraManager ?: return
        val id = torchCameraId ?: return
        runCatching { cam.setTorchMode(id, false) }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.cancel()
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
        }
    }

    fun release() {
        stopAlarm()
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }
}
