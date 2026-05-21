package com.recorder.app.ui.screens.detail

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import com.recorder.app.data.model.Recording
import com.recorder.app.data.model.SyncStatus
import com.recorder.app.data.model.TimedChord
import com.recorder.app.ui.theme.BackgroundDark
import com.recorder.app.ui.theme.ChordGold
import com.recorder.app.ui.theme.LyricsGray
import com.recorder.app.ui.theme.OnSurfaceDark
import com.recorder.app.ui.theme.OnSurfaceVariantDark
import com.recorder.app.ui.theme.PrimaryAmber
import com.recorder.app.ui.theme.RecordRed
import com.recorder.app.ui.theme.Secondary
import com.recorder.app.ui.theme.SurfaceDark
import com.recorder.app.ui.theme.SurfaceVariantDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recordingId: Long,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    LaunchedEffect(recordingId) {
        viewModel.load(recordingId)
    }

    val context = LocalContext.current

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? DetailUiState.Ready)?.recording?.title ?: "Recording",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDark,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurfaceDark
                        )
                    }
                },
                actions = {
                    if (uiState is DetailUiState.Ready) {
                        IconButton(onClick = {
                            val rec = (uiState as DetailUiState.Ready).recording
                            val file = File(rec.filePath)
                            if (file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, rec.title)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share recording"))
                                } catch (_: Exception) { }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = OnSurfaceVariantDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryAmber)
                }
            }

            is DetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Recording not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnSurfaceVariantDark
                    )
                }
            }

            is DetailUiState.Ready -> {
                RecordingDetail(
                    recording = state.recording,
                    isSyncing = isSyncing,
                    onTitleChange = { viewModel.updateTitle(it) },
                    onSyncToDrive = { viewModel.syncToDrive() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun RecordingDetail(
    recording: Recording,
    isSyncing: Boolean,
    onTitleChange: (String) -> Unit,
    onSyncToDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title card with inline editing
        TitleCard(
            title = recording.title,
            onTitleChange = onTitleChange
        )

        // Meta info
        MetaInfoRow(recording = recording)

        // Playback bar
        PlaybackCard(recording = recording)

        // Chord + Lyrics combined card (key feature)
        ChordLyricsCard(recording = recording)

        // Drive sync card
        DriveSyncCard(
            syncStatus = recording.syncStatus,
            isSyncing = isSyncing,
            onSyncClick = onSyncToDrive
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun TitleCard(title: String, onTitleChange: (String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(title) { mutableStateOf(title) }

    fun commitEdit() {
        val trimmed = editText.trim()
        if (trimmed.isNotEmpty()) onTitleChange(trimmed)
        isEditing = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = OnSurfaceDark,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAmber,
                        unfocusedBorderColor = SurfaceVariantDark,
                        cursorColor = PrimaryAmber
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitEdit() })
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { commitEdit() }) {
                    Text(text = "Save", color = PrimaryAmber, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurfaceDark,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isEditing = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit title",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaInfoRow(recording: Recording) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetaChip(
            label = formatDuration(recording.durationMs),
            modifier = Modifier.weight(1f)
        )
        MetaChip(
            label = formatDate(recording.createdAt),
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun MetaChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariantDark
        )
    }
}

// ---------------------------------------------------------------------------
// Playback Card
// ---------------------------------------------------------------------------

@Composable
private fun PlaybackCard(recording: Recording) {
    var isPlaying by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var prepared by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(recording.filePath) {
        val file = File(recording.filePath)
        if (file.exists()) {
            try {
                mediaPlayer.setDataSource(recording.filePath)
                mediaPlayer.prepare()
                prepared = true
            } catch (_: Exception) {
                prepared = false
            }
        }
        mediaPlayer.setOnCompletionListener {
            isPlaying = false
            positionMs = 0L
        }
        onDispose {
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = mediaPlayer.currentPosition.toLong()
            delay(200)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (!prepared) return@IconButton
                    if (isPlaying) {
                        mediaPlayer.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer.start()
                        isPlaying = true
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (prepared) PrimaryAmber else SurfaceVariantDark)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (prepared) Color.Black else OnSurfaceVariantDark,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                val durationMs = recording.durationMs.toFloat().coerceAtLeast(1f)
                Slider(
                    value = positionMs.toFloat().coerceIn(0f, durationMs),
                    onValueChange = { newPos ->
                        positionMs = newPos.toLong()
                        if (prepared) mediaPlayer.seekTo(newPos.toInt())
                    },
                    valueRange = 0f..durationMs,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = prepared,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAmber,
                        activeTrackColor = PrimaryAmber,
                        inactiveTrackColor = SurfaceVariantDark,
                        disabledThumbColor = SurfaceVariantDark,
                        disabledActiveTrackColor = SurfaceVariantDark
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantDark
                    )
                    Text(
                        text = formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariantDark
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Chord + Lyrics Card (combined)
// ---------------------------------------------------------------------------

@Composable
private fun ChordLyricsCard(recording: Recording) {
    val hasChords = recording.chords.isNotEmpty()
    val hasLyrics = recording.lyrics.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = PrimaryAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Chords & Lyrics",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDark
                    )
                }
                // Re-analyze hint (visual only; re-analysis wired through ViewModel)
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Re-analyze",
                    tint = OnSurfaceVariantDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            when {
                hasChords && hasLyrics -> ChordLyricsView(
                    chords = recording.chords,
                    lyrics = recording.lyrics
                )
                hasChords -> ChordProgressionSection(chords = recording.chords)
                hasLyrics -> Text(
                    text = recording.lyrics,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 28.sp
                    ),
                    color = LyricsGray
                )
                else -> Text(
                    text = "Chord analysis will appear here",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ChordLyricsView — Ultimate Guitar-style layout
// ---------------------------------------------------------------------------

/**
 * Renders chords floating above lyric lines (Ultimate Guitar / chord sheet style).
 *
 * 1. Split lyrics into words.
 * 2. Assign each chord to a word index proportionally (chordIdx * wordCount / chordCount).
 * 3. Chunk words into lines of WORDS_PER_LINE.
 * 4. For each line render:
 *    - A chord row with names positioned above their word using leading Spacers.
 *    - A lyric row with the words joined by spaces in monospace font.
 */
@Composable
fun ChordLyricsView(
    chords: List<TimedChord>,
    lyrics: String
) {
    val words = lyrics.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) {
        ChordProgressionSection(chords = chords)
        return
    }

    // Map wordIndex -> chord (first chord wins when multiple land on same word)
    val chordAtWord = buildChordWordMap(chords, words.size)

    val wordsPerLine = 8
    val lines = words.chunked(wordsPerLine)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        lines.forEachIndexed { lineIndex, lineWords ->
            ChordLyricsLine(
                lineWords = lineWords,
                lineStartWord = lineIndex * wordsPerLine,
                chordAtWord = chordAtWord
            )
        }
    }
}

private fun buildChordWordMap(chords: List<TimedChord>, wordCount: Int): Map<Int, String> {
    val map = mutableMapOf<Int, String>()
    chords.forEachIndexed { chordIndex, timedChord ->
        val wordIndex = (chordIndex.toLong() * wordCount / chords.size)
            .toInt().coerceIn(0, wordCount - 1)
        if (!map.containsKey(wordIndex)) {
            map[wordIndex] = timedChord.chord
        }
    }
    return map
}

@Composable
private fun ChordLyricsLine(
    lineWords: List<String>,
    lineStartWord: Int,
    chordAtWord: Map<Int, String>
) {
    // Accumulate character start positions for each word (word + 1 space)
    val wordCharStarts = mutableListOf<Int>()
    var charPos = 0
    lineWords.forEach { word ->
        wordCharStarts.add(charPos)
        charPos += word.length + 1
    }

    // Collect (charPosition, chordName) pairs for this line
    val chordPositions: List<Pair<Int, String>> = lineWords.indices.mapNotNull { localIdx ->
        chordAtWord[lineStartWord + localIdx]?.let { chord -> wordCharStarts[localIdx] to chord }
    }

    // Approximate monospace character width at fontSize 14sp
    val charWidthDp = 8.4.dp

    Column {
        // Chord row
        if (chordPositions.isNotEmpty()) {
            Row(verticalAlignment = Alignment.Bottom) {
                var cursorChar = 0
                chordPositions.forEach { (charIdx, chord) ->
                    val gap = (charIdx - cursorChar).coerceAtLeast(0)
                    if (gap > 0) Spacer(modifier = Modifier.width(charWidthDp * gap))
                    Text(
                        text = chord,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ChordGold
                        )
                    )
                    cursorChar = charIdx + chord.length
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        // Lyric row
        Text(
            text = lineWords.joinToString(" "),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
            color = LyricsGray
        )
    }
}

// ---------------------------------------------------------------------------
// Chord-only progression (no lyrics)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChordProgressionSection(chords: List<TimedChord>) {
    val deduplicated = chords.fold(mutableListOf<TimedChord>()) { acc, tc ->
        if (acc.isEmpty() || acc.last().chord != tc.chord) acc.add(tc); acc
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        deduplicated.forEach { tc ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tc.chord,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = ChordGold
                    )
                }
                Text(
                    text = formatDuration(tc.startTimeMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = OnSurfaceVariantDark
                )
            }
        }
    }
}

@Composable
private fun DriveSyncCard(
    syncStatus: SyncStatus,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, tint, statusText) = when {
                isSyncing -> Triple(Icons.Default.CloudUpload, PrimaryAmber, "Uploading to Drive…")
                syncStatus == SyncStatus.SYNCED -> Triple(Icons.Default.CloudDone, Secondary, "Saved to Google Drive")
                syncStatus == SyncStatus.ERROR -> Triple(Icons.Default.CloudOff, RecordRed, "Sync failed")
                else -> Triple(Icons.Default.CloudOff, OnSurfaceVariantDark, "Not backed up")
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
                modifier = Modifier.weight(1f)
            )

            if (syncStatus != SyncStatus.SYNCED && !isSyncing) {
                Button(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Upload",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryAmber,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}


private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return sdf.format(Date(epochMs))
}
