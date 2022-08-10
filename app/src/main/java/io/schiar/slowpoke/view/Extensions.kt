package io.schiar.slowpoke.view

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue

fun Bundle?.println() {
    val extras = StringBuilder()
    if (this != null) {
        val keys = this.keySet()
        val it: Iterator<String> = keys.iterator()
        while (it.hasNext()) {
            val key = it.next()
            extras.append("$key = ${this[key]}\n")
        }
    }
    println(extras.toString())
}

fun Float.dp(displayMetrics: DisplayMetrics): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        displayMetrics
    )
}
