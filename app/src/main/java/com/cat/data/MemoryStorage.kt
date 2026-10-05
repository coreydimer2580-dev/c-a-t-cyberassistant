package com.cat.data

import android.content.Context
import android.os.StatFs
import java.io.File
import java.util.Locale

/**
 * Offline notes stay in Room with no count cap.
 * [SOFT_NOTE_TARGET] is a display target only. Storage is whatever the phone has free.
 */
object MemoryStorage {
    const val SOFT_NOTE_TARGET = 100_000
    const val LOW_FREE_BYTES = 200L * 1024L * 1024L
    const val DB_NAME = "cat-memory.db"

    data class Snapshot(
        val usedBytes: Long,
        val freeBytes: Long
    ) {
        val lowSpace: Boolean get() = freeBytes < LOW_FREE_BYTES

        val label: String
            get() = "Memory: ${formatBytes(usedBytes)} used · $SOFT_NOTE_TARGET soft note target · limited by device storage"

        val warning: String?
            get() = if (lowSpace) {
                "Low space: ${formatBytes(freeBytes)} free. Notes stay on this phone and are limited by device storage."
            } else {
                null
            }
    }

    fun probe(context: Context): Snapshot {
        return Snapshot(usedBytes = usedBytes(context), freeBytes = freeBytes(context))
    }

    fun usedBytes(context: Context): Long = bytesOf(context.getDatabasePath(DB_NAME))

    fun freeBytes(context: Context): Long {
        val dir = context.filesDir
        val stat = StatFs(dir.absolutePath)
        return stat.availableBytes
    }

    fun bytesOf(db: File): Long {
        return listOf(db, File(db.path + "-wal"), File(db.path + "-shm"))
            .sumOf { file -> if (file.isFile) file.length() else 0L }
    }

    fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        val gb = safe / (1024.0 * 1024.0 * 1024.0)
        if (gb >= 1.0) {
            return String.format(Locale.US, "%.1f GB", gb)
        }
        val mb = safe / (1024.0 * 1024.0)
        return if (mb >= 10.0) {
            String.format(Locale.US, "%.0f MB", mb)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }
}
