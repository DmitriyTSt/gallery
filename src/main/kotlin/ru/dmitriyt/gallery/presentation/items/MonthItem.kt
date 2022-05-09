package ru.dmitriyt.gallery.presentation.items

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MonthItem(title: String) {
    Text(text = title, modifier = Modifier.padding(24.dp))
}