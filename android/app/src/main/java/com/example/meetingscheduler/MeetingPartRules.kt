package com.example.meetingscheduler

private val soloTalkTitle = Regex("""^\s*(?:\d+[.)]\s*)?(?:pahayag|talk)(?=\s|$|[:—–-])""", RegexOption.IGNORE_CASE)

internal fun isSoloTalk(title: String): Boolean = soloTalkTitle.containsMatchIn(title)
