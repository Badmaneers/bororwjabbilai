package com.heliactyl.bororwjabbilai.ui.screens

import android.Manifest
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.heliactyl.bororwjabbilai.CustomSongRepository
import com.heliactyl.bororwjabbilai.LyricSection
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CustomSongEditorScreen(
    initialSong: Song? = null,
    onSave: (Song) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val customSongRepository = remember { CustomSongRepository(context) }
    
    var title by remember { mutableStateOf(initialSong?.title ?: "") }
    val lyrics = remember { 
        mutableStateListOf<LyricSection>().apply {
            if (initialSong != null) {
                addAll(initialSong.lyrics)
            } else {
                add(LyricSection("verse", 1, listOf("")))
            }
        }
    }
    
    var showCamera by remember { mutableStateOf(false) }
    var showWipWarning by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf("") }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    if (showCamera) {
        BackHandler(onBack = { showCamera = false })
    } else {
        PredictiveBackHandler { progress: Flow<BackEventCompat> ->
            try {
                progress.collect { backEvent ->
                    predictiveProgress = backEvent.progress
                    swipeEdge = backEvent.swipeEdge
                }
                onCancel()
            } catch (_: CancellationException) {
                predictiveProgress = 0f
            }
        }
    }

    if (showWipWarning) {
        AlertDialog(
            onDismissRequest = { showWipWarning = false },
            title = { Text("OCR Beta") },
            text = { Text("The song scanner is still a work in progress and may be buggy. Alignment and recognition accuracy are being improved.") },
            confirmButton = {
                TextButton(onClick = {
                    showWipWarning = false
                    if (cameraPermissionState.status.isGranted) {
                        showCamera = true
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Song") },
            text = {
                Column {
                    Text("Paste the song code shared by another user.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = importCode,
                        onValueChange = { importCode = it },
                        label = { Text("Song Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val imported = customSongRepository.importSongFromCode(importCode)
                        if (imported != null) {
                            title = imported.title
                            lyrics.clear()
                            lyrics.addAll(imported.lyrics)
                            showImportDialog = false
                            importCode = ""
                        }
                    },
                    enabled = importCode.isNotBlank()
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importCode = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    val animatedProgress by animateFloatAsState(
        targetValue = predictiveProgress,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "editorUnsnapProgress"
    )

    val densityPx = LocalDensity.current.density
    val unsnapScale = 1f - (animatedProgress * 0.12f)
    val unsnapCornerRadius = (animatedProgress * 28).dp
    val unsnapTranslationX = if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
        -animatedProgress * 60f * densityPx
    } else {
        animatedProgress * 60f * densityPx
    }
    val unsnapTranslationY = animatedProgress * 20f * densityPx
    val unsnapAlpha = 1f - (animatedProgress * 0.15f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = unsnapScale
                scaleY = unsnapScale
                translationX = unsnapTranslationX
                translationY = unsnapTranslationY
                alpha = unsnapAlpha
                shadowElevation = animatedProgress * 16f * densityPx
                shape = RoundedCornerShape(unsnapCornerRadius)
                clip = true
            }
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .liquidGlass(cornerRadius = 28)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCancel, modifier = Modifier.bouncyClick(onClick = onCancel)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        Text(
                            text = if (initialSong == null) "New Custom Song" else "Edit Song",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.bouncyClick { }
                        ) {
                            Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Import Code")
                        }
                        IconButton(
                            onClick = {
                                showWipWarning = true
                            },
                            modifier = Modifier.bouncyClick { }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Scan Lyrics")
                        }
                        TextButton(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSave(
                                        Song(
                                            id = initialSong?.id ?: -1,
                                            categoryChar = "C",
                                            title = title,
                                            lyrics = lyrics.toList(),
                                            isFavorite = initialSong?.isFavorite ?: false,
                                            isCustom = true
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.bouncyClick { }
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Song Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                itemsIndexed(lyrics) { sectionIndex, section ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(cornerRadius = 20)
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (section.type == "chorus") "Chorus" else "Verse ${section.number ?: (sectionIndex + 1)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        val newType = if (section.type == "verse") "chorus" else "verse"
                                        lyrics[sectionIndex] = section.copy(type = newType)
                                    }
                                ) {
                                    Text(if (section.type == "verse") "Set as Chorus" else "Set as Verse")
                                }
                                if (lyrics.size > 1) {
                                    IconButton(
                                        onClick = { lyrics.removeAt(sectionIndex) },
                                        modifier = Modifier.bouncyClick { }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Remove Section",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = section.lines.joinToString("\n"),
                                onValueChange = { text ->
                                    val newLines = text.split("\n")
                                    lyrics[sectionIndex] = section.copy(lines = newLines)
                                },
                                label = { Text("Lyrics (one line per row)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = {
                            lyrics.add(LyricSection("verse", lyrics.size + 1, listOf("")))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Section")
                    }
                }
                
                item { Spacer(Modifier.height(100.dp)) }
            }
        }

        AnimatedVisibility(
            visible = showCamera,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            CameraOcrScreen(
                onTextRecognized = { recognizedText ->
                    val sections = recognizedText.split(Regex("\n\\s*\n"))
                    if (sections.isNotEmpty()) {
                        if ((lyrics.size == 1) && lyrics[0].lines.all { it.isBlank() }) {
                            lyrics.clear()
                        }
                        sections.forEach { sectionText ->
                            val lines = sectionText.trim().split("\n")
                            if (lines.any { it.isNotBlank() }) {
                                lyrics.add(LyricSection("verse", lyrics.size + 1, lines))
                            }
                        }
                    }
                    showCamera = false
                },
                onBack = { showCamera = false }
            )
        }
    }
}
