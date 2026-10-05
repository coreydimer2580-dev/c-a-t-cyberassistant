package com.cat.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-chosen label only. C@T never decides True, False, or Unsure.
 */
object TruthTag {
    const val TRUE = "True"
    const val FALSE = "False"
    const val UNSURE = "Unsure"
    val ALL = listOf(TRUE, FALSE, UNSURE)

    fun normalize(raw: String?): String = when (raw?.trim()?.lowercase()) {
        "true" -> TRUE
        "false" -> FALSE
        else -> UNSURE
    }
}

/**
 * A note kept in the on-device Room database (C@T hard save).
 * Nothing here expires. It stays until the user clears it.
 * [truthTag] is set by the user. Default is Unsure.
 */
@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val category: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "'Unsure'")
    val truthTag: String = TruthTag.UNSURE
)
