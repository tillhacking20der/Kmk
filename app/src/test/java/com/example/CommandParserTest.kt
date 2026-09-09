package com.example

import com.example.service.CommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    @Test
    fun testOpenYouTube() {
        val result = CommandParser.parse("Jarvis, open YouTube")
        assertTrue(result.hasWakeWord)
        assertFalse(result.isWakeWordOnly)
        assertEquals("open youtube", result.normalizedCommand)
        assertEquals("youtube", CommandParser.extractAppQuery(result.normalizedCommand))
    }

    @Test
    fun testOpenGoogle() {
        val result = CommandParser.parse("Jarvis, open Google")
        assertTrue(result.hasWakeWord)
        assertFalse(result.isWakeWordOnly)
        assertEquals("open google", result.normalizedCommand)
        assertEquals("google", CommandParser.extractAppQuery(result.normalizedCommand))
    }

    @Test
    fun testOpenMinecraft() {
        val result = CommandParser.parse("Jarvis, open Minecraft")
        assertTrue(result.hasWakeWord)
        assertFalse(result.isWakeWordOnly)
        assertEquals("open minecraft", result.normalizedCommand)
        assertEquals("minecraft", CommandParser.extractAppQuery(result.normalizedCommand))
    }

    @Test
    fun testOpenDiscord() {
        val result = CommandParser.parse("Jarvis, open Discord")
        assertTrue(result.hasWakeWord)
        assertFalse(result.isWakeWordOnly)
        assertEquals("open discord", result.normalizedCommand)
        assertEquals("discord", CommandParser.extractAppQuery(result.normalizedCommand))
    }

    @Test
    fun testActivateSchoolMode() {
        val result = CommandParser.parse("Jarvis, activate School Mode")
        assertTrue(result.hasWakeWord)
        assertEquals("activate school mode", result.normalizedCommand)
    }

    @Test
    fun testDeactivateSchoolMode() {
        val result = CommandParser.parse("Jarvis, deactivate School Mode")
        assertTrue(result.hasWakeWord)
        assertEquals("deactivate school mode", result.normalizedCommand)
    }

    @Test
    fun testStopListening() {
        val result = CommandParser.parse("Jarvis, stop listening")
        assertTrue(result.hasWakeWord)
        assertEquals("stop listening", result.normalizedCommand)
    }

    @Test
    fun testGoHome() {
        val result = CommandParser.parse("Jarvis, go home")
        assertTrue(result.hasWakeWord)
        assertEquals("go home", result.normalizedCommand)
    }

    @Test
    fun testWakeWordOnly() {
        val result = CommandParser.parse("Jarvis")
        assertTrue(result.hasWakeWord)
        assertTrue(result.isWakeWordOnly)
        assertEquals("", result.normalizedCommand)
    }

    @Test
    fun testPunctuationAndSpacing() {
        val result = CommandParser.parse("  Hey Jarvis,  could you please open YouTube!??  ")
        assertTrue(result.hasWakeWord)
        assertEquals("open youtube", result.normalizedCommand)
        assertEquals("youtube", CommandParser.extractAppQuery(result.normalizedCommand))
    }

    @Test
    fun testConversationalQueries() {
        val timeResult = CommandParser.parse("Jarvis, what time is it?")
        assertTrue(timeResult.hasWakeWord)
        assertEquals("what time is it", timeResult.normalizedCommand)

        val howAreYouResult = CommandParser.parse("Jarvis, how are you?")
        assertTrue(howAreYouResult.hasWakeWord)
        assertEquals("how are you", howAreYouResult.normalizedCommand)

        val explainResult = CommandParser.parse("Jarvis, explain black holes to me.")
        assertTrue(explainResult.hasWakeWord)
        assertEquals("explain black holes to me", explainResult.normalizedCommand)

        val memoryResult = CommandParser.parse("Jarvis, what did I just ask you?")
        assertTrue(memoryResult.hasWakeWord)
        assertEquals("what did i just ask you", memoryResult.normalizedCommand)
    }
}
