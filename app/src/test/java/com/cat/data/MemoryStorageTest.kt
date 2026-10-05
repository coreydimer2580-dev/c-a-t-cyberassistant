package com.cat.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryStorageTest {
    @Test
    fun formatsMegabytesAndGigabytesFromRealByteCounts() {
        assertEquals("12 MB", MemoryStorage.formatBytes(12L * 1024L * 1024L))
        assertEquals("1.5 GB", MemoryStorage.formatBytes((1.5 * 1024.0 * 1024.0 * 1024.0).toLong()))
        assertEquals("0.5 MB", MemoryStorage.formatBytes(512L * 1024L))
    }

    @Test
    fun labelIsSoftTargetNotAFakeQuota() {
        val snap = MemoryStorage.Snapshot(usedBytes = 12L * 1024L * 1024L, freeBytes = 8L * 1024L * 1024L * 1024L)
        assertEquals(
            "Memory: 12 MB used · 100000 soft note target · limited by device storage",
            snap.label
        )
        assertFalse(Regex("\\b10000\\b").containsMatchIn(snap.label))
        assertFalse(snap.label.contains("GB quota"))
        assertNull(snap.warning)
        assertFalse(snap.lowSpace)
    }

    @Test
    fun warnsBelowTwoHundredMegabytesFree() {
        val snap = MemoryStorage.Snapshot(usedBytes = 1024L * 1024L, freeBytes = MemoryStorage.LOW_FREE_BYTES - 1)
        assertTrue(snap.lowSpace)
        assertTrue(snap.warning!!.contains("Low space"))
    }

    @Test
    fun databaseSizeIncludesWalAndShm() {
        val dir = createTempDir(prefix = "cat-mem")
        val db = File(dir, "cat-memory.db")
        db.writeBytes(ByteArray(100))
        File(db.path + "-wal").writeBytes(ByteArray(50))
        File(db.path + "-shm").writeBytes(ByteArray(25))
        assertEquals(175L, MemoryStorage.bytesOf(db))
        dir.deleteRecursively()
    }
}
