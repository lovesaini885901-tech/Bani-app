package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AssistantSettings
import com.example.data.model.ListeningMode
import com.example.data.repository.SettingsRepository
import com.example.ui.alarm.LockScreenAlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceActivationService : Service(), RecognitionListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var alarmController: AudioAlarmController

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isPaused = false
    private var isScreenOn = true

    private var wakeLock: PowerManager.WakeLock? = null
    private var settingsJob: Job? = null

    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository.getInstance(applicationContext)
        alarmController = AudioAlarmController(applicationContext)

        // WakeLock for background execution
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Awaaz:VoiceWakeLock")

        createNotificationChannel()

        // Screen On/Off receiver for Screen-On Only mode
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOn = true
                        val currentMode = settingsRepo.settings.value.listeningMode
                        if (currentMode == ListeningMode.SCREEN_ON_ONLY && !isPaused && !isListening) {
                            startSpeechRecognition()
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOn = false
                        val currentMode = settingsRepo.settings.value.listeningMode
                        if (currentMode == ListeningMode.SCREEN_ON_ONLY && isListening) {
                            stopSpeechRecognition()
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)

        // Observe settings changes
        settingsJob = serviceScope.launch {
            settingsRepo.settings.collect { settings ->
                updateNotification(settings, alarmController.isAlarmRinging.value)
            }
        }

        serviceScope.launch {
            alarmController.isAlarmRinging.collect { ringing ->
                _isAlarmActiveFlow.value = ringing
                updateNotification(settingsRepo.settings.value, ringing)
            }
        }

        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                isPaused = false
                val settings = settingsRepo.settings.value
                val notification = buildForegroundNotification(settings, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes safe window, refreshed
                settingsRepo.updateServiceEnabled(true)
                _isServiceRunningFlow.value = true

                mainHandler.post { startSpeechRecognition() }
            }
            ACTION_STOP -> {
                stopListeningAndService()
            }
            ACTION_STOP_ALARM -> {
                alarmController.stopAlarm()
                updateNotification(settingsRepo.settings.value, false)
                // Resume speech recognition
                mainHandler.postDelayed({
                    if (!isPaused) startSpeechRecognition()
                }, 1000)
            }
            ACTION_TOGGLE_PAUSE -> {
                isPaused = !isPaused
                if (isPaused) {
                    stopSpeechRecognition()
                } else {
                    startSpeechRecognition()
                }
                updateNotification(settingsRepo.settings.value, alarmController.isAlarmRinging.value)
            }
            ACTION_TRIGGER_FIND -> {
                onWakeWordDetected("Manual / Voice Trigger")
            }
        }

        return START_STICKY
    }

    private fun startSpeechRecognition() {
        if (isPaused || alarmController.isAlarmRinging.value) return
        val currentMode = settingsRepo.settings.value.listeningMode
        if (currentMode == ListeningMode.SCREEN_ON_ONLY && !isScreenOn) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w("VoiceService", "Speech recognition not available on device")
            _lastRecognizedTextFlow.value = "Speech recognition unavailable on this engine"
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(this@VoiceActivationService)
                }
            }

            val settings = settingsRepo.settings.value
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                // Select language preference
                if (settings.hindiEnabled && !settings.englishEnabled) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                } else if (!settings.hindiEnabled && settings.englishEnabled) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                } else {
                    // Both Hindi & English: Default to hi-IN with en-IN extra
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "en-US"))
                }
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            _isListeningFlow.value = true
        } catch (e: Exception) {
            Log.e("VoiceService", "Error starting speech recognition", e)
            scheduleRestart(1500)
        }
    }

    private fun stopSpeechRecognition() {
        isListening = false
        _isListeningFlow.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w("VoiceService", "Error stopping recognizer", e)
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        stopSpeechRecognition()
        if (isPaused || alarmController.isAlarmRinging.value) return

        val settings = settingsRepo.settings.value
        val effectiveDelay = when (settings.listeningMode) {
            ListeningMode.CONTINUOUS -> delayMs.coerceAtLeast(300L)
            ListeningMode.BATTERY_SAVER -> delayMs.coerceAtLeast(2500L) // Conserves battery cycles
            ListeningMode.SCREEN_ON_ONLY -> if (isScreenOn) delayMs.coerceAtLeast(1000L) else 5000L
        }

        mainHandler.postDelayed({
            if (!isPaused && !alarmController.isAlarmRinging.value) {
                startSpeechRecognition()
            }
        }, effectiveDelay)
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {
        _isListeningFlow.value = true
    }

    override fun onBeginningOfSpeech() {}

    override fun onRmsChanged(rmsdB: Float) {
        _audioLevelFlow.value = rmsdB.coerceIn(0f, 10f) / 10f
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        _audioLevelFlow.value = 0f
    }

    override fun onError(error: Int) {
        _audioLevelFlow.value = 0f
        Log.d("VoiceService", "Recognition error code: $error")
        // Schedule smooth recovery
        scheduleRestart(1000)
    }

    override fun onResults(results: Bundle?) {
        _audioLevelFlow.value = 0f
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        processRecognizedMatches(matches)
        scheduleRestart(500)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            _lastRecognizedTextFlow.value = text
            if (checkWakeWordTrigger(text)) {
                onWakeWordDetected(text)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun processRecognizedMatches(matches: ArrayList<String>?) {
        if (matches.isNullOrEmpty()) return
        val top = matches[0]
        _lastRecognizedTextFlow.value = top

        for (match in matches) {
            if (checkWakeWordTrigger(match)) {
                onWakeWordDetected(match)
                return
            }
        }
    }

    /**
     * Checks if recognized speech matches the user's custom assistant name
     * or common find phrases in Hindi & English.
     */
    fun checkWakeWordTrigger(input: String): Boolean {
        val settings = settingsRepo.settings.value
        val customName = settings.assistantName.trim().lowercase(Locale.ROOT)
        val text = input.trim().lowercase(Locale.ROOT)

        // 1. Direct contains or equals assistant name
        if (text.contains(customName)) return true

        // 2. Phonetic / Transliteration Hindi variants
        val hindiEquivalents = getHindiVariantsForName(customName)
        for (variant in hindiEquivalents) {
            if (text.contains(variant.lowercase(Locale.ROOT))) return true
        }

        // 3. Common prefixes: "hey <name>", "ok <name>", "सुनो <name>", "अरे <name>"
        val prefixes = listOf("hey", "ok", "okay", "hi", "hello", "हे", "अरे", "सुनो", "ओ")
        for (prefix in prefixes) {
            if (text.contains("$prefix $customName") || text.contains("$prefix$customName")) return true
        }

        // 4. Find Phone commands with name context or explicit find triggers
        val findKeywordsHindi = listOf("ढूंढो", "कहाँ हो", "कहा हो", "बजाओ", "किधर हो", "मेरा फोन", "फोन ढूंढो", "आवाज दो")
        val findKeywordsEnglish = listOf("find my phone", "where are you", "find phone", "ring phone", "ring", "locate phone")

        for (kw in findKeywordsHindi) {
            if (text.contains(kw) && (text.contains(customName) || text.length < 20)) {
                return true
            }
        }
        for (kw in findKeywordsEnglish) {
            if (text.contains(kw) && (text.contains(customName) || text.length < 25)) {
                return true
            }
        }

        return false
    }

    private fun getHindiVariantsForName(name: String): List<String> {
        val lower = name.lowercase(Locale.ROOT)
        return when (lower) {
            "jarvis" -> listOf("जार्विस", "ज़ार्विस", "जर्विश", "jarvish")
            "dost", "दोस्त" -> listOf("दोस्त", "dost", "dosth", "मित्र")
            "alexa" -> listOf("एलेक्सा", "अलैक्सा", "अलेक्सा")
            "siri" -> listOf("सिरी", "सीरी")
            "chitti", "चिट्टी" -> listOf("चिट्टी", "चिट्ठी", "chitti")
            "bhai", "भाई" -> listOf("भाई", "bhai", "भैया")
            "awaaz", "आवाज़" -> listOf("आवाज़", "आवाज", "awaaz", "avaz")
            else -> listOf(name)
        }
    }

    private fun onWakeWordDetected(sourceText: String) {
        Log.i("VoiceService", "Wake word triggered: $sourceText")
        stopSpeechRecognition()

        // Record in repository
        settingsRepo.recordTriggerEvent()

        // 1. Trigger Alarm & Spoken Reply
        alarmController.startAlarm(settingsRepo.settings.value)

        // 2. Launch Full Screen Lock Screen Activity
        val alarmIntent = Intent(this, LockScreenAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LockScreenAlarmActivity.EXTRA_TRIGGER_TEXT, sourceText)
        }
        startActivity(alarmIntent)

        // 3. Update Notification with Alert
        updateNotification(settingsRepo.settings.value, true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(settings: AssistantSettings, isRinging: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val content: String

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(if (isRinging) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)

        if (isRinging) {
            title = "🚨 " + getString(R.string.notification_alarm_title)
            content = getString(R.string.notification_alarm_text)

            val stopAlarmIntent = Intent(this, VoiceActivationService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            val stopAlarmPending = PendingIntent.getService(
                this, 1, stopAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_lock_power_off, getString(R.string.action_stop_alarm), stopAlarmPending)
        } else {
            val name = settings.assistantName
            title = if (isPaused) {
                "⏸️ $name रुका हुआ है (Paused)"
            } else {
                "🎙️ $name सुन रहा है (Listening…)"
            }
            content = if (isPaused) {
                "पुनः शुरू करने के लिए टैप करें"
            } else {
                "\"$name\" या \"Hey $name\" बोलकर फोन खोजें"
            }

            val pauseIntent = Intent(this, VoiceActivationService::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }
            val pausePending = PendingIntent.getService(
                this, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val pauseLabel = if (isPaused) getString(R.string.action_toggle_resume) else getString(R.string.action_toggle_pause)
            builder.addAction(android.R.drawable.ic_media_pause, pauseLabel, pausePending)
        }

        return builder
            .setContentTitle(title)
            .setContentText(content)
            .build()
    }

    private fun updateNotification(settings: AssistantSettings, isRinging: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildForegroundNotification(settings, isRinging))
    }

    private fun stopListeningAndService() {
        stopSpeechRecognition()
        alarmController.stopAlarm()
        settingsRepo.updateServiceEnabled(false)
        _isServiceRunningFlow.value = false

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpeechRecognition()
        alarmController.release()
        runCatching { unregisterReceiver(screenReceiver) }
        settingsJob?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        instance = null
        _isServiceRunningFlow.value = false
        _isListeningFlow.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "awaaz_voice_service_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.awaaz.ACTION_START"
        const val ACTION_STOP = "com.example.awaaz.ACTION_STOP"
        const val ACTION_STOP_ALARM = "com.example.awaaz.ACTION_STOP_ALARM"
        const val ACTION_TOGGLE_PAUSE = "com.example.awaaz.ACTION_TOGGLE_PAUSE"
        const val ACTION_TRIGGER_FIND = "com.example.awaaz.ACTION_TRIGGER_FIND"

        @Volatile
        var instance: VoiceActivationService? = null
            private set

        private val _isServiceRunningFlow = MutableStateFlow(false)
        val isServiceRunningFlow: StateFlow<Boolean> = _isServiceRunningFlow.asStateFlow()

        private val _isListeningFlow = MutableStateFlow(false)
        val isListeningFlow: StateFlow<Boolean> = _isListeningFlow.asStateFlow()

        private val _isAlarmActiveFlow = MutableStateFlow(false)
        val isAlarmActiveFlow: StateFlow<Boolean> = _isAlarmActiveFlow.asStateFlow()

        private val _lastRecognizedTextFlow = MutableStateFlow("")
        val lastRecognizedTextFlow: StateFlow<String> = _lastRecognizedTextFlow.asStateFlow()

        private val _audioLevelFlow = MutableStateFlow(0f)
        val audioLevelFlow: StateFlow<Float> = _audioLevelFlow.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, VoiceActivationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceActivationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, VoiceActivationService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }

        fun triggerManualFind(context: Context) {
            val intent = Intent(context, VoiceActivationService::class.java).apply {
                action = ACTION_TRIGGER_FIND
            }
            context.startService(intent)
        }
    }
}
