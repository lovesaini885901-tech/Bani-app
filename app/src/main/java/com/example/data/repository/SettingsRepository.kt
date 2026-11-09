package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AlarmToneType
import com.example.data.model.AssistantSettings
import com.example.data.model.ListeningMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("awaaz_assistant_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AssistantSettings> = _settings.asStateFlow()

    private fun loadSettings(): AssistantSettings {
        val name = prefs.getString(KEY_NAME, "Jarvis") ?: "Jarvis"
        val hindi = prefs.getBoolean(KEY_HINDI, true)
        val english = prefs.getBoolean(KEY_ENGLISH, true)
        val replyText = prefs.getString(KEY_REPLY_HINDI, "मैं यहाँ हूँ, मुझे ढूँढ लो!") ?: "मैं यहाँ हूँ, मुझे ढूँढ लो!"
        val replyEn = prefs.getString(KEY_REPLY_EN, "I am here, find me!") ?: "I am here, find me!"
        val replyEnabled = prefs.getBoolean(KEY_REPLY_ENABLED, true)
        val toneName = prefs.getString(KEY_ALARM_TONE, AlarmToneType.SIREN.name) ?: AlarmToneType.SIREN.name
        val tone = runCatching { AlarmToneType.valueOf(toneName) }.getOrDefault(AlarmToneType.SIREN)
        val maxVol = prefs.getBoolean(KEY_MAX_VOL, true)
        val flash = prefs.getBoolean(KEY_FLASH, true)
        val vib = prefs.getBoolean(KEY_VIB, true)
        val modeName = prefs.getString(KEY_MODE, ListeningMode.BATTERY_SAVER.name) ?: ListeningMode.BATTERY_SAVER.name
        val mode = runCatching { ListeningMode.valueOf(modeName) }.getOrDefault(ListeningMode.BATTERY_SAVER)
        val pitch = prefs.getFloat(KEY_PITCH, 1.0f)
        val speed = prefs.getFloat(KEY_SPEED, 1.0f)
        val serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        val triggers = prefs.getInt(KEY_TRIGGER_COUNT, 0)
        val lastTrigger = prefs.getLong(KEY_LAST_TRIGGER, 0L)

        return AssistantSettings(
            assistantName = name,
            hindiEnabled = hindi,
            englishEnabled = english,
            voiceReplyText = replyText,
            voiceReplyEnglish = replyEn,
            voiceReplyEnabled = replyEnabled,
            alarmTone = tone,
            maxVolumeOverride = maxVol,
            flashlightStrobe = flash,
            vibrationEnabled = vib,
            listeningMode = mode,
            ttsPitch = pitch,
            ttsSpeed = speed,
            isServiceEnabled = serviceEnabled,
            triggerCount = triggers,
            lastTriggerTime = lastTrigger
        )
    }

    fun updateAssistantName(name: String) {
        val cleanName = name.trim().ifEmpty { "Jarvis" }
        prefs.edit().putString(KEY_NAME, cleanName).apply()
        _settings.value = _settings.value.copy(assistantName = cleanName)
    }

    fun updateLanguageSupport(hindi: Boolean, english: Boolean) {
        // Ensure at least one language is active
        val validHindi = if (!hindi && !english) true else hindi
        val validEnglish = if (!hindi && !english) false else english
        prefs.edit()
            .putBoolean(KEY_HINDI, validHindi)
            .putBoolean(KEY_ENGLISH, validEnglish)
            .apply()
        _settings.value = _settings.value.copy(hindiEnabled = validHindi, englishEnabled = validEnglish)
    }

    fun updateVoiceReply(text: String, englishText: String, enabled: Boolean) {
        prefs.edit()
            .putString(KEY_REPLY_HINDI, text)
            .putString(KEY_REPLY_EN, englishText)
            .putBoolean(KEY_REPLY_ENABLED, enabled)
            .apply()
        _settings.value = _settings.value.copy(
            voiceReplyText = text,
            voiceReplyEnglish = englishText,
            voiceReplyEnabled = enabled
        )
    }

    fun updateAlarmTone(tone: AlarmToneType) {
        prefs.edit().putString(KEY_ALARM_TONE, tone.name).apply()
        _settings.value = _settings.value.copy(alarmTone = tone)
    }

    fun updateAlarmOptions(maxVol: Boolean, flash: Boolean, vib: Boolean) {
        prefs.edit()
            .putBoolean(KEY_MAX_VOL, maxVol)
            .putBoolean(KEY_FLASH, flash)
            .putBoolean(KEY_VIB, vib)
            .apply()
        _settings.value = _settings.value.copy(
            maxVolumeOverride = maxVol,
            flashlightStrobe = flash,
            vibrationEnabled = vib
        )
    }

    fun updateListeningMode(mode: ListeningMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _settings.value = _settings.value.copy(listeningMode = mode)
    }

    fun updateTtsVoice(pitch: Float, speed: Float) {
        prefs.edit()
            .putFloat(KEY_PITCH, pitch)
            .putFloat(KEY_SPEED, speed)
            .apply()
        _settings.value = _settings.value.copy(ttsPitch = pitch, ttsSpeed = speed)
    }

    fun updateServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(isServiceEnabled = enabled)
    }

    fun recordTriggerEvent() {
        val count = _settings.value.triggerCount + 1
        val time = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_TRIGGER_COUNT, count)
            .putLong(KEY_LAST_TRIGGER, time)
            .apply()
        _settings.value = _settings.value.copy(triggerCount = count, lastTriggerTime = time)
    }

    companion object {
        private const val KEY_NAME = "assistant_name"
        private const val KEY_HINDI = "lang_hindi"
        private const val KEY_ENGLISH = "lang_english"
        private const val KEY_REPLY_HINDI = "reply_hindi"
        private const val KEY_REPLY_EN = "reply_en"
        private const val KEY_REPLY_ENABLED = "reply_enabled"
        private const val KEY_ALARM_TONE = "alarm_tone"
        private const val KEY_MAX_VOL = "max_volume"
        private const val KEY_FLASH = "flashlight"
        private const val KEY_VIB = "vibration"
        private const val KEY_MODE = "listening_mode"
        private const val KEY_PITCH = "tts_pitch"
        private const val KEY_SPEED = "tts_speed"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_TRIGGER_COUNT = "trigger_count"
        private const val KEY_LAST_TRIGGER = "last_trigger_time"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
