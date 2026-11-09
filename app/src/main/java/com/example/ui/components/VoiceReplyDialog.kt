package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated

@Composable
fun VoiceReplyDialog(
    currentHindiText: String,
    currentEnglishText: String,
    currentEnabled: Boolean,
    currentPitch: Float,
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onPreviewVoice: (text: String, pitch: Float, speed: Float, isHindi: Boolean) -> Unit,
    onSave: (hindi: String, english: String, enabled: Boolean, pitch: Float, speed: Float) -> Unit
) {
    var hindiText by remember { mutableStateOf(currentHindiText) }
    var englishText by remember { mutableStateOf(currentEnglishText) }
    var enabled by remember { mutableStateOf(currentEnabled) }
    var pitch by remember { mutableFloatStateOf(currentPitch) }
    var speed by remember { mutableFloatStateOf(currentSpeed) }

    val quickRepliesHindi = listOf(
        "मैं यहाँ हूँ, मुझे ढूँढ लो!",
        "हाँ, मैं यहाँ हूँ!",
        "अरे भाई, मैं इधर हूँ!",
        "फोन यहाँ रखा है!"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Voice Reply",
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "वॉइस रिप्लाई कस्टमाइज़ करें",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Toggle Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "वॉइस रिप्लाई चालू रखें",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "पुकारने पर फोन बोलकर जवाब देगा",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("voice_reply_toggle_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hindi Reply Text Field
                OutlinedTextField(
                    value = hindiText,
                    onValueChange = { hindiText = it },
                    label = { Text("हिंदी में उत्तर (Hindi Response)") },
                    placeholder = { Text("मैं यहाँ हूँ, मुझे ढूँढ लो!") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hindi_voice_reply_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick reply chips
                Text(
                    text = "त्वरित सुझाव (Quick Hindi Suggestions):",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                quickRepliesHindi.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hindiText == suggestion) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { hindiText = suggestion }
                    ) {
                        Text(
                            text = "• $suggestion",
                            fontSize = 12.sp,
                            color = if (hindiText == suggestion) CyanPrimary else Color(0xFFCBD5E1),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // English Reply Text Field
                OutlinedTextField(
                    value = englishText,
                    onValueChange = { englishText = it },
                    label = { Text("English Response") },
                    placeholder = { Text("I am here, find me!") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("english_voice_reply_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pitch & Speed Controls
                Text(
                    text = "आवाज़ का पिच (Pitch): ${String.format("%.1f", pitch)}x",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary
                    )
                )

                Text(
                    text = "बोलने की गति (Speed): ${String.format("%.1f", speed)}x",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPrimary,
                        activeTrackColor = CyanPrimary
                    )
                )

                // Test Voice Button
                OutlinedButton(
                    onClick = {
                        onPreviewVoice(hindiText.ifEmpty { "मैं यहाँ हूँ, मुझे ढूँढ लो!" }, pitch, speed, true)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preview_voice_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Test Voice",
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "आवाज़ सुनकर टेस्ट करें (Play Preview)",
                        fontSize = 12.sp,
                        color = CyanPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(hindiText.trim(), englishText.trim(), enabled, pitch, speed)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_voice_reply_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "सेव करें",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "रद्द करें",
                    color = Color(0xFF94A3B8)
                )
            }
        }
    )
}
