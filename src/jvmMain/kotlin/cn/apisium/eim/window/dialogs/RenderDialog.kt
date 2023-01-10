package cn.apisium.eim.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.impl.RendererImpl
import java.awt.Dimension
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import cn.apisium.eim.api.RenderFormat
import cn.apisium.eim.api.convertPPQToSamples
import cn.apisium.eim.api.processor.ChannelType
import cn.apisium.eim.components.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[ExportDialog] = false
}

private fun formateSecondTime(timeInSecond: Float): String {
    return "${(timeInSecond / 60).toInt().toString().padStart(2, '0')}:${
        (timeInSecond % 60).toInt().toString().padStart(2, '0')
    }:${((timeInSecond * 1000) % 1000).toInt().toString().padStart(3, '0')}"
}

private val floatingDialogProvider = FloatingDialogProvider()

@OptIn(DelicateCoroutinesApi::class)
val ExportDialog = @Composable {
    Dialog(::closeQuickLoadWindow, title = "导出") {
        remember { EchoInMirror.currentPosition.isPlaying = false }
        window.minimumSize = Dimension(300, 500)
        CompositionLocalProvider(LocalFloatingDialogProvider.provides(floatingDialogProvider)) {
            Surface(Modifier.fillMaxSize(), tonalElevation = 4.dp) {
                Box(Modifier.padding(10.dp)) {
                    Column {
                        val position = EchoInMirror.currentPosition
                        val endPPQ = position.projectRange.last - position.projectRange.first
                        val timeInSecond = position.convertPPQToSamples(endPPQ).toFloat() / position.sampleRate
                        Row {
                            Text("长度: ")
                            Text(
                                "${endPPQ / position.ppq / position.timeSigNumerator + 1}节",
                                color = Color.Black.copy(0.5f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "总时间: "
                            )
                            Text(
                                "${
                                    (timeInSecond / 60).toInt().toString().padStart(2, '0')
                                }'${(timeInSecond % 60).toInt().toString().padStart(2, '0')}\"",
                                color = Color.Black.copy(0.5f)
                            )
                            Spacer(Modifier.width(10.dp))
                        }

                        Spacer(Modifier.height(10.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))
                        var renderFormat by remember { mutableStateOf(RenderFormat.WAV) }
                        Row {
                            Row(
                                Modifier.weight(1F),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("导出格式: ")
                                Menu({ close ->
                                    RenderFormat.values().forEach {
                                        MenuItem(renderFormat == it, {
                                            close()
                                            renderFormat = it
                                        }, modifier = Modifier.fillMaxWidth()) { Text(it.extend) }
                                    }
                                }) {
                                    Text(
                                        renderFormat.extend
                                    )
                                }
                            }

                            Spacer(Modifier.width(10.dp))

                            var soundSelect by remember { mutableStateOf(ChannelType.STEREO) }
                            Row(Modifier.weight(1F)) {
                                Menu({ close ->
                                    Column {
                                        ChannelType.values().forEach {
                                            MenuItem(soundSelect == it, {
                                                close()
                                                soundSelect = it
                                            }, modifier = Modifier.fillMaxWidth()) {
                                                Text(it.name)
                                            }
                                        }
                                    }
                                }) {
                                    Text(soundSelect.name)
                                }
                            }
                        }

                        Spacer(Modifier.height(5.dp))

                        var bits by remember { mutableStateOf(16) }
                        if (renderFormat.isLossLess) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("位深: ", Modifier.width(80.dp))
                                arrayOf(16, 24, 32).map {
                                    RadioButton(bits == it, { bits = it })
                                    Text("${it}位")

                                }
                            }
                        }

                        var bitRate by remember { mutableStateOf(320) }
                        if (!renderFormat.isLossLess) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("比特率: ", Modifier.width(80.dp))
                                Menu({ close ->
                                    Column {
                                        arrayOf(128, 192, 256, 320).map {
                                            MenuItem(bitRate == it, {
                                                close()
                                                bitRate = it
                                            }, modifier = Modifier.fillMaxWidth()) {
                                                Text(it.toString())
                                            }
                                        }
                                    }
                                }) {
                                    Text("${bitRate}kbps")
                                }
                            }
                        }

                        var compressionLevel by remember { mutableStateOf(5) }
                        if (renderFormat.extend == "flac") {
                            Spacer(Modifier.width(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("flac压缩: ", Modifier.width(80.dp))
                                Slider(
                                    compressionLevel.toFloat(),
                                    { compressionLevel = it.toInt() },
                                    valueRange = 0f..8f,
                                    steps = 7,
                                    modifier = Modifier.weight(5f)
                                )
                                Text(" $compressionLevel", modifier = Modifier.weight(1f))
                            }
                        }

                        Filled()
                        var renderProcess by remember { mutableStateOf(0f) }
                        var isRendering by remember { mutableStateOf(false) }
                        var renderJob by remember { mutableStateOf<Job?>(null) }
                        Text(
                            "${(renderProcess * 100).toInt()}% ${formateSecondTime(timeInSecond * renderProcess)}  5.5倍快于实时",
                            Modifier.align(Alignment.CenterHorizontally)
                        )
                        if (isRendering) {
                            Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                LinearProgressIndicator(renderProcess, Modifier.fillMaxWidth())
                            }
                        }
                        Button({
                            if (isRendering) {
                                if(!renderJob!!.isCompleted)
                                    renderJob?.cancel()
                                isRendering = false
                            } else {
                                val renderer = EchoInMirror.bus?.let { RendererImpl(it) }
                                val curposition = EchoInMirror.currentPosition
                                val audioFile = File("./test.${renderFormat.extend}")
                                isRendering = true
                                EchoInMirror.player!!.close()
                                renderJob = GlobalScope.launch {
                                    renderer?.start(
                                        curposition.projectRange,
                                        curposition.sampleRate,
                                        curposition.ppq,
                                        curposition.bpm,
                                        audioFile,
                                        renderFormat,
                                        bits,
                                        bitRate,
                                        compressionLevel
                                    ) {
                                        renderProcess = it
//                                        if(renderProcess >= 1f)
//                                            isRendering = false
                                    }
                                }
                            }
                        }, Modifier.zIndex(-10f).fillMaxWidth()) {
                            Row {
                                if (isRendering && renderProcess < 1f) Text("取消")
                                else if(isRendering && renderProcess >= 1f) Text("确认")
                                else Text("导出到 test.${renderFormat.extend}")
                            }
                        }
                    }
                }
            }
        }
        floatingDialogProvider.FloatingDialogs()
    }
}
