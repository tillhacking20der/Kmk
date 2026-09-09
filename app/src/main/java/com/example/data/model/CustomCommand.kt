package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommandActionType(val displayName: String) {
    OPEN_APP("Open Application"),
    GO_HOME("Go to Home Screen"),
    ACTIVATE_SCHOOL_MODE("Activate School Mode"),
    DEACTIVATE_SCHOOL_MODE("Deactivate School Mode"),
    SPEAK_TEXT("Voice Feedback Only")
}

@Entity(tableName = "custom_commands")
data class CustomCommand(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val triggerPhrase: String,
    val actionType: String = CommandActionType.OPEN_APP.name,
    val targetPackage: String? = null,
    val targetAppName: String? = null,
    val responseText: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
