package com.heliactyl.bororwjabbilai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun SocialRow(
    text: String, 
    handle: String, 
    imageLoader: ImageLoader,
    iconModel: Any? = null,
    iconVector: ImageVector? = null,
    tint: Color? = null, 
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = text,
                tint = tint ?: LocalContentColor.current,
                modifier = Modifier.size(24.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconModel)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                colorFilter = tint?.let { ColorFilter.tint(it) }
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = handle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
