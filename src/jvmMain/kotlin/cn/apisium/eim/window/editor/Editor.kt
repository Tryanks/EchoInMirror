package cn.apisium.eim.window.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.window.Panel
import cn.apisium.eim.api.window.PanelDirection
import cn.apisium.eim.components.*
import cn.apisium.eim.components.splitpane.VerticalSplitPane
import cn.apisium.eim.components.splitpane.rememberSplitPaneState
import cn.apisium.eim.data.midi.NoteMessage
import cn.apisium.eim.data.midi.defaultNoteMessage
import cn.apisium.eim.data.midi.noteOff
import cn.apisium.eim.data.midi.noteOn
import cn.apisium.eim.utils.*
import kotlin.math.roundToInt

object EditorSingleton {
    val selectedNotes = mutableStateSetOf<NoteMessage>()
    var startNoteIndex = 0
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NotesEditorCanvas(
    noteHeight: Dp,
    noteWidth: Dp,
    verticalScrollState: ScrollState,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val highlightNoteColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03F)
    val outlineColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    EditorGrid(noteWidth, horizontalScrollState,
        modifier = modifier.scrollable(verticalScrollState, Orientation.Vertical, reverseDirection = true)
            .onPointerEvent(PointerEventType.Press) {
                EchoInMirror.selectedTrack?.let { track ->
                    val currentNote = KEYBOARD_KEYS - ((it.y + verticalScrollState.value) / noteHeight.value).toInt() - 1
                    val x = ((it.x + horizontalScrollState.value) / noteWidth.value).roundToInt()
                    for (i in EditorSingleton.startNoteIndex until track.notes.size) {
                        val note = track.notes[i]
                        if (note.note == currentNote && note.time <= x && note.time + note.duration >= x) {
                            EditorSingleton.selectedNotes.add(note)
                            return@let
                        }
                    }
                    val note = defaultNoteMessage(currentNote, x, 96)
                    val time = System.nanoTime()
                    track.notes.add(note)
                    track.notes.sort()
                    track.notes.update()
                    println((System.nanoTime() - time) / 1e6)
                }
            }
    ) {
        val verticalScrollValue = verticalScrollState.value
        val horizontalScrollValue = horizontalScrollState.value
        val noteHeightPx = noteHeight.toPx()
        val noteWidthPx = noteWidth.toPx()

        for (i in (verticalScrollValue / noteHeightPx).toInt()..((verticalScrollValue + size.height) / noteHeightPx).toInt()) {
            val y = i * noteHeightPx - verticalScrollValue
            if (y < 0) continue
            drawLine(
                color = outlineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1F
            )
            if (scales[i % 12]) {
                drawRect(
                    color = highlightNoteColor,
                    topLeft = Offset(0f, y),
                    size = Size(size.width, noteHeightPx)
                )
            }
        }

        EchoInMirror.selectedTrack?.let { track ->
            val invertsColor = track.color.inverts()
            val currentPPQ = EchoInMirror.currentPosition.timeInPPQ
            val isPlaying = EchoInMirror.currentPosition.isPlaying
            var startNoteIndex = 0
            track.notes.read()
            for ((index, it) in track.notes.withIndex()) {
                val y = (KEYBOARD_KEYS - 1 - it.note) * noteHeightPx - verticalScrollValue
                val x = it.time * noteWidthPx - horizontalScrollValue
                if (y < -noteHeightPx || y > size.height) continue
                val width = it.duration * noteWidthPx
                if (x < 0 && width < -x) continue
                if (x > size.width) break
                if (startNoteIndex == 0) startNoteIndex = index
                val isPlayingNote = isPlaying && it.time <= currentPPQ && it.time + it.duration >= currentPPQ
                val pos = Offset(x, y.coerceAtLeast(0F))
                val size = Size(width, if (y < 0)
                    (noteHeightPx + y).coerceAtLeast(0F) else noteHeightPx)
                drawRoundRect(if (isPlayingNote) invertsColor else track.color, pos, size, BorderCornerRadius2PX)
                if (EditorSingleton.selectedNotes.contains(it)) {
                    drawRoundRect(primaryColor, pos, size, BorderCornerRadius2PX, Stroke1PX)
                }
            }
            EditorSingleton.startNoteIndex = startNoteIndex
        }
    }
}

@Composable
private fun editorContent(horizontalScrollState: ScrollState) {
    VerticalSplitPane(splitPaneState = rememberSplitPaneState(100F)) {
        first(0.dp) {
            val verticalScrollState = rememberScrollState()
            val noteHeight by remember { mutableStateOf(16.dp) }
            val noteWidth by remember { mutableStateOf(0.4.dp) }
            Column(Modifier.fillMaxSize()) {
                val localDensity = LocalDensity.current
                var contentWidth by remember { mutableStateOf(0.dp) }
                val surfaceColor = getSurfaceColor(2.dp)
                Box(Modifier.drawWithContent {
                    drawContent()
                    drawRect(surfaceColor, Offset(0f, -8f), Size(size.width, 8F))
                }) {
                    Timeline(Modifier.zIndex(3F), noteWidth, horizontalScrollState, 68.dp)
                }
                Box(Modifier.weight(1F).onGloballyPositioned { with(localDensity) { contentWidth = it.size.width.toDp() } }) {
                    Row(Modifier.fillMaxSize().zIndex(-1F)) {
                        Surface(Modifier.verticalScroll(verticalScrollState).zIndex(5f), shadowElevation = 4.dp) {
                            Keyboard(
                                { it, p -> EchoInMirror.selectedTrack?.playMidiEvent(noteOn(0, it, (127 * p).toInt())) },
                                { it, _ -> EchoInMirror.selectedTrack?.playMidiEvent(noteOff(0, it)) },
                                Modifier, noteHeight
                            )
                        }
                        NotesEditorCanvas(
                            noteHeight,
                            noteWidth,
                            verticalScrollState,
                            horizontalScrollState,
                            Modifier.weight(1F).fillMaxHeight().background(MaterialTheme.colorScheme.background)
                        )
                    }
                    PlayHead(noteWidth, horizontalScrollState, contentWidth, 68.dp)
                    VerticalScrollbar(
                        rememberScrollbarAdapter(verticalScrollState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                    )
                }
            }
        }
        second(0.dp) {
            Row {

            }
        }

        splitter {
            visiblePart {
                Divider()
            }
        }
    }

}

class Editor: Panel {
    override val name = "编辑器"
    override val direction = PanelDirection.Horizontal

    @Composable
    override fun icon() {
        Icon(Icons.Default.Piano, "Editor")
    }

    @Composable
    override fun content() {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(200.dp)) {

            }
            Surface(Modifier.fillMaxSize(), shadowElevation = 2.dp) {
                Column {
                    Box {
                        val stateScroll = rememberSaveable(saver = ScrollState.Saver) {
                            ScrollState(0).apply {
                                @Suppress("INVISIBLE_SETTER")
                                maxValue = 10000
                            }
                        }
                        Column(Modifier.padding(top = 8.dp).fillMaxSize()) {
                            editorContent(stateScroll)
                        }
                        HorizontalScrollbar(
                            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
                            adapter = rememberScrollbarAdapter(stateScroll)
                        )
                    }
                }
            }
        }
    }
}