package cn.apisium.eim.impl.audiosources

import be.tarsos.dsp.resample.Resampler
import cn.apisium.eim.api.AudioSource
import cn.apisium.eim.api.ResampledAudioSource
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class DefaultResampledAudioSource(override val source: AudioSource, override var factor: Double = 1.0):
    ResampledAudioSource {
    override val name = "Resampled"
    override val channels get() = source.channels
    override val sampleRate get() = (source.sampleRate * factor).toFloat()
    override val length get() = (source.length * factor).toLong()
    private val resamplers = Array(channels) { Resampler(false, 0.1, 4.0) }
    private var nextStart = 0L

    override fun getSamples(start: Long, buffers: Array<FloatArray>): Int {
        if (factor == 1.0) {
            source.getSamples(start, buffers)
            return buffers[0].size
        }
        var sourceStart = (start / factor).roundToLong()
        if (nextStart - 1 == sourceStart) sourceStart = nextStart
        val sourceLength = (buffers[0].size / factor).roundToInt()
        nextStart = sourceStart + sourceLength
        val ch = channels.coerceAtMost(buffers.size)
        val sourceBuffers = Array(ch) { FloatArray(sourceLength) }
        source.getSamples(sourceStart, sourceBuffers)
        var consumed = 0
        for (i in 0 until ch) {
            consumed = resamplers[i].process(factor, sourceBuffers[i], 0, sourceLength, false,
                buffers[i], 0, buffers[i].size).outputSamplesGenerated
        }
        return consumed
    }
}