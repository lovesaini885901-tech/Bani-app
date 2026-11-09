package com.example.data.model

enum class ListeningMode(val displayNameHindi: String, val displayNameEnglish: String, val description: String) {
    CONTINUOUS(
        displayNameHindi = "निरंतर (Continuous)",
        displayNameEnglish = "Continuous Listening",
        description = "तुरंत प्रतिक्रिया - जब आप कमरे में फोन खोज रहे हों तब सबसे अच्छा"
    ),
    BATTERY_SAVER(
        displayNameHindi = "स्मार्ट बैटरी सेवर (Battery Saver)",
        displayNameEnglish = "Smart Battery Saver",
        description = "हल्के अंतराल के साथ सुनता है जिससे 60% बैटरी बचती है"
    ),
    SCREEN_ON_ONLY(
        displayNameHindi = "स्क्रीन चालू पर (Screen-On Only)",
        displayNameEnglish = "Screen-On Only",
        description = "केवल तभी सुनता है जब स्क्रीन ऑन हो - अधिकतम बैटरी बचत"
    )
}

enum class AlarmToneType(val displayName: String) {
    SIREN("🚨 High Alert Siren (तेज़ सायरन)"),
    DEFAULT_ALARM("⏰ Standard Alarm (डिफ़ॉल्ट अलार्म)"),
    CHIME("🔔 Loud Electronic Chime (इलेक्ट्रॉनिक चाइम)"),
    PULSE_BEEP("⚡ High Pitch Beep (हाई पिच बीप)"),
    SYSTEM_RINGTONE("📱 Phone Ringtone (सिस्टम रिंगटोन)")
}

data class AssistantSettings(
    val assistantName: String = "Jarvis",
    val hindiEnabled: Boolean = true,
    val englishEnabled: Boolean = true,
    val voiceReplyText: String = "मैं यहाँ हूँ, मुझे ढूँढ लो!",
    val voiceReplyEnglish: String = "I am here, find me!",
    val voiceReplyEnabled: Boolean = true,
    val alarmTone: AlarmToneType = AlarmToneType.SIREN,
    val maxVolumeOverride: Boolean = true,
    val flashlightStrobe: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val listeningMode: ListeningMode = ListeningMode.BATTERY_SAVER,
    val ttsPitch: Float = 1.0f,
    val ttsSpeed: Float = 1.0f,
    val isServiceEnabled: Boolean = false,
    val triggerCount: Int = 0,
    val lastTriggerTime: Long = 0L
)
