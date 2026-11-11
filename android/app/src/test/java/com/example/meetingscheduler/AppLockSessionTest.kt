package com.example.meetingscheduler

import org.junit.Assert.*
import org.junit.Test

class AppLockSessionTest {
    @Test fun coldStartAndBackgroundRequireNewAuthentication() {
        val session = AppLockSession(true)
        assertTrue(session.snapshot.locked)
        val unlock = session.begin(LockAction.UNLOCK)!!
        assertTrue(session.authenticated(unlock, foreground = true))
        assertFalse(session.snapshot.locked)
        session.background()
        assertTrue(session.snapshot.locked)
        assertTrue(AppLockSession(session.snapshot.enabled).snapshot.locked)
    }

    @Test fun cancelOrStaleCallbackCannotEnableDisableOrUnlock() {
        val session = AppLockSession(false)
        val enable = session.begin(LockAction.ENABLE)!!
        assertNull(session.begin(LockAction.ENABLE))
        session.failed(enable)
        assertFalse(session.authenticated(enable, foreground = true))
        assertFalse(session.snapshot.enabled)
        val confirmedEnable = session.begin(LockAction.ENABLE)!!
        session.authenticated(confirmedEnable, foreground = true)
        val disable = session.begin(LockAction.DISABLE)!!
        session.failed(disable)
        assertTrue(session.snapshot.enabled)
        session.background()
        val unlock = session.begin(LockAction.UNLOCK)!!
        session.failed(unlock)
        assertTrue(session.snapshot.locked)
        assertFalse(session.authenticated(unlock, foreground = true))
        assertTrue(session.snapshot.locked)
    }

    @Test fun lateSuccessInBackgroundDoesNotExposeWorkspace() {
        val session = AppLockSession(true)
        val unlock = session.begin(LockAction.UNLOCK)!!
        session.background()
        session.authenticated(unlock, foreground = false)
        assertTrue(session.snapshot.locked)
        assertNull(session.snapshot.attempt)
        val newAttempt = session.begin(LockAction.UNLOCK)!!
        assertFalse(session.authenticated(unlock, foreground = true))
        assertTrue(session.snapshot.locked)
        session.authenticated(newAttempt, foreground = true)
        assertFalse(session.snapshot.locked)
    }

    @Test fun enableAndDisableNeedSuccessfulConfirmation() {
        val session = AppLockSession(false)
        assertNull(session.begin(LockAction.UNLOCK))
        assertNull(session.begin(LockAction.DISABLE))
        val enable = session.begin(LockAction.ENABLE)!!
        assertFalse(session.snapshot.enabled)
        session.authenticated(enable, foreground = false)
        assertTrue(session.snapshot.enabled)
        assertTrue(session.snapshot.locked)
        val disable = session.begin(LockAction.DISABLE)!!
        session.authenticated(disable, foreground = true)
        assertFalse(session.snapshot.enabled)
        assertFalse(session.snapshot.locked)
    }
}
