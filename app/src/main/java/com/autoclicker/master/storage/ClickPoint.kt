package com.autoclicker.master.storage

data class ClickPoint(
    val id: Int,
    var x: Int,
    var y: Int,
    var delayMs: Long = 1000,
    var durationMs: Long = 100,
    var isBack: Boolean = false
)