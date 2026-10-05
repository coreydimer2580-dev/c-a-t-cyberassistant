package com.cat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A line the user typed. Not discovered from Wi-Fi, Bluetooth, or radio.
 */
@Entity(tableName = "route_log")
data class RouteLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val fromPlace: String,
    val toPlace: String,
    val note: String,
    val createdAt: Long
)
