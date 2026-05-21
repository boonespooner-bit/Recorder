package com.recorder.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.recorder.app.ui.theme.BackgroundDark
import com.recorder.app.ui.theme.ChordGold
import com.recorder.app.ui.theme.OnSurfaceDark
import com.recorder.app.ui.theme.OnSurfaceVariantDark
import com.recorder.app.ui.theme.PrimaryAmber
import com.recorder.app.ui.theme.PrimaryAmberDark
import com.recorder.app.ui.theme.RecordRed
import com.recorder.app.ui.theme.SurfaceDark
import com.recorder.app.ui.theme.SurfaceVariantDark

private val CHORD_DISPLAY_NAMES = mapOf(
    "G"  to "G major",
    "C"  to "C major",
    "D"  to "D major",
    "A"  to "A major",
    "Am" to "A minor",
    "Em" to "E minor",
    "E"  to "E major",
    "F"  to "F major"
)

private val CHORD_FINGERING_HINTS = mapOf(
    "G"  to "Frets: 3-2-0-0-0-3  (low→high)\nFingers: R - M - - R",
    "C"  to "Frets: x-3-2-0-1-0  (low→high)\nFingers: - R M - I -",
    "D"  to "Frets: x-x-0-2-3-2  (low→high)\nFingers: - - - I R M",
    "A"  to "Frets: x-0-2-2-2-0  (low→high)\nFingers: - - I M R -",
    "Am" to "Frets: x-0-2-2-1-0  (low→high)\nFingers: - - M R I -",
    "Em" to "Frets: 0-2-2-0-0-0  (low→high)\nFingers: - I M - - -",
    "E"  to "Frets: 0-2-2-1-0-0  (low→high)\nFingers: - M R I - -",
    "F"  to "Frets: 1-3-3-2-1-1  (low→high)\nFingers: I R R M I I (barre)"
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentChordIndex by viewModel.currentChordIndex.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val trainedChords by viewModel.trainedChords.collectAsState()
    val allDone by viewModel.allDone.collectAsState()

    val chords = OnboardingViewModel.TRAINING_CHORDS
    val totalChords = chords.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (allDone) {
            AllDoneCard(onComplete = onComplete)
        } else {
            val currentChord = chords.getOrNull(currentChordIndex) ?: return@Box

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header
                Text(
                    text = "Train Your Chords",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryAmber,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "So the app can recognize your playing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${trainedChords.size} / $totalChords trained",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantDark
                        )
                        Text(
                            text = "${((trainedChords.size.toFloat() / totalChords) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { trainedChords.size.toFloat() / totalChords },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryAmber,
                        trackColor = SurfaceVariantDark
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Chord progress dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    chords.forEachIndexed { index, chord ->
                        ChordDot(
                            chord = chord,
                            isTrained = trainedChords.contains(chord),
                            isCurrent = index == currentChordIndex
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Chord card
                AnimatedContent(
                    targetState = currentChord,
                    transitionSpec = {
                        slideInVertically { it } + fadeIn() togetherWith
                                slideOutVertically { -it } + fadeOut()
                    },
                    label = "chord_transition"
                ) { chord ->
                    ChordCard(chord = chord)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Instructions
                Text(
                    text = "Play this chord and hold it for 3 seconds, then tap Stop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Recording controls
                if (isProcessing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAmber,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Saving template…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariantDark
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAndSaveChord()
                            } else {
                                viewModel.startRecordingChord()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) RecordRed else PrimaryAmber,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRecording) "Stop & Save" else "Start Recording",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { viewModel.skipChord() },
                    enabled = !isProcessing
                ) {
                    Text(
                        text = "Skip this chord",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )
                }
            }
        }
    }
}

@Composable
private fun ChordDot(
    chord: String,
    isTrained: Boolean,
    isCurrent: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isTrained -> PrimaryAmber
                        isCurrent -> SurfaceVariantDark
                        else -> SurfaceDark
                    }
                )
                .border(
                    width = if (isCurrent) 2.dp else 1.dp,
                    color = when {
                        isTrained -> PrimaryAmberDark
                        isCurrent -> PrimaryAmber
                        else -> SurfaceVariantDark
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isTrained) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Trained",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = chord,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (chord.length > 1) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isCurrent) PrimaryAmber else OnSurfaceVariantDark
                )
            }
        }
    }
}

@Composable
private fun ChordCard(chord: String) {
    val displayName = CHORD_DISPLAY_NAMES[chord] ?: chord
    val fingeringHint = CHORD_FINGERING_HINTS[chord] ?: ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = chord,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 80.sp,
                    letterSpacing = 0.sp
                ),
                color = ChordGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariantDark
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Fingering hint in monospace
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)
            ) {
                Text(
                    text = fingeringHint,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    ),
                    color = OnSurfaceDark,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AllDoneCard(onComplete: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "All chords trained!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PrimaryAmber,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "The app will now recognize your chord changes in real time.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAmber,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Let's go",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}
