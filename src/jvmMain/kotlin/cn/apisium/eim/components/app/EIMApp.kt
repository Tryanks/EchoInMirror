@file:Suppress("INVISIBLE_SETTER")

package cn.apisium.eim.components.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.components.*
import cn.apisium.eim.components.splitpane.ExperimentalSplitPaneApi
import cn.apisium.eim.components.splitpane.HorizontalSplitPane
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.impl.CommandManagerImpl
import cn.apisium.eim.impl.WindowManagerImpl
import org.jetbrains.skiko.Cursor

@Composable
fun checkSampleRateAndBufferSize() {
    LaunchedEffect(Unit) {
        snapshotFlow {
            EchoInMirror.sampleRate
            EchoInMirror.bufferSize
        }
            .collect {
                println("Changed: ${EchoInMirror.sampleRate} ${EchoInMirror.bufferSize}")
            }
    }
}
@Suppress("unused")
private fun Modifier.cursorForHorizontalResize() = pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSplitPaneApi::class, ExperimentalComposeUiApi::class)
fun eimApp() {
    application {
        val icon = painterResource("logo.png")
        checkSampleRateAndBufferSize()
        MaterialTheme {
            Window(onCloseRequest = ::exitApplication, icon = icon, title = "Echo In Mirror", onKeyEvent = {
                if (it.type != KeyEventType.KeyUp) return@Window false
                var keys = it.key.keyCode.toString()
                if (it.isCtrlPressed) keys = "${Key.CtrlLeft.keyCode} $keys"
                if (it.isShiftPressed) keys = "${Key.ShiftLeft.keyCode} $keys"
                if (it.isAltPressed) keys = "${Key.AltLeft.keyCode} $keys"
                if (it.isMetaPressed) keys = "${Key.MetaLeft.keyCode} $keys"
                (EchoInMirror.commandManager as CommandManagerImpl).commands[keys]?.execute()
                false
            }) {
                Row {
                    sideBar()
                    Scaffold(
                        topBar = { eimAppBar() },
                        content = {
                            Column {
                                Box(Modifier.weight(1F)) {
                                    HorizontalSplitPane(splitPaneState = sideBarWidthState) {
                                        first(0.dp) { sideBarContent() }
                                        second(400.dp) {
                                            VerticalSplitPane(splitPaneState = bottomBarHeightState) {
                                                first(200.dp) { playList() }
                                                second(0.dp) { bottomBarContent() }
                                            }
                                        }
                                    }
                                }
                                statusBar()
                            }
                        }
                    )
                }

                if (EchoInMirror.windowManager is WindowManagerImpl) EchoInMirror.windowManager.dialogs()
            }
        }
    }
}