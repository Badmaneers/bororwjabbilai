package com.heliactyl.bororwjabbilai.ui.paper.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceContainer
import com.heliactyl.bororwjabbilai.ui.paper.PaperSurfaceStyle
import com.heliactyl.bororwjabbilai.ui.paper.paperSurface
import com.heliactyl.bororwjabbilai.ui.theme.paper.PaperTheme

// 1. PaperScaffold
@Composable
fun PaperScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = PaperTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            topBar()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(PaddingValues(0.dp))
            }
            bottomBar()
        }
    }
}

// 2. PaperCard
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    style: PaperSurfaceStyle = PaperSurfaceStyle.RULED,
    onClick: (() -> Unit)? = null,
    shape: Shape = PaperTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .paperSurface(style = style, shape = shape)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

// 3. PaperButton
enum class PaperButtonVariant {
    PRIMARY,
    SECONDARY,
    TEXT,
}

@Composable
fun PaperButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PaperButtonVariant = PaperButtonVariant.PRIMARY,
    enabled: Boolean = true,
    shape: Shape = PaperTheme.shapes.medium,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = PaperTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "buttonScale"
    )

    val bgModifier = when (variant) {
        PaperButtonVariant.PRIMARY -> Modifier.background(
            if (enabled) colors.accentInk else colors.border,
            shape = shape
        )
        PaperButtonVariant.SECONDARY -> Modifier
            .paperSurface(style = PaperSurfaceStyle.FLAT, shape = shape)
            .background(
                if (enabled) colors.surfaceElevated else colors.surface,
                shape = shape
            )
        PaperButtonVariant.TEXT -> Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(bgModifier)
            .clip(shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

// 4. PaperIconButton
@Composable
fun PaperIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    val colors = PaperTheme.colors

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = colors.textPrimary,
            disabledContentColor = colors.textTertiary
        ),
        content = icon
    )
}

// 5. PaperTopBar
@Composable
fun PaperTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = PaperTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .paperSurface(style = PaperSurfaceStyle.FLAT, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationIcon?.invoke()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    style = PaperTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = PaperTheme.typography.caption,
                        color = colors.accentInk
                    )
                }
            }
            actions?.invoke(this)
        }
    }
}

// 6. PaperTextField
@Composable
fun PaperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
) {
    val colors = PaperTheme.colors

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it, style = PaperTheme.typography.labelMedium) } },
        placeholder = placeholder?.let { { Text(it, style = PaperTheme.typography.bodyMedium) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isError = isError,
        shape = PaperTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceElevated,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.accentInk,
            unfocusedBorderColor = colors.border,
            focusedLabelColor = colors.accentInk,
            unfocusedLabelColor = colors.textSecondary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        )
    )
}

// 7. PaperChip
@Composable
fun PaperChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val colors = PaperTheme.colors

    val chipBg = if (selected) colors.accentInk.copy(alpha = 0.18f) else colors.surface
    val chipBorder = if (selected) colors.accentInk else colors.border
    val textColor = if (selected) colors.accentInk else colors.textPrimary

    Box(
        modifier = modifier
            .paperSurface(style = PaperSurfaceStyle.FLAT, shape = PaperTheme.shapes.pill)
            .background(chipBg, shape = PaperTheme.shapes.pill)
            .border(1.dp, chipBorder, shape = PaperTheme.shapes.pill)
            .clip(PaperTheme.shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                style = PaperTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// 8. PaperDivider
@Composable
fun PaperDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    color: Color = PaperTheme.colors.divider,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

// 9. PaperSectionHeader
@Composable
fun PaperSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = PaperTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .background(colors.accentInk, shape = RoundedCornerShape(2.dp))
                )
                Text(
                    text = title,
                    style = PaperTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )
            }
            action?.invoke()
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = PaperTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}

// 10. PaperSwitch & PaperCheckbox
@Composable
fun PaperSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PaperTheme.colors
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "switchOffset"
    )

    Box(
        modifier = modifier
            .size(48.dp, 28.dp)
            .clip(CircleShape)
            .background(if (checked) colors.accentInk else colors.border)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color.White, CircleShape)
                .padding(start = thumbOffset.dp)
        )
    }
}

@Composable
fun PaperCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PaperTheme.colors

    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) colors.accentInk else colors.surface)
            .border(1.5.dp, if (checked) colors.accentInk else colors.border, RoundedCornerShape(6.dp))
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// 11. PaperDialog & PaperBottomSheet
@Composable
fun PaperDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = PaperTheme.colors

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .paperSurface(style = PaperSurfaceStyle.RULED, shape = PaperTheme.shapes.sheet)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = title,
                    style = PaperTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                content()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}

// 12. PaperNavigationBar
@Composable
fun PaperNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = PaperTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .paperSurface(style = PaperSurfaceStyle.FLAT, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// 13. PaperLoadingIndicator
@Composable
fun PaperLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = PaperTheme.colors.accentInk,
) {
    CircularProgressIndicator(
        modifier = modifier.size(32.dp),
        color = color,
        strokeWidth = 3.dp
    )
}
