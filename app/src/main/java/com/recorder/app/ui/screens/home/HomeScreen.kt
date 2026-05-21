package com.recorder.app.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recorder.app.data.model.Recording
import com.recorder.app.data.model.SyncStatus
import com.recorder.app.ui.theme.BackgroundDark
import com.recorder.app.ui.theme.ChordGold
import com.recorder.app.ui.theme.ErrorRed
import com.recorder.app.ui.theme.OnSurfaceVariantDark
import com.recorder.app.ui.theme.PrimaryAmber
import com.recorder.app.ui.theme.RecordRed
import com.recorder.app.ui.theme.SurfaceDark
import com.recorder.app.ui.theme.SurfaceVariantDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecordClick: () -> Unit,
    onRecordingClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recordings by viewModel.recordings.collectAsState()

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recorder",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = PrimaryAmber
                    )
                },
                actions = {
                    IconButton(onClick = onRecordClick) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "New recording",
                            tint = PrimaryAmber
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    scrolledContainerColor = SurfaceDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRecordClick,
                containerColor = PrimaryAmber,
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        if (recordings.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(
                    items = recordings,
                    key = { it.id }
                ) { recording ->
                    SwipeableRecordingCard(
                        recording = recording,
                        onDelete = { viewModel.deleteRecording(recording) },
                        onTap = { onRecordingClick(recording.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = OnSurfaceVariantDark,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                text = "Tap + to make your first recording",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariantDark
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableRecordingCard(
    recording: Recording,
    onDelete: () -> Unit,
    onTap: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    ErrorRed else SurfaceVariantDark,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        RecordingCard(recording = recording, onTap = onTap)
    }
}

@Composable
private fun RecordingCard(recording: Recording, onTap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
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
            // Waveform icon placeholder
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = PrimaryAmber,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDate(recording.createdAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                    Text(
                        text = formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Sync status icon
                SyncStatusIcon(syncStatus = recording.syncStatus)

                // Chord count badge
                if (recording.chords.isNotEmpty()) {
                    ChordBadge(count = recording.chords.size)
                }
            }
        }
    }
}

@Composable
private fun SyncStatusIcon(syncStatus: SyncStatus) {
    when (syncStatus) {
        SyncStatus.SYNCED -> Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = "Synced to Drive",
            tint = ChordGold,
            modifier = Modifier.size(18.dp)
        )
        SyncStatus.SYNCING -> Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = "Syncing to Drive",
            tint = OnSurfaceVariantDark,
            modifier = Modifier.size(18.dp)
        )
        SyncStatus.NOT_SYNCED -> Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Not synced",
            tint = OnSurfaceVariantDark,
            modifier = Modifier.size(18.dp)
        )
        SyncStatus.ERROR -> Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Sync error",
            tint = RecordRed,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ChordBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count chords",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = ChordGold
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return "%d:%02d".format(minutes, seconds)
}
