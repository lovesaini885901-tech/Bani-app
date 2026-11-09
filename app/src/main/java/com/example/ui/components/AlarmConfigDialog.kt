package com.example.ui.components

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
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.AlarmToneType
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated

@Composable
fun AlarmConfigDialog(
    currentTone: AlarmToneType,
    currentMaxVol: Boolean,
    currentFlash: Boolean,
    currentVib: Boolean,
    onDismiss: () -> Unit,
    onSave: (tone: AlarmToneType, maxVol: Boolean, flash: Boolean, vib: Boolean) -> Unit
) {
    var selectedTone by remember { mutableStateOf(currentTone) }
    var maxVol by remember { mutableStateOf(currentMaxVol) }
    var flash by remember { mutableStateOf(currentFlash) }
    var vib by remember { mutableStateOf(currentVib) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = "Alarm Settings",
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "अलार्म और रिंगटोन सेटिंग्स",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "रिंगटोन टोन चुनें (Alarm Tone):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                AlarmToneType.values().forEach { tone ->
                    val isSelected = selectedTone == tone
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedTone = tone }
                            .testTag("alarm_tone_${tone.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTone = tone },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CyanPrimary,
                                    unselectedColor = DarkBorder
                                )
                            )
                            Text(
                                text = tone.displayName,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "अलार्म अलर्ट विकल्प:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Max Volume Override
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "अधिकतम वॉल्यूम ओवरराइड",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "साइलेंट/वाइब्रेट होने पर भी तेज आवाज़",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Switch(
                        checked = maxVol,
                        onCheckedChange = { maxVol = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("max_volume_override_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Flashlight Strobe
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "फ्लैशलाइट ब्लिंक (Strobe)",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "अंधेरे कमरे या तकिए के नीचे खोजने के लिए",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Switch(
                        checked = flash,
                        onCheckedChange = { flash = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("flashlight_strobe_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Vibration Alert
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibration",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "तेज़ वाइब्रेशन अलर्ट",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Text(
                                text = "स्पंदन पैटर्न से तुरंत ध्यान आकर्षित करता है",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Switch(
                        checked = vib,
                        onCheckedChange = { vib = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanPrimary
                        ),
                        modifier = Modifier.testTag("vibration_alert_switch")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedTone, maxVol, flash, vib)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_alarm_config_button")
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
