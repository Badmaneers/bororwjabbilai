package com.heliactyl.bororwjabbilai.ui.paper.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceContainer
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceStyle
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperButton
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperButtonVariant
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperCard
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperCheckbox
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperChip
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperDialog
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperDivider
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperIconButton
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperLoadingIndicator
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperScaffold
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperSectionHeader
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperSwitch
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperTextField
import com.heliactyl.bororwjabbilai.ui.paper.components.PaperTopBar
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperTheme
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaperThemeShowcaseScreen(
    onBack: () -> Unit = {},
) {
    var themeMode by remember { mutableStateOf(PaperThemeMode.LIGHT) }
    var selectedSurfaceStyle by remember { mutableStateOf(PaperSurfaceStyle.RULED) }
    var textInput by remember { mutableStateOf("Sacred Hymn Note") }
    var switchState by remember { mutableStateOf(true) }
    var checkboxState by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }

    PaperTheme(mode = themeMode) {
        val colors = PaperTheme.colors
        val typography = PaperTheme.typography

        PaperScaffold(
            topBar = {
                PaperTopBar(
                    title = "Paper Design System",
                    subtitle = "Tactile Print & Editorial Theme",
                    navigationIcon = {
                        PaperIconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Theme Mode Switcher
                PaperSectionHeader(
                    title = "Theme Mode",
                    subtitle = "Switch between Light, Dark, AMOLED, and System paper palettes"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaperChip(
                        text = "Light Ivory",
                        selected = themeMode == PaperThemeMode.LIGHT,
                        icon = Icons.Default.WbSunny,
                        onClick = { themeMode = PaperThemeMode.LIGHT }
                    )
                    PaperChip(
                        text = "Dark Slate",
                        selected = themeMode == PaperThemeMode.DARK,
                        icon = Icons.Default.NightsStay,
                        onClick = { themeMode = PaperThemeMode.DARK }
                    )
                    PaperChip(
                        text = "AMOLED Black",
                        selected = themeMode == PaperThemeMode.AMOLED,
                        icon = Icons.Default.Contrast,
                        onClick = { themeMode = PaperThemeMode.AMOLED }
                    )
                    PaperChip(
                        text = "System",
                        selected = themeMode == PaperThemeMode.SYSTEM,
                        icon = Icons.Default.PhoneAndroid,
                        onClick = { themeMode = PaperThemeMode.SYSTEM }
                    )
                }

                PaperDivider()

                // 2. Paper Surface Style Switcher
                PaperSectionHeader(
                    title = "Surface Styles",
                    subtitle = "Select paper surface finish"
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaperSurfaceStyle.entries.forEach { style ->
                        PaperChip(
                            text = style.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = selectedSurfaceStyle == style,
                            onClick = { selectedSurfaceStyle = style }
                        )
                    }
                }

                // 3. Editorial Ruled Journal Note Showcase
                PaperSurfaceContainer(
                    style = selectedSurfaceStyle,
                    shape = PaperTheme.shapes.sheet,
                    showMargin = selectedSurfaceStyle == PaperSurfaceStyle.RULED,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = colors.accentInk
                            )
                            Text(
                                text = "Editorial Journal Note",
                                style = typography.titleLarge,
                                color = colors.textPrimary
                            )
                        }

                        Text(
                            text = "1. Gwthar, Gwthar, Gwthar,\nBwlwgrasin Iswr,\nPhungjaniao Nwngkhounw\nBakhnaitharw jwngw.",
                            style = typography.journalHandwritten,
                            color = colors.textPrimary
                        )

                        Text(
                            text = "A tactile paper surface designed for quiet focus and elegant reading.",
                            style = typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                PaperDivider()

                // 4. Buttons & Interactive Controls
                PaperSectionHeader(
                    title = "Interactive Controls",
                    subtitle = "Buttons, switches, and selection components"
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaperButton(
                        onClick = { showDialog = true },
                        variant = PaperButtonVariant.PRIMARY
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color.White)
                        Text("Open Dialog", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    PaperButton(
                        onClick = {},
                        variant = PaperButtonVariant.SECONDARY
                    ) {
                        Text("Secondary", color = colors.textPrimary)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaperSwitch(checked = switchState, onCheckedChange = { switchState = it })
                        Text("Paper Switch", style = typography.bodyMedium, color = colors.textPrimary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaperCheckbox(checked = checkboxState, onCheckedChange = { checkboxState = it })
                        Text("Checkbox", style = typography.bodyMedium, color = colors.textPrimary)
                    }
                }

                // 5. Input Fields
                PaperSectionHeader(
                    title = "Paper Input Field",
                    subtitle = "Text fields with warm paper outlines and label typography"
                )

                PaperTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = "Song Title / Note",
                    placeholder = "Enter title..."
                )

                // 6. Paper Cards Gallery
                PaperSectionHeader(
                    title = "Paper Card Gallery",
                    subtitle = "Cards with restrained depth and paper surface treatments"
                )

                PaperCard(
                    style = PaperSurfaceStyle.RULED,
                    onClick = {}
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PaperLoadingIndicator()
                        Column {
                            Text(
                                "Ruled Paper Card",
                                style = typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                "Interactive tactile card with notebook rules",
                                style = typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Dialog Preview
                if (showDialog) {
                    PaperDialog(
                        onDismissRequest = { showDialog = false },
                        title = "Paper Design Dialog",
                        confirmButton = {
                            PaperButton(
                                onClick = { showDialog = false },
                                variant = PaperButtonVariant.PRIMARY
                            ) {
                                Text("Confirm", color = Color.White)
                            }
                        },
                        dismissButton = {
                            PaperButton(
                                onClick = { showDialog = false },
                                variant = PaperButtonVariant.SECONDARY
                            ) {
                                Text("Cancel", color = colors.textPrimary)
                            }
                        }
                    ) {
                        Text(
                            text = "This dialog demonstrates the tactile paper overlay container with warm borders and high contrast typography.",
                            style = typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Paper Showcase Light", showBackground = true)
@Composable
fun PaperShowcaseLightPreview() {
    PaperTheme(mode = PaperThemeMode.LIGHT) {
        PaperThemeShowcaseScreen()
    }
}

@Preview(name = "Paper Showcase Dark", showBackground = true)
@Composable
fun PaperShowcaseDarkPreview() {
    PaperTheme(mode = PaperThemeMode.DARK) {
        PaperThemeShowcaseScreen()
    }
}

@Preview(name = "Paper Showcase AMOLED", showBackground = true)
@Composable
fun PaperShowcaseAmoledPreview() {
    PaperTheme(mode = PaperThemeMode.AMOLED) {
        PaperThemeShowcaseScreen()
    }
}
