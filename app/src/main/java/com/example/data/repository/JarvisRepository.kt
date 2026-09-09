package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.local.CustomCommandDao
import com.example.data.model.CommandActionType
import com.example.data.model.CustomCommand
import com.example.data.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class JarvisRepository(
    private val context: Context,
    private val commandDao: CustomCommandDao
) {
    val allCommands: Flow<List<CustomCommand>> = commandDao.getAllCommands()
    val enabledCommands: Flow<List<CustomCommand>> = commandDao.getEnabledCommands()

    suspend fun getEnabledCommandsList(): List<CustomCommand> {
        return commandDao.getEnabledCommandsList()
    }

    suspend fun insertCommand(command: CustomCommand): Long {
        return commandDao.insert(command)
    }

    suspend fun updateCommand(command: CustomCommand) {
        commandDao.update(command)
    }

    suspend fun deleteCommand(command: CustomCommand) {
        commandDao.delete(command)
    }

    suspend fun deleteCommandById(id: Long) {
        commandDao.deleteById(id)
    }

    suspend fun seedInitialCommandsIfNeeded() = withContext(Dispatchers.IO) {
        if (commandDao.getCount() == 0) {
            val apps = getInstalledApps()
            // Find browser app
            val browserApp = apps.find {
                it.packageName.contains("chrome") ||
                        it.packageName.contains("browser") ||
                        it.label.contains("Chrome", ignoreCase = true) ||
                        it.label.contains("Browser", ignoreCase = true)
            }
            // Find youtube app
            val ytApp = apps.find {
                it.packageName.contains("youtube") ||
                        it.label.contains("YouTube", ignoreCase = true)
            }
            // Find game app (minecraft or others)
            val gameApp = apps.find {
                it.label.contains("Minecraft", ignoreCase = true) ||
                        it.packageName.contains("minecraft") ||
                        it.label.contains("Game", ignoreCase = true)
            }

            commandDao.insert(
                CustomCommand(
                    triggerPhrase = "start gaming",
                    actionType = CommandActionType.OPEN_APP.name,
                    targetPackage = gameApp?.packageName ?: "com.mojang.minecraftpe",
                    targetAppName = gameApp?.label ?: "Minecraft",
                    responseText = "Starting gaming session."
                )
            )

            commandDao.insert(
                CustomCommand(
                    triggerPhrase = "open my browser",
                    actionType = CommandActionType.OPEN_APP.name,
                    targetPackage = browserApp?.packageName ?: "com.android.chrome",
                    targetAppName = browserApp?.label ?: "Google Chrome",
                    responseText = "Opening browser."
                )
            )

            if (ytApp != null) {
                commandDao.insert(
                    CustomCommand(
                        triggerPhrase = "watch videos",
                        actionType = CommandActionType.OPEN_APP.name,
                        targetPackage = ytApp.packageName,
                        targetAppName = ytApp.label,
                        responseText = "Launching YouTube."
                    )
                )
            }
        }
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = mutableListOf<InstalledAppInfo>()
        val seenPackages = mutableSetOf<String>()

        // 1. Primary: query launcher intents
        try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            for (resolveInfo in resolveInfos) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg != context.packageName && seenPackages.add(pkg)) {
                    val label = resolveInfo.loadLabel(pm).toString()
                    val icon = resolveInfo.loadIcon(pm)
                    apps.add(InstalledAppInfo(label = label, packageName = pkg, icon = icon))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Secondary: check installed applications with launch intents for OEM/MIUI visibility
        try {
            val installedApplications = pm.getInstalledApplications(0)
            for (appInfo in installedApplications) {
                val pkg = appInfo.packageName
                if (pkg != context.packageName && seenPackages.add(pkg)) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        val label = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        apps.add(InstalledAppInfo(label = label, packageName = pkg, icon = icon))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        apps.sortedBy { it.label.lowercase() }
    }

    suspend fun findAppByName(query: String): InstalledAppInfo? = withContext(Dispatchers.IO) {
        val apps = getInstalledApps()
        val normalizedQuery = query.lowercase().trim()

        if (normalizedQuery.isBlank()) return@withContext null

        // 1. Exact label match
        apps.firstOrNull { it.label.equals(normalizedQuery, ignoreCase = true) }
            ?: // 2. Label starts with query (e.g. "Minecraft PE" for "minecraft")
            apps.firstOrNull { it.label.lowercase().startsWith(normalizedQuery) }
            ?: // 3. Label contains whole word or query
            apps.firstOrNull { it.label.lowercase().contains(normalizedQuery) }
            ?: // 4. Query contains label (e.g. "watch youtube videos" -> "YouTube")
            apps.firstOrNull { normalizedQuery.contains(it.label.lowercase()) }
            ?: // 5. Package name contains query (e.g. com.mojang.minecraftpe for "minecraft", com.discord for "discord")
            apps.firstOrNull { it.packageName.lowercase().contains(normalizedQuery) }
            ?: // 6. Fallback aliases mapping (YouTube, Minecraft, Discord, Chrome, etc.)
            findAppByAlias(normalizedQuery, apps)
            ?: // 7. Direct package check for known popular apps if installed
            findKnownPackageDirectly(normalizedQuery)
    }

    private fun findKnownPackageDirectly(query: String): InstalledAppInfo? {
        val pm = context.packageManager
        val knownMappings = mapOf(
            "youtube" to listOf("com.google.android.youtube", "com.google.android.youtube.tv"),
            "minecraft" to listOf("com.mojang.minecraftpe", "com.mojang.minecrafttrialpe"),
            "discord" to listOf("com.discord", "com.discord.canary"),
            "chrome" to listOf("com.android.chrome", "com.chrome.beta"),
            "spotify" to listOf("com.spotify.music"),
            "instagram" to listOf("com.instagram.android"),
            "whatsapp" to listOf("com.whatsapp"),
            "telegram" to listOf("org.telegram.messenger")
        )

        for ((alias, pkgs) in knownMappings) {
            if (query.contains(alias) || alias.contains(query)) {
                for (pkg in pkgs) {
                    try {
                        val launchIntent = pm.getLaunchIntentForPackage(pkg)
                        if (launchIntent != null) {
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val icon = pm.getApplicationIcon(appInfo)
                            return InstalledAppInfo(label = label, packageName = pkg, icon = icon)
                        }
                    } catch (e: Exception) {
                        // Package not installed
                    }
                }
            }
        }
        return null
    }

    private fun findAppByAlias(query: String, apps: List<InstalledAppInfo>): InstalledAppInfo? {
        return when {
            query.contains("youtube") -> {
                apps.firstOrNull {
                    it.packageName.contains("youtube") || it.label.contains("YouTube", ignoreCase = true)
                }
            }
            query.contains("minecraft") -> {
                apps.firstOrNull {
                    it.packageName.contains("minecraft") || it.label.contains("Minecraft", ignoreCase = true)
                }
            }
            query.contains("discord") -> {
                apps.firstOrNull {
                    it.packageName.contains("discord") || it.label.contains("Discord", ignoreCase = true)
                }
            }
            query.contains("browser") || query.contains("chrome") || query.contains("internet") -> {
                apps.firstOrNull {
                    it.packageName.contains("chrome") ||
                            it.packageName.contains("browser") ||
                            it.packageName.contains("firefox") ||
                            it.label.contains("Chrome", ignoreCase = true) ||
                            it.label.contains("Browser", ignoreCase = true)
                }
            }
            query.contains("google") -> {
                apps.firstOrNull {
                    it.packageName == "com.google.android.googlequicksearchbox" ||
                            it.label.equals("Google", ignoreCase = true) ||
                            it.packageName.contains("chrome")
                }
            }
            query.contains("music") || query.contains("spotify") -> {
                apps.firstOrNull {
                    it.packageName.contains("spotify") ||
                            it.packageName.contains("music") ||
                            it.label.contains("Music", ignoreCase = true) ||
                            it.label.contains("Spotify", ignoreCase = true)
                }
            }
            query.contains("camera") -> {
                apps.firstOrNull {
                    it.packageName.contains("camera") || it.label.contains("Camera", ignoreCase = true)
                }
            }
            query.contains("gallery") || query.contains("photos") -> {
                apps.firstOrNull {
                    it.packageName.contains("gallery") ||
                            it.packageName.contains("photos") ||
                            it.label.contains("Photos", ignoreCase = true) ||
                            it.label.contains("Gallery", ignoreCase = true)
                }
            }
            query.contains("settings") -> {
                apps.firstOrNull {
                    it.packageName.contains("settings") || it.label.contains("Settings", ignoreCase = true)
                }
            }
            query.contains("calculator") -> {
                apps.firstOrNull {
                    it.packageName.contains("calculator") || it.label.contains("Calculator", ignoreCase = true)
                }
            }
            else -> null
        }
    }
}
