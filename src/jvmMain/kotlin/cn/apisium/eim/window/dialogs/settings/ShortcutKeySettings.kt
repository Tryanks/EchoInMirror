package cn.apisium.eim.window.dialogs.settings

import androidx.compose.foundation.layout.*
import cn.apisium.eim.components.Tab
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Command
import cn.apisium.eim.components.ReadonlyTextField
import cn.apisium.eim.impl.CommandManagerImpl
import cn.apisium.eim.utils.clickableWithIcon
import cn.apisium.eim.utils.mutableStateSetOf
import java.awt.event.KeyEvent

internal object ShortcutKeySettings : Tab {
    @Composable
    override fun label() {
        Text("快捷键")
    }

    @Composable
    override fun icon() {
        Icon(Icons.Filled.Keyboard, "Shortcut")
    }

    @Composable
    @OptIn(ExperimentalComposeUiApi::class)
    override fun content() {
        var selectKey by remember { mutableStateOf("") }
        Column {
            val commandManager = EchoInMirror.commandManager as CommandManagerImpl
            val commands = mutableMapOf<Command, String>()

            commandManager.commands.forEach { (key, command) ->
                commands[command] = key
            }
            commandManager.customCommand.forEach { (key, commandName) ->
                commands[commandManager.commandMap[commandName]!!] = key
            }

            commands.forEach { (command, key) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(command.displayName, Modifier.weight(1f))
                    var curKey by remember { mutableStateOf(key) }
                    val keyCompose = remember { mutableStateSetOf<String>().apply { addAll(key.split(" ")) } }
                    val preKeyCompose = remember { mutableStateListOf<String>() }

                    fun cancel() {
                        keyCompose.clear()
                        selectKey = ""
                        preKeyCompose.forEach {
                            keyCompose.add(it)
                        }
                    }

                    fun done() {
                        selectKey = ""
                        val keyStr = keyCompose.joinToString(separator = " ")
                        if (keyStr in commandManager.commands || keyStr in commandManager.customCommand) {
                            cancel()
                            return
                        }
                        if (curKey !in commandManager.commands && curKey !in commandManager.customCommand) {
                            cancel()
                            return
                        }
                        val commandTar: String
                        if (curKey in commandManager.customCommand) {
                            commandTar = commandManager.customCommand[curKey]!!
                            commandManager.customCommand.minus(curKey)
                        } else {
                            commandTar = commandManager.commands[curKey]!!.name
                        }
                        commandManager.customCommand[keyStr] = commandTar
                        curKey = keyStr
                        commandManager.saveCustomShortcutKeys()
                    }

                    ReadonlyTextField(Modifier.clickableWithIcon {
                        selectKey = curKey
                        preKeyCompose.clear()
                        keyCompose.forEach {
                            preKeyCompose.add(it)
                        }
                        keyCompose.clear()
                    }.onFocusChanged {
                        if (!it.isFocused && selectKey == curKey) done()
                    }.onKeyEvent {
                        if (it.type != KeyEventType.KeyDown || selectKey != curKey) return@onKeyEvent false

                        if (it.key == Key.Escape) cancel()
                        else keyCompose.add(it.key.keyCode.toString())
                        true
                    }) {
                        Text(keyCompose.joinToString(separator = "+") {
                            KeyEvent.getKeyText(Key(it.toLong()).nativeKeyCode)
                        })
                    }
                    Spacer(Modifier.weight(0.5f))
                }
                Spacer(Modifier.height(10.dp))
            }

        }
    }
}