package ru.dmitriyt.gallery.presentation.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun DirectoryItem(directory: File, onClick: (File) -> Unit) {
    Box(modifier = Modifier.aspectRatio(1f).fillMaxSize().padding(2.dp).clickable {
        onClick(directory)
    }) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Image(
                painter = painterResource("ic_folder.svg"),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentDescription = null,
            )
            Text(
                text = directory.name,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
            )
        }
    }
}