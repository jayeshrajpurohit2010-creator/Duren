package com.duren.app.data.tribe

import java.util.Calendar

/**
 * Question of the Night (Feature 22) — a daily prompt that rotates the same way for
 * everyone, every night, with no Cloud Function. The Blaze version auto-posts it at
 * midnight per tribe timezone; this free version simply shows tonight's prompt at the
 * top of the tribe so people have something to gather around.
 *
 * [forToday] is deterministic: the day of the year selects the prompt, so the whole
 * tribe sees the same question and it changes once a day on its own.
 */
object NightQuestions {

    fun forToday(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return PROMPTS[dayOfYear % PROMPTS.size]
    }

    private val PROMPTS = listOf(
        "What kept you up tonight?",
        "What are you quietly proud of this week?",
        "What song is stuck in your head right now?",
        "What's one thing you'd tell your 3AM self?",
        "What are you avoiding by being here?",
        "What small thing went right today?",
        "Who are you thinking about tonight?",
        "What would you do with one more hour awake?",
        "What's the last thing that made you laugh?",
        "What are you grateful the dark hides?",
        "What's a comfort you keep coming back to?",
        "If tonight had a colour, what would it be?",
        "What's something you've never said out loud here?",
        "What are you hungry for that isn't food?",
        "What did you let go of today?",
        "What's the smallest win you'll take?",
        "What's playing in the background of your life lately?",
        "What would make tomorrow lighter?",
        "What's a question you wish someone would ask you?",
        "What are you carrying that isn't yours?",
        "What does 'rest' look like for you?",
        "What's the most alive you've felt this month?",
        "What are you pretending not to know?",
        "What would you say to whoever's awake with you?",
        "What's a small ritual that keeps you sane?",
        "What's something you're slowly getting better at?",
        "What's the bravest thing you did this week, however tiny?",
        "What's a memory tonight reminds you of?",
        "What do you wish were easier?",
        "What are you looking forward to, even a little?",
        "What's the last kind thing someone did for you?",
        "What's a thought you keep circling back to?",
        "What would you tell a stranger who couldn't sleep?",
        "What's something beautiful you noticed today?",
        "What are you ready to begin?",
        "What's been heavier than it should be lately?",
        "What's a place you'd disappear to right now?",
        "What's one truth you're sitting with tonight?",
        "What do you need more of?",
        "What did today teach you?",
        "What's a sound that feels like home?",
        "What are you forgiving yourself for?",
        "What's worth staying up for?",
        "What's changed in you this year?",
        "What's the last thing you wrote and deleted?",
        "What are you hoping no one asks — and what if they did?",
        "What's keeping the fire going for you?",
        "What would 'enough' feel like tonight?",
        "What's a door you're standing in front of?",
        "What do you want to remember about right now?"
    )
}
