package com.cat.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupSolverTest {
    @Test
    fun pickGroupKeepsSelectedFirst() {
        val roster = GroupSolver.pickGroupPersonas(AiPersona.CODER)
        assertEquals(AiPersona.CODER, roster.first())
        assertEquals(3, roster.size)
    }

    @Test
    fun pickGroupForModeUsesAnalystCoderCoach() {
        val roster = GroupSolver.pickGroupPersonas(AiPersona.OFFLINE_CAT)
        assertEquals(listOf(AiPersona.ANALYST, AiPersona.CODER, AiPersona.COACH), roster)
    }

    @Test
    fun mergeIncludesVerdictAndPersonas() {
        val replies = listOf(
            GroupSolver.PersonaReply(AiPersona.ANALYST, "Break it into steps about tea."),
            GroupSolver.PersonaReply(AiPersona.CODER, "Write a short checklist for tea."),
            GroupSolver.PersonaReply(AiPersona.COACH, "Start with boiling water for tea.")
        )
        val merged = GroupSolver.merge("how do I make tea", replies)
        assertTrue(merged.contains("Analyst"))
        assertTrue(merged.contains("Coder"))
        assertTrue(merged.contains("Coach"))
        assertTrue(merged.contains("Group verdict"))
        assertTrue(merged.contains("tea"))
    }

    @Test
    fun personaFromIdDefaultsOffline() {
        assertEquals(AiPersona.OFFLINE_CAT, AiPersona.fromId(null))
        assertEquals(AiPersona.CREATIVE, AiPersona.fromId("creative"))
    }
}
