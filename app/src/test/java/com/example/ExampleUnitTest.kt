package com.example

import com.example.data.model.AlarmToneType
import com.example.data.model.AssistantSettings
import com.example.data.model.ListeningMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun `default assistant settings are properly initialized`() {
        val settings = AssistantSettings()
        assertEquals("Jarvis", settings.assistantName)
        assertEquals("मैं यहाँ हूँ, मुझे ढूँढ लो!", settings.voiceReplyText)
        assertTrue(settings.hindiEnabled)
        assertTrue(settings.englishEnabled)
        assertTrue(settings.voiceReplyEnabled)
        assertEquals(AlarmToneType.SIREN, settings.alarmTone)
        assertEquals(ListeningMode.BATTERY_SAVER, settings.listeningMode)
    }

    @Test
    fun `wake word matching logic identifies custom name and variants`() {
        fun matchesTrigger(spokenText: String, customName: String): Boolean {
            val name = customName.trim().lowercase(Locale.ROOT)
            val text = spokenText.trim().lowercase(Locale.ROOT)

            if (text.contains(name)) return true

            val hindiVariants = when (name) {
                "jarvis" -> listOf("जार्विस", "ज़ार्विस", "जर्विश")
                "dost", "दोस्त" -> listOf("दोस्त", "dost")
                "alexa" -> listOf("एलेक्सा", "अलैक्सा")
                else -> listOf(name)
            }
            if (hindiVariants.any { text.contains(it) }) return true

            val prefixes = listOf("hey", "ok", "okay", "hi", "hello", "हे", "अरे", "सुनो")
            if (prefixes.any { text.contains("$it $name") || text.contains("$it$name") }) return true

            val findKeywords = listOf("ढूंढो", "कहाँ हो", "find my phone", "where are you", "phone")
            return findKeywords.any { text.contains(it) && text.contains(name) }
        }

        // Test Jarvis
        assertTrue(matchesTrigger("hey jarvis", "Jarvis"))
        assertTrue(matchesTrigger("Hey Jarvis", "Jarvis"))
        assertTrue(matchesTrigger("जार्विस कहाँ हो", "Jarvis"))
        assertTrue(matchesTrigger("सुनो जार्विस ढूंढो", "Jarvis"))
        assertTrue(matchesTrigger("where are you jarvis", "Jarvis"))

        // Test custom name: Dost
        assertTrue(matchesTrigger("अरे दोस्त कहाँ हो", "दोस्त"))
        assertTrue(matchesTrigger("dost find my phone", "Dost"))

        // Negative test
        assertFalse(matchesTrigger("hello world google", "Jarvis"))
    }
}
