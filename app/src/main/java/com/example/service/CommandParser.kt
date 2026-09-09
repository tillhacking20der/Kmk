package com.example.service

import java.util.Locale

/**
 * Standardized command parser for JARVIS according to requirements:
 * 1. Convert speech to text.
 * 2. Remove the wake word "Jarvis" from the beginning.
 * 3. Normalize the command (lowercase, trim spaces, handle punctuation).
 * 4. Match the command against built-in commands and Custom Commands.
 */
object CommandParser {

    data class ParseResult(
        val hasWakeWord: Boolean,
        val isWakeWordOnly: Boolean,
        val normalizedCommand: String,
        val originalText: String
    )

    /**
     * Parses raw recognized speech:
     * - Detects wake word "Jarvis"
     * - Strips the wake word from the beginning
     * - Normalizes the command: lowercase, trims, removes punctuation, collapses whitespace
     */
    fun parse(rawSpeech: String): ParseResult {
        val trimmed = rawSpeech.trim()
        if (trimmed.isEmpty()) {
            return ParseResult(
                hasWakeWord = false,
                isWakeWordOnly = false,
                normalizedCommand = "",
                originalText = rawSpeech
            )
        }

        val lower = trimmed.lowercase(Locale.ROOT)
        val hasWakeWord = lower.contains("jarvis")

        // Remove wake word "Jarvis" (and optional conversational prefixes like "hey", "ok", "okay") from the beginning
        var textAfterWakeWord = lower
        if (hasWakeWord) {
            // Regex to remove leading wake word phrases like:
            // "hey jarvis", "ok jarvis", "okay jarvis", "jarvis"
            val wakePrefixRegex = Regex("^(?:hey\\s+|ok\\s+|okay\\s+)?jarvis\\b[\\s,:;!?-]*", RegexOption.IGNORE_CASE)
            val match = wakePrefixRegex.find(lower)
            textAfterWakeWord = if (match != null) {
                lower.substring(match.range.last + 1)
            } else {
                // If wake word was somewhere in the middle, extract what comes after the first "jarvis"
                val idx = lower.indexOf("jarvis")
                if (idx >= 0) {
                    lower.substring(idx + "jarvis".length)
                } else {
                    lower
                }
            }
        }

        // Normalize command:
        var normalized = normalize(textAfterWakeWord)
        // Strip polite conversational prefixes if present (e.g. "could you please open youtube")
        var changed = true
        while (changed) {
            val previous = normalized
            normalized = normalized
                .removePrefix("please ")
                .removePrefix("could you ")
                .removePrefix("can you ")
                .removePrefix("would you ")
                .removePrefix("will you ")
                .trim()
            changed = (normalized != previous)
        }

        val isWakeWordOnly = hasWakeWord && normalized.isEmpty()

        return ParseResult(
            hasWakeWord = hasWakeWord,
            isWakeWordOnly = isWakeWordOnly,
            normalizedCommand = normalized,
            originalText = rawSpeech
        )
    }

    /**
     * Normalizes text: lowercase, remove punctuation, collapse whitespace, trim.
     */
    fun normalize(text: String): String {
        return text.lowercase(Locale.ROOT)
            // Replace punctuation with space so "open, youtube." or "open youtube!" becomes clean
            .replace(Regex("[.,!?:;\"'()\\-_/\\[\\]{}]"), " ")
            // Collapse multiple whitespace characters into a single space
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Extracts target app query from normalized command (e.g. "open youtube" -> "youtube")
     */
    fun extractAppQuery(normalizedCommand: String): String? {
        val prefixes = listOf(
            "open up the", "open the", "open up", "open",
            "launch the", "launch",
            "start the", "start",
            "run the", "run",
            "play the", "play"
        )

        for (prefix in prefixes) {
            if (normalizedCommand == prefix) return ""
            if (normalizedCommand.startsWith("$prefix ")) {
                var query = normalizedCommand.substring(prefix.length).trim()
                // Clean trailing app indicators, e.g. "youtube app" -> "youtube"
                query = query.removeSuffix(" application")
                    .removeSuffix(" app")
                    .trim()
                if (query.startsWith("the ")) {
                    query = query.removePrefix("the ").trim()
                }
                return query
            }
        }
        return null
    }
}
