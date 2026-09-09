package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommandActionType
import com.example.data.model.CustomCommand
import com.example.data.model.InstalledAppInfo
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisNavyBg
import com.example.ui.theme.JarvisNavyCard
import com.example.ui.theme.JarvisNavyCardBorder
import com.example.ui.theme.JarvisSchoolRed
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@Composable
fun CustomCommandsSection(
    commands: List<CustomCommand>,
    installedApps: List<InstalledAppInfo>,
    onAddCommand: (trigger: String, actionType: CommandActionType, targetPackage: String?, targetAppName: String?, response: String?) -> Unit,
    onUpdateCommand: (CustomCommand) -> Unit,
    onDeleteCommand: (CustomCommand) -> Unit,
    onToggleCommand: (CustomCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<CustomCommand?>(null) }
    var commandToDelete by remember { mutableStateOf<CustomCommand?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CUSTOM COMMANDS",
                    color = JarvisTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "${commands.count { it.isEnabled }} active / ${commands.size} total",
                    color = JarvisTextSecondary,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCyanDark,
                    contentColor = JarvisCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add Command",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (commands.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(JarvisNavyCard)
                    .border(1.dp, JarvisNavyCardBorder, RoundedCornerShape(14.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No custom commands yet",
                        color = JarvisTextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Tap '+ Add Command' to teach JARVIS new phrases",
                        color = JarvisTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                commands.forEach { command ->
                    CustomCommandItem(
                        command = command,
                        onToggle = { onToggleCommand(command) },
                        onEdit = { commandToEdit = command },
                        onDelete = { commandToDelete = command }
                    )
                }
            }
        }
    }

    // Add Command Dialog
    if (showAddDialog) {
        CommandEditDialog(
            command = null,
            installedApps = installedApps,
            onDismiss = { showAddDialog = false },
            onSave = { trigger, action, pkg, appName, response ->
                onAddCommand(trigger, action, pkg, appName, response)
                showAddDialog = false
            }
        )
    }

    // Edit Command Dialog
    commandToEdit?.let { cmd ->
        CommandEditDialog(
            command = cmd,
            installedApps = installedApps,
            onDismiss = { commandToEdit = null },
            onSave = { trigger, action, pkg, appName, response ->
                onUpdateCommand(
                    cmd.copy(
                        triggerPhrase = trigger,
                        actionType = action.name,
                        targetPackage = pkg,
                        targetAppName = appName,
                        responseText = response
                    )
                )
                commandToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    commandToDelete?.let { cmd ->
        AlertDialog(
            onDismissRequest = { commandToDelete = null },
            title = {
                Text("Delete Command?", color = JarvisTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to remove the voice command \"${cmd.triggerPhrase}\"?",
                    color = JarvisTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCommand(cmd)
                        commandToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSchoolRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { commandToDelete = null }) {
                    Text("Cancel", color = JarvisTextSecondary)
                }
            },
            containerColor = JarvisNavyCard
        )
    }
}

@Composable
private fun CustomCommandItem(
    command: CustomCommand,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val actionType = try {
        CommandActionType.valueOf(command.actionType)
    } catch (e: Exception) {
        CommandActionType.OPEN_APP
    }

    val actionIcon = when (actionType) {
        CommandActionType.OPEN_APP -> Icons.Default.Apps
        CommandActionType.GO_HOME -> Icons.Default.Home
        CommandActionType.ACTIVATE_SCHOOL_MODE -> Icons.Default.School
        CommandActionType.DEACTIVATE_SCHOOL_MODE -> Icons.Default.School
        CommandActionType.SPEAK_TEXT -> Icons.Default.RecordVoiceOver
    }

    val actionColor = when (actionType) {
        CommandActionType.OPEN_APP -> JarvisCyan
        CommandActionType.GO_HOME -> JarvisGreen
        CommandActionType.ACTIVATE_SCHOOL_MODE -> JarvisSchoolRed
        CommandActionType.DEACTIVATE_SCHOOL_MODE -> JarvisAmber
        CommandActionType.SPEAK_TEXT -> Color(0xFF00E5FF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisNavyCard)
            .border(
                1.dp,
                if (command.isEnabled) JarvisNavyCardBorder else JarvisNavyCardBorder.copy(alpha = 0.4f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(actionColor.copy(alpha = if (command.isEnabled) 0.15f else 0.05f))
                    .border(
                        1.dp,
                        actionColor.copy(alpha = if (command.isEnabled) 0.4f else 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                    tint = if (command.isEnabled) actionColor else JarvisTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\"${command.triggerPhrase}\"",
                        color = if (command.isEnabled) JarvisTextPrimary else JarvisTextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val actionSummary = when (actionType) {
                    CommandActionType.OPEN_APP -> "Opens ${command.targetAppName ?: "App"}"
                    CommandActionType.GO_HOME -> "Navigates to Home screen"
                    CommandActionType.ACTIVATE_SCHOOL_MODE -> "Activates School Mode"
                    CommandActionType.DEACTIVATE_SCHOOL_MODE -> "Deactivates School Mode"
                    CommandActionType.SPEAK_TEXT -> "Speaks custom response"
                }

                Text(
                    text = actionSummary,
                    color = if (command.isEnabled) actionColor.copy(alpha = 0.9f) else JarvisTextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit command",
                    tint = JarvisTextSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete command",
                    tint = JarvisSchoolRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(17.dp)
                )
            }

            Switch(
                checked = command.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF001A24),
                    checkedTrackColor = JarvisCyan,
                    uncheckedThumbColor = JarvisTextMuted,
                    uncheckedTrackColor = Color(0xFF141F33),
                    uncheckedBorderColor = JarvisNavyCardBorder
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandEditDialog(
    command: CustomCommand?,
    installedApps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onSave: (trigger: String, action: CommandActionType, targetPackage: String?, targetAppName: String?, response: String?) -> Unit
) {
    var triggerPhrase by remember { mutableStateOf(command?.triggerPhrase ?: "") }
    var selectedAction by remember {
        mutableStateOf(
            command?.let {
                try {
                    CommandActionType.valueOf(it.actionType)
                } catch (e: Exception) {
                    CommandActionType.OPEN_APP
                }
            } ?: CommandActionType.OPEN_APP
        )
    }

    var selectedApp by remember {
        mutableStateOf(
            installedApps.find { it.packageName == command?.targetPackage }
                ?: installedApps.firstOrNull()
        )
    }

    var customResponse by remember { mutableStateOf(command?.responseText ?: "") }
    var actionDropdownExpanded by remember { mutableStateOf(false) }
    var appDropdownExpanded by remember { mutableStateOf(false) }
    var appFilterText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (command == null) "Add Custom Command" else "Edit Command",
                color = JarvisTextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Trigger phrase input
                OutlinedTextField(
                    value = triggerPhrase,
                    onValueChange = { triggerPhrase = it },
                    label = { Text("Spoken Trigger Phrase") },
                    placeholder = { Text("e.g. start gaming, open my browser") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisNavyCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("command_trigger_input")
                )

                // Action Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = actionDropdownExpanded,
                    onExpandedChange = { actionDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAction.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Action") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyan,
                            unfocusedBorderColor = JarvisNavyCardBorder,
                            focusedTextColor = JarvisTextPrimary,
                            unfocusedTextColor = JarvisTextPrimary
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = actionDropdownExpanded,
                        onDismissRequest = { actionDropdownExpanded = false },
                        modifier = Modifier.background(JarvisNavyCard)
                    ) {
                        CommandActionType.values().forEach { action ->
                            DropdownMenuItem(
                                text = { Text(action.displayName, color = JarvisTextPrimary) },
                                onClick = {
                                    selectedAction = action
                                    actionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // App Picker if OPEN_APP
                if (selectedAction == CommandActionType.OPEN_APP) {
                    ExposedDropdownMenuBox(
                        expanded = appDropdownExpanded,
                        onExpandedChange = { appDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedApp?.label ?: "Select Installed App",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Application") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = appDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyan,
                                unfocusedBorderColor = JarvisNavyCardBorder,
                                focusedTextColor = JarvisTextPrimary,
                                unfocusedTextColor = JarvisTextPrimary
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("select_app_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = appDropdownExpanded,
                            onDismissRequest = { appDropdownExpanded = false },
                            modifier = Modifier.background(JarvisNavyCard).height(240.dp)
                        ) {
                            if (installedApps.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No apps detected", color = JarvisTextMuted) },
                                    onClick = { appDropdownExpanded = false }
                                )
                            } else {
                                installedApps.forEach { app ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(app.label, color = JarvisTextPrimary, fontSize = 13.sp)
                                                Text(app.packageName, color = JarvisTextMuted, fontSize = 10.sp)
                                            }
                                        },
                                        onClick = {
                                            selectedApp = app
                                            appDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Spoken feedback
                OutlinedTextField(
                    value = customResponse,
                    onValueChange = { customResponse = it },
                    label = { Text("Spoken Voice Response (Optional)") },
                    placeholder = { Text("e.g. Starting your gaming session.") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisNavyCardBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (triggerPhrase.isNotBlank()) {
                        onSave(
                            triggerPhrase,
                            selectedAction,
                            if (selectedAction == CommandActionType.OPEN_APP) selectedApp?.packageName else null,
                            if (selectedAction == CommandActionType.OPEN_APP) selectedApp?.label else null,
                            customResponse.ifBlank { null }
                        )
                    }
                },
                enabled = triggerPhrase.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                modifier = Modifier.testTag("save_command_button")
            ) {
                Text("Save Command", color = Color(0xFF001A24), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = JarvisTextSecondary)
            }
        },
        containerColor = JarvisNavyCard
    )
}
