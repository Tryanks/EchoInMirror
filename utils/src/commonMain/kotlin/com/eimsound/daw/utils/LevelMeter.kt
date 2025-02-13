@file:Suppress("unused")

package com.eimsound.daw.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

@JvmInline
value class Level(val value: Float = 0F) {
    override fun toString() = toDecibel().let { if (it.isInfinite()) "-inf" else if (it > -0.01 && it < 0.01) "0.00" else "%.2f".format(it) }
    @Suppress("MemberVisibilityCanBePrivate")
    fun toDecibel() = 20 * log10(value)
    fun toPercentage(maxDB: Float = 0F) = mapValue(toDecibel(), -60f, maxDB)
    fun toDisplayPercentage() = (sqrt(value) / 1.4f).coerceIn(0f, 1f)
    fun update(other: Float) = Level(if (other > value) other else if (value > 0.001F) (value * 0.8F).coerceAtLeast(other) else 0F)
    @ExperimentalEIMApi
    fun getLevelColor(defaultColor: Color, warningColor: Color, errorColor: Color) =
        if (value > 1) errorColor else if (value > 0.51187F) warningColor else defaultColor

    operator fun plus(other: Level) = Level(value + other.value)
    operator fun minus(other: Level) = Level(value - other.value)
    operator fun times(other: Level) = Level(value * other.value)
    operator fun div(other: Level) = Level(value / other.value)
    operator fun compareTo(other: Level) = value.compareTo(other.value)
    operator fun unaryMinus() = Level(-value)
    operator fun unaryPlus() = this
    operator fun inc() = Level(value + 1)
    operator fun dec() = Level(value - 1)
    operator fun rem(other: Level) = Level(value % other.value)
    operator fun rangeTo(other: Level) = value..other.value
    operator fun contains(other: Level) = value in this..other
    operator fun not() = Level(1 - value)

    operator fun plus(other: Float) = Level(value + other)
    operator fun minus(other: Float) = Level(value - other)
    operator fun times(other: Float) = Level(value * other)
    operator fun div(other: Float) = Level(value / other)
    operator fun compareTo(other: Float) = value.compareTo(other)
    operator fun rem(other: Float) = Level(value % other)
    operator fun rangeTo(other: Float) = value..other
    operator fun contains(other: Float) = value in this..other

    fun isZero() = value < 0.0001F
    fun isNotZero() = value > 0.0001F

    fun coerceAtLeast(minimumValue: Level) = Level(value.coerceAtLeast(minimumValue.value))
    fun coerceAtMost(maximumValue: Level) = Level(value.coerceAtMost(maximumValue.value))
    fun coerceIn(minimumValue: Level, maximumValue: Level) = Level(value.coerceIn(minimumValue.value, maximumValue.value))
}

interface LevelMeter {
    var left: Level
    var right: Level
    val center get() = (left + right) / 2F
    val side get() = left - right
    val maxLevel get() = left.coerceAtLeast(right)
    val cachedMaxLevelString: String
    fun reset()
}

class LevelMeterImpl : LevelMeter {
    override var left by mutableStateOf(Level(0F))
    override var right by mutableStateOf(Level(0F))
    override var cachedMaxLevelString by mutableStateOf("-inf")
    override fun reset() {
        left = Level()
        right = Level()
        cachedMaxLevelString = "-inf"
    }
}

fun decibelToVoltage(db: Float) = 10.0F.pow(db / 20.0F)
