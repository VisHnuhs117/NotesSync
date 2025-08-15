package com.vishnuhs.notessync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String,
    val color: String = "#6200EE", // Material color code
    val noteCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)