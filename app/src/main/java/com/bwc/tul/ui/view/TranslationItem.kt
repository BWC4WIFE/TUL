package com.bwc.tul.ui.view // Added package declaration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment

// Data class from existing TranslationAdapter.kt
data class TranslationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

@Composable
fun TranslationItemComposable(item: TranslationItem) {
    val speakerLabel = if (item.isUser) "You said:" else "Translation:"
    val backgroundColor = if (item.isUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val horizontalAlignment = if (item.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(color = backgroundColor, shape = MaterialTheme.shapes.medium) // Apply background and shape to the message container
            .padding(12.dp), // Padding inside the message container
        horizontalAlignment = horizontalAlignment, // Align the content within the column
        verticalArrangement = Arrangement.spacedBy(2.dp) // Space between speaker label and text
    ) {
        Text(
            text = speakerLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTranslationItemUser() {
    MaterialTheme {
        TranslationItemComposable(TranslationItem(text = "Hello, how are you?", isUser = true))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTranslationItemModel() {
    MaterialTheme {
        TranslationItemComposable(TranslationItem(text = "สวัสดี คุณเป็นอย่างไรบ้าง", isUser = false))
    }
}
