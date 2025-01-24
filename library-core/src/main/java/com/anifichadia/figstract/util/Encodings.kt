package com.anifichadia.figstract.util

@OptIn(ExperimentalStdlibApi::class)
fun Int.toHexString(length: Int = 8): String {
    val hexValue = this.toHexString(format = HexFormat.UpperCase).takeLast(length)
    return "0x$hexValue"
}

fun Float.toHexString(length: Int = 8): String {
    require(this in 0f..1f)
    return (this * 255f).toInt().toHexString(length)
}
