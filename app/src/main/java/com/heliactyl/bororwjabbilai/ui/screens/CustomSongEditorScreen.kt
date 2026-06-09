package com.heliactyl.bororwjabbilai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.LyricSection
import com.heliactyl.bororwjabbilai.Song
import com.heliactyl.bororwjabbilai.ui.components.bouncyClick
import com.heliactyl.bororwjabbilai.ui.components.liquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSongEditorScreen(
    initialSong: Song? = null,
    onSave: (Song) -> Unit,
    onCancel: () -> Unit
) {
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

    BackHandler(onBack = onCancel)

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
                    TextButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    Song(
                                        id = initialSong?.id ?: -1,
                                        categoryChar = "C",
                                        title = title,
                                        lyrics = lyrics.toList(),
                                        intentId = "custom",
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }

            itemsIndexed(lyrics) { index, section ->
                Card(
                    modifier = Modifier.fillMaxWidth().liquidGlass(cornerRadius = 16),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (section.type == "chorus") "Chorus" else "Verse ${section.number ?: (index + 1)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val newType = if (section.type == "verse") "chorus" else "verse"
                                lyrics[index] = section.copy(type = newType)
                            }) {
                                Icon(
                                    imageVector = if (section.type == "chorus") Icons.Default.FormatItalic else Icons.Default.FormatQuote,
                                    contentDescription = "Toggle Type"
                                )
                            }
                            IconButton(onClick = { if (lyrics.size > 1) lyrics.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Section")
                            }
                        }
                        
                        OutlinedTextField(
                            value = section.lines.joinToString("\n"),
                            onValueChange = { text ->
                                lyrics[index] = section.copy(lines = text.split("\n"))
                            },
                            placeholder = { Text("Enter lyrics here...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        lyrics.add(LyricSection("verse", lyrics.size + 1, listOf("")))
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).bouncyClick { },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Section")
                }
            }
            
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}
