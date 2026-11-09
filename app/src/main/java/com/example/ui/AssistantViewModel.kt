package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlarmToneType
import com.example.data.model.AssistantSettings
import com.example.data.model.ListeningMode
import com.example.data.repository.SettingsRepository
import com.example.service.AudioAlarmController
import com.example.service.VoiceActivationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository.getInstance(application)
    val settings: StateFlow<AssistantSettings> = repository.settings

    val isServiceRunning: StateFlow<Boolean> = VoiceActivationService.isServiceRunningFlow
    val isListening: StateFlow<Boolean> = VoiceActivationService.isListeningFlow
    val isAlarmActive: StateFlow<Boolean> = VoiceActivationService.isAlarmActiveFlow
    val audioLevel: StateFlow<Float> = VoiceActivationService.audioLevelFlow
    val lastRecognizedText: StateFlow<String> = VoiceActivationService.lastRecognizedTextFlow

    private val previewAlarmController = AudioAlarmController(application)

    private val _hasAudioPermission = MutableStateFlow(checkAudioPermission())
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(checkNotificationPermission())
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    fun refreshPermissions() {
        _hasAudioPermission.value = checkAudioPermission()
        _hasNotificationPermission.value = checkNotificationPermission()
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                getApplication(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun toggleService() {
        val app = getApplication<Application>()
        if (isServiceRunning.value) {
            VoiceActivationService.stop(app)
        } else {
            VoiceActivationService.start(app)
        }
    }

    fun stopAlarm() {
        val app = getApplication<Application>()
        VoiceActivationService.stopAlarm(app)
    }

    fun triggerFindPhoneTest() {
        val app = getApplication<Application>()
        // If service is running, send manual trigger, otherwise start service or alarm
        if (!isServiceRunning.value) {
            VoiceActivationService.start(app)
        }
        VoiceActivationService.triggerManualFind(app)
    }

    fun updateAssistantName(name: String) {
        repository.updateAssistantName(name)
    }

    fun updateLanguageSupport(hindi: Boolean, english: Boolean) {
        repository.updateLanguageSupport(hindi, english)
    }

    fun updateVoiceReply(hindi: String, english: String, enabled: Boolean, pitch: Float, speed: Float) {
        repository.updateVoiceReply(hindi, english, enabled)
        repository.updateTtsVoice(pitch, speed)
    }

    fun updateAlarmOptions(tone: AlarmToneType, maxVol: Boolean, flash: Boolean, vib: Boolean) {
        repository.updateAlarmTone(tone)
        repository.updateAlarmOptions(maxVol, flash, vib)
    }

    fun updateListeningMode(mode: ListeningMode) {
        repository.updateListeningMode(mode)
    }

    fun previewVoice(text: String, pitch: Float, speed: Float, isHindi: Boolean) {
        previewAlarmController.speakPreview(text, pitch, speed, isHindi)
    }

    override fun onCleared() {
        super.onCleared()
        previewAlarmController.release()
    }
}
