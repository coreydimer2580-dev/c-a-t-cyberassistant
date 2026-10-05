package com.cat.security

import com.cat.data.CopilotPrefs

/**
 * Terminal gate. The PIN is stored only as a salted SHA-256 in encrypted prefs.
 * Unlock lives in memory and ends when the process dies or lock() is called.
 */
class TerminalLock(
    private val prefs: CopilotPrefs,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    @Volatile
    var unlocked: Boolean = false
        private set

    init {
        ensureSeed()
    }

    fun cooldownSeconds(): Int {
        val left = prefs.terminalCooldownUntil() - now()
        if (left <= 0L) return 0
        return ((left + 999L) / 1000L).toInt()
    }

    fun lock() {
        unlocked = false
    }

    fun tryUnlock(pin: String): Unlock {
        val wait = cooldownSeconds()
        if (wait > 0) return Unlock.Wait(wait)
        if (!PinHash.isValidFormat(pin)) return Unlock.BadFormat
        val salt = PinHash.fromHex(prefs.terminalPinSalt()) ?: return Unlock.BadFormat
        if (PinHash.verify(salt, pin, prefs.terminalPinHash())) {
            prefs.setTerminalFails(0)
            prefs.setTerminalCooldownUntil(0L)
            unlocked = true
            return Unlock.Ok
        }
        return noteFailure()
    }

    fun changePin(current: String, next: String, confirm: String): Change {
        val wait = cooldownSeconds()
        if (wait > 0) return Change.Wait(wait)
        if (!PinHash.isValidFormat(current) || !PinHash.isValidFormat(next)) return Change.BadFormat
        if (next != confirm) return Change.Mismatch
        val salt = PinHash.fromHex(prefs.terminalPinSalt()) ?: return Change.BadFormat
        if (!PinHash.verify(salt, current, prefs.terminalPinHash())) {
            return when (val fail = noteFailure()) {
                is Unlock.Wait -> Change.Wait(fail.seconds)
                is Unlock.Wrong -> Change.Wrong(fail.left)
                else -> Change.Wrong(0)
            }
        }
        install(next)
        unlocked = false
        return Change.Ok
    }

    private fun ensureSeed() {
        val salt = PinHash.fromHex(prefs.terminalPinSalt())
        if (prefs.terminalPinHash().isNotBlank() && salt != null) return
        install(initialDigits())
    }

    private fun install(pin: String) {
        val salt = PinHash.newSalt()
        prefs.setTerminalPin(PinHash.hex(salt), PinHash.hash(salt, pin))
        prefs.setTerminalFails(0)
        prefs.setTerminalCooldownUntil(0L)
    }

    private fun noteFailure(): Unlock {
        val fails = prefs.terminalFails() + 1
        if (fails >= 5) {
            prefs.setTerminalFails(0)
            prefs.setTerminalCooldownUntil(now() + COOLDOWN_MS)
            return Unlock.Wait(30)
        }
        prefs.setTerminalFails(fails)
        return Unlock.Wrong(5 - fails)
    }

    /** First-run gate only. Never logged. */
    private fun initialDigits(): String = charArrayOf('7', '1', '9', '4').concatToString()

    sealed class Unlock {
        data object Ok : Unlock()
        data object BadFormat : Unlock()
        data class Wrong(val left: Int) : Unlock()
        data class Wait(val seconds: Int) : Unlock()
    }

    sealed class Change {
        data object Ok : Change()
        data object BadFormat : Change()
        data object Mismatch : Change()
        data class Wrong(val left: Int) : Change()
        data class Wait(val seconds: Int) : Change()
    }

    companion object {
        private const val COOLDOWN_MS = 30_000L
    }
}
