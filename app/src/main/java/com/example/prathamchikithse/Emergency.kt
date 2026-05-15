package com.example.prathamchikithse

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency")

data class Emergency(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val steps: String,
    val dos: String,
    val donts: String,

)