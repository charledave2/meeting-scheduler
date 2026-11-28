package com.example.meetingscheduler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingPartRulesTest {
    @Test fun recognizesTalkTitlesFromCapturesAndSavedSchedules() {
        listOf("Pahayag", "6. Pahayag", " 6) PAHAYAG: Paksa", "Talk", "6. Talk (5 min.)").forEach {
            assertTrue(it, isSoloTalk(it))
        }
    }

    @Test fun preservesAssistantRolesForDemonstrationsAndBibleStudy() {
        listOf("Pakikipag-usap Muli", "Pagpapasimula ng Pakikipag-usap", "Pag-aaral ng Kongregasyon sa Bibliya", "Pagpapahayag ng Mabuting Balita").forEach {
            assertFalse(it, isSoloTalk(it))
        }
    }
}
