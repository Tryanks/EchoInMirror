package cn.apisium.eim.api

import cn.apisium.eim.api.processor.AudioProcessor

abstract class AudioPlayer(val currentPosition: CurrentPosition, var processor: AudioProcessor? = null): AutoCloseable {
    abstract fun open(sampleRate: Int, bufferSize: Int, bits: Int)
}
