@file:Suppress("unused")

package com.github.spotbugs.snom

import org.gradle.api.provider.Property

@JvmName("assignConfidence")
fun Property<Confidence>.assign(string: String) {
    set(Confidence.valueOf(string))
}

@JvmName("assignEffort")
fun Property<Effort>.assign(string: String) {
    set(Effort.valueOf(string))
}
