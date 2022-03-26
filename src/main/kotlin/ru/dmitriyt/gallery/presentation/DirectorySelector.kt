package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.dmitriyt.gallery.data.model.LoadingState
import java.io.File

@Composable
fun DirectorySelectorButton(text: String, oldDirectory: File?, modifier: Modifier = Modifier, onSelect: (File) -> Unit) {
    val showDialog = remember { mutableStateOf(false) }
    DirectorySelectorDialog(showDialog.value, oldDirectory, onSelect) {
        showDialog.value = false
    }
    Button(onClick = {
        showDialog.value = true
    }, modifier = modifier) {
        Text(text)
    }
}

@Composable
private fun DirectorySelectorDialog(showDialog: Boolean, oldDirectory: File?, onSelect: (File) -> Unit, closeDialog: () -> Unit) {
    if (showDialog) {
        Dialog(title = "Выбор директории", onCloseRequest = {
            closeDialog()
        }) {
            DirectoryListDialogContent(oldDirectory) { selectedDirectory ->
                onSelect(selectedDirectory)
                closeDialog()
            }
        }
    }
}

@Composable
private fun DirectoryListDialogContent(oldDirectory: File?, onSelect: (File) -> Unit) {
    val currentDir = remember { mutableStateOf(oldDirectory) }
    Column {
        if (currentDir.value != null) {
            TopAppBar(
                backgroundColor = Color.White,
                contentColor = Color.Black,
            ) {
                IconButton(onClick = {
                    currentDir.value = currentDir.value?.parentFile
                }) {
                    Icon(painter = rememberVectorPainter(Icons.Default.ArrowBack), contentDescription = null)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = currentDir.value.toString(),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
        FileListState(currentDir.value, { newDirectory ->
            currentDir.value = newDirectory
        }, { selectedDirectory ->
            onSelect(selectedDirectory)
        })
    }
}

@Composable
private fun FileListState(directory: File?, onChangeDir: (File) -> Unit, onSelect: (File) -> Unit) {
    val stateFiles = loadListFiles(directory)
    Box(modifier = Modifier.fillMaxSize()) {
        when (stateFiles.value) {
            is LoadingState.Error -> Text(
                text = "Не удалить получить файлы в директории",
                modifier = Modifier.align(Alignment.Center)
            )
            is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is LoadingState.Success -> {
                FileList((stateFiles.value as LoadingState.Success).data, onChangeDir, onSelect)
            }
        }
    }
}

@Composable
private fun FileList(files: List<File>, onChangeDir: (File) -> Unit, onSelect: (File) -> Unit) {
    LazyColumn {
        files.forEach { file ->
            item {
                Row(modifier = Modifier.clickable { onChangeDir(file) }.fillMaxWidth()) {
                    Text(
                        text = file.name.ifEmpty { file.toString() },
                        modifier = Modifier.padding(4.dp).weight(1f).align(Alignment.CenterVertically)
                    )
                    TextButton(onClick = {
                        onSelect(file)
                    }) {
                        Text("Выбрать")
                    }
                }
            }
        }
    }
}

@Composable
private fun loadListFiles(directory: File?): State<LoadingState<List<File>>> {
    return produceState<LoadingState<List<File>>>(initialValue = LoadingState.Loading(), directory) {
        val listFiles = (if (directory != null) directory.listFiles().orEmpty() else File.listRoots()).filter { it.isDirectory }

        value = LoadingState.Success(listFiles)
    }
}