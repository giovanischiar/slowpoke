package io.schiar.slowpoke.view

import android.os.Bundle

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