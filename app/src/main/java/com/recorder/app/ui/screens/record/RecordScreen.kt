package com.recorder.app.ui.screens.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recorder.app.ui.theme.BackgroundDark
import com.recorder.app.ui.theme.LyricsGray
import com.recorder.app.ui.theme.OnSurfaceVariantDark
import com.recorder.app.ui.theme.PrimaryAmber
import com.recorder.app.ui.theme.RecordRed
import com.recorder.app.ui.theme.SurfaceDark
import com.recorder.app.ui.theme.SurfaceVariantDark
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onRecordingComplete: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel()
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val elapsedMs by viewModel.elapsedMs.collectAsState()
    val partialLyrics by viewModel.partialLyrics.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    LaunchedEffect(recordingState) {
        if (recordingState is RecordState.Done) {
            onRecordingComplete((recordingState as RecordState.Done).recordingId)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isRecording) "Recording…" else "New Recording",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isRecording) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isRecording) OnSurfaceVariantDark else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Timer display
            ElapsedTimer(elapsedMs = elapsedMs, isRecording = isRecording)

            // Record / Stop button
            RecordButton(
                isRecording = isRecording,
                isSaving = recordingState is RecordState.Saving,
                onRecord = { viewModel.startRecording() },
                onStop = { viewModel.stopAndSave() }
            )

            // Live lyrics
            AnimatedVisibility(
                visible = isRecording || partialLyrics.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LyricsDisplay(lyrics = partialLyrics, isRecording = isRecording)
            }

            if (!isRecording && recordingState is RecordState.Idle) {
                Text(
                    text = "Tap the mic to start recording.\nChords and lyrics are detected automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun ElapsedTimer(elapsedMs: Long, isRecording: Boolean) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60
    val tenths = (elapsedMs % 1000) / 100

    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Light,
                fontSize = 72.sp,
                letterSpacing = 4.sp
            ),
            color = if (isRecording) RecordRed else OnSurfaceVariantDark
        )
        if (isRecording) {
            Text(
                text = ".$tenths",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Light
                ),
                color = RecordRed.copy(alpha = alpha),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    isSaving: Boolean,
    onRecord: () -> Unit,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isRecording) 0.1f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        if (isRecording) {
            // Pulsing ring
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = RecordRed.copy(alpha = ringAlpha),
                        shape = CircleShape
                    )
            )
        }

        IconButton(
            onClick = if (isRecording) onStop else onRecord,
            enabled = !isSaving,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isRecording) RecordRed else PrimaryAmber),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Black
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.Black,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsDisplay(lyrics: String, isRecording: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) RecordRed else SurfaceVariantDark)
                        .alpha(if (isRecording) 1f else 0.4f)
                )
                Text(
                    text = "Live Lyrics",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurfaceVariantDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (lyrics.isNotEmpty()) {
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = FontStyle.Italic,
                        lineHeight = 28.sp
                    ),
                    color = LyricsGray
                )
            } else {
                Text(
                    text = "Listening for lyrics…",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = OnSurfaceVariantDark
                )
            }
        }
    }
}
