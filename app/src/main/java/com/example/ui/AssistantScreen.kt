package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantSettings
import com.example.ui.components.AlarmConfigDialog
import com.example.ui.components.AssistantNameDialog
import com.example.ui.components.ListeningModeDialog
import com.example.ui.components.PowerOffNoticeCard
import com.example.ui.components.VoiceOrb
import com.example.ui.components.VoiceReplyDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAlarm
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.EmeraldActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val lastRecognizedText by viewModel.lastRecognizedText.collectAsState()
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showReplyDialog by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }

    // Permission launcher for Mic and Notifications
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.2f))
                                .border(1.dp, CyanPrimary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "App Logo",
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Awaaz AI Assistant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Voice Activated Phone Finder",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    // Language Selection Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = when {
                                    settings.hindiEnabled && settings.englishEnabled -> "हिन्दी + EN"
                                    settings.hindiEnabled -> "हिन्दी"
                                    else -> "English"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission Banner if missing
            if (!hasAudioPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CoralAlarm.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CoralAlarm, RoundedCornerShape(14.dp))
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Permission Required",
                            tint = CoralAlarm,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "माइक्रोफ़ोन अनुमति आवश्यक है",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "आवाज़ पहचानकर फोन खोजने के लिए अनुमति दें",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAlarm),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("grant_mic_permission_button")
                        ) {
                            Text("अनुमति दें", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Status Badge
            val statusColor by animateColorAsState(
                targetValue = when {
                    isAlarmActive -> CoralAlarm
                    isListening -> CyanPrimary
                    isServiceRunning -> EmeraldActive
                    else -> Color(0xFF64748B)
                },
                label = "status_color"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = when {
                        isAlarmActive -> "🚨 अलार्म बज रहा है (Alarm Ringing)"
                        isListening -> "🎙️ आवाज़ सुन रहा है (Listening…)"
                        isServiceRunning -> "🟢 एक्टिव (Background Active)"
                        else -> "⚪ बंद है (Service Inactive)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Central Glowing AI Voice Orb
            VoiceOrb(
                isServiceActive = isServiceRunning,
                isListening = isListening,
                isAlarmRinging = isAlarmActive,
                audioLevel = audioLevel,
                size = 200.dp,
                onClick = { viewModel.toggleService() },
                modifier = Modifier.testTag("voice_orb_interactive")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Assistant Name Display & Edit Chip
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = DarkSurfaceElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showNameDialog = true }
                    .testTag("edit_assistant_name_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "असिस्टेंट: ",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = settings.assistantName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanPrimary
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Live recognized speech indicator
            if (lastRecognizedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "सुना गया: \"$lastRecognizedText\"",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Master Toggle & Emergency Stop Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleService() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) Color(0xFF334155) else CyanPrimary,
                        contentColor = if (isServiceRunning) Color.White else DarkBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("toggle_voice_service_button")
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Toggle Service",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isServiceRunning) "सर्विस रोकें (Stop)" else "शुरू करें (Activate)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // If alarm is ringing, show Stop Alarm, else show Test Find My Phone
                if (isAlarmActive) {
                    Button(
                        onClick = { viewModel.stopAlarm() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralAlarm,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("stop_alarm_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AlarmOff,
                            contentDescription = "Stop Alarm",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "अलार्म बंद करें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.triggerFindPhoneTest() },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("test_find_phone_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Test Find",
                            tint = AmberAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "टेस्ट करें (Test)",
                            color = AmberAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Wake Word Instructions Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "How it works",
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "फोन कैसे खोजें (How to Find):",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "कमरे में कहीं भी आवाज़ लगाएँ:\n• \"Hey ${settings.assistantName}\"  या  \"${settings.assistantName} कहाँ हो\"\n• \"सुनो ${settings.assistantName}\"  या  \"${settings.assistantName} ढूंढो\"\n\nफोन तेज अलार्म बजाएगा और बोलकर जवाब देगा!",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings & Customization Grid
            Text(
                text = "फीचर्स और कस्टमाइज़ेशन (Settings)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            // 1. Voice Reply Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .clickable { showReplyDialog = true }
                    .testTag("feature_card_voice_reply")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Reply",
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "वॉइस रिप्लाई (Spoken Voice Reply)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "\"${settings.voiceReplyText}\"",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.previewVoice(
                                settings.voiceReplyText,
                                settings.ttsPitch,
                                settings.ttsSpeed,
                                settings.hindiEnabled
                            )
                        },
                        modifier = Modifier.testTag("quick_play_voice_preview_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = CyanPrimary
                        )
                    }
                }
            }

            // 2. Alarm Tone & Alerts Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .clickable { showAlarmDialog = true }
                    .testTag("feature_card_alarm_settings")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(AmberAccent.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alarm Alert",
                            tint = AmberAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "अलार्म, फ्लैशलाइट और रिंगटोन",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${settings.alarmTone.displayName} • ${if (settings.flashlightStrobe) "फ्लैशलाइट ON" else "फ्लैशलाइट OFF"}",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 3. Listening Mode & Battery Optimization Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .clickable { showModeDialog = true }
                    .testTag("feature_card_listening_mode")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldActive.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatterySaver,
                            contentDescription = "Battery Mode",
                            tint = EmeraldActive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "लिसनिंग मोड & बैटरी सेवर",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = settings.listeningMode.displayNameHindi,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 4. Language Selector Row
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "भाषा समर्थन (Hindi & English Voice)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = settings.hindiEnabled && settings.englishEnabled,
                            onClick = { viewModel.updateLanguageSupport(hindi = true, english = true) },
                            label = { Text("दोनों (Dual)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = DarkBg
                            ),
                            modifier = Modifier.weight(1f).testTag("lang_dual_chip")
                        )
                        FilterChip(
                            selected = settings.hindiEnabled && !settings.englishEnabled,
                            onClick = { viewModel.updateLanguageSupport(hindi = true, english = false) },
                            label = { Text("हिन्दी", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = DarkBg
                            ),
                            modifier = Modifier.weight(1f).testTag("lang_hindi_chip")
                        )
                        FilterChip(
                            selected = !settings.hindiEnabled && settings.englishEnabled,
                            onClick = { viewModel.updateLanguageSupport(hindi = false, english = true) },
                            label = { Text("English", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = DarkBg
                            ),
                            modifier = Modifier.weight(1f).testTag("lang_english_chip")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. System Limitations & Security Notice Card (Points 7, 9, 10, 11)
            PowerOffNoticeCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Dialogs
    if (showNameDialog) {
        AssistantNameDialog(
            currentName = settings.assistantName,
            onDismiss = { showNameDialog = false },
            onSaveName = { newName ->
                viewModel.updateAssistantName(newName)
                showNameDialog = false
            }
        )
    }

    if (showReplyDialog) {
        VoiceReplyDialog(
            currentHindiText = settings.voiceReplyText,
            currentEnglishText = settings.voiceReplyEnglish,
            currentEnabled = settings.voiceReplyEnabled,
            currentPitch = settings.ttsPitch,
            currentSpeed = settings.ttsSpeed,
            onDismiss = { showReplyDialog = false },
            onPreviewVoice = { text, pitch, speed, isHindi ->
                viewModel.previewVoice(text, pitch, speed, isHindi)
            },
            onSave = { hindi, english, enabled, pitch, speed ->
                viewModel.updateVoiceReply(hindi, english, enabled, pitch, speed)
                showReplyDialog = false
            }
        )
    }

    if (showAlarmDialog) {
        AlarmConfigDialog(
            currentTone = settings.alarmTone,
            currentMaxVol = settings.maxVolumeOverride,
            currentFlash = settings.flashlightStrobe,
            currentVib = settings.vibrationEnabled,
            onDismiss = { showAlarmDialog = false },
            onSave = { tone, maxVol, flash, vib ->
                viewModel.updateAlarmOptions(tone, maxVol, flash, vib)
                showAlarmDialog = false
            }
        )
    }

    if (showModeDialog) {
        ListeningModeDialog(
            currentMode = settings.listeningMode,
            onDismiss = { showModeDialog = false },
            onSave = { mode ->
                viewModel.updateListeningMode(mode)
                showModeDialog = false
            }
        )
    }
}
