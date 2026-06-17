package com.duren.app.data.tribe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Question of the Night rotates the same prompt for everyone, every night, with no
 * server — the day of the year picks it. These tests pin that contract: the prompt is
 * stable within a day (two reads in a row agree), it always lands inside the prompt list
 * (the `% size` can never index out of bounds), and the prompt set itself is healthy —
 * non-blank and with no accidental duplicates that would make a day silently repeat its
 * neighbour. They deliberately do not assert *which* prompt shows today, since that moves
 * with the calendar; they pin the determinism the feature promises.
 */
class NightQuestionsTest {

    @Test
    fun `today's prompt is stable across repeated reads`() {
        // Same calendar day -> same prompt. (Identical string instances, since they come
        // straight out of the constant list.)
        val first = NightQuestions.forToday()
        val second = NightQuestions.forToday()
        assertSame(first, second)
    }

    @Test
    fun `today's prompt is one of the defined prompts and never blank`() {
        val prompt = NightQuestions.forToday()
        assertTrue("prompt should not be blank", prompt.isNotBlank())
        assertTrue("prompt should come from the curated list", prompt in allPrompts())
    }

    @Test
    fun `the day-of-year index can never fall off either end of the list`() {
        // Calendar.DAY_OF_YEAR runs 1..366 (leap years). Reproduce the exact selection
        // math the object uses and prove every possible day maps to a real entry.
        val prompts = allPrompts()
        for (dayOfYear in 1..366) {
            val idx = dayOfYear % prompts.size
            assertTrue("index $idx for day $dayOfYear out of range", idx in prompts.indices)
        }
    }

    @Test
    fun `every prompt is non-blank`() {
        allPrompts().forEachIndexed { i, p ->
            assertTrue("prompt #$i is blank", p.isNotBlank())
        }
    }

    @Test
    fun `prompts are unique so no two days share a question by accident`() {
        val prompts = allPrompts()
        assertEquals(prompts.size, prompts.toSet().size)
    }

    /**
     * Pull the private PROMPTS list back out via reflection so the tests above can range
     * over it without us copying the strings (which would just restate the source). This
     * reads the production constant, it never mutates it.
     */
    @Suppress("UNCHECKED_CAST")
    private fun allPrompts(): List<String> {
        val field = NightQuestions::class.java.getDeclaredField("PROMPTS")
        field.isAccessible = true
        return field.get(NightQuestions) as List<String>
    }
}
