package com.example.smartcalculator.ui.gallery

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartcalculator.data.EncryptedFile
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    isDecoyVault: Boolean,
    files: List<EncryptedFile>,
    onBack: () -> Unit,
    onAddFile: (Uri) -> Unit,
    onDeleteFile: (EncryptedFile) -> Unit,
    onFileClick: (EncryptedFile) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onAddFile(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        if (permissionsMap.values.all { it }) {
            pickMediaLauncher.launch("*/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isDecoyVault) "Gallery" else "Private Gallery")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    permissionLauncher.launch(permissions.toTypedArray())
                },
                containerColor = Color(0xFFFF9500)
            ) {
                Icon(Icons.Default.Add, "Add file", tint = Color.White)
            }
        }
    ) { padding ->
        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "No files yet",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Tap + to add photos or videos",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1E1E1E))
                    .padding(padding),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files, key = { it.id }) { file ->
                    GalleryItem(
                        file = file,
                        onClick = { onFileClick(file) },
                        onDelete = { onDeleteFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryItem(
    file: EncryptedFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(file.id) {
        if (file.fileType == "image" || file.fileType == "video") {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val encryptionUtils = com.example.smartcalculator.utils.EncryptionUtils(context)
                    val thumbnailPath = file.thumbnailPath ?: file.encryptedPath
                    val encryptedFile = java.io.File(thumbnailPath)
                    val tempFile = java.io.File(encryptionUtils.getTempDir(), "thumb_${file.id}.jpg")

                    if (encryptionUtils.decryptFile(encryptedFile, tempFile)) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(tempFile.absolutePath)
                        thumbnailBitmap = bitmap
                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .background(Color(0xFF2D2D2D))
    ) {
        when {
            file.fileType == "image" || file.fileType == "video" -> {
                thumbnailBitmap?.let { bitmap ->
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = file.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    }
                } else {
                    // Failed to load thumbnail
                    GenericFileView(file)
                }
            }
            else -> {
                // Non-image/video files
                GenericFileView(file)
            }
        }

        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete File") },
            text = { Text("Are you sure you want to delete this file?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GenericFileView(file: EncryptedFile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when (file.fileType) {
                "video" -> Icons.Default.VideoLibrary
                else -> Icons.Default.Description
            },
            contentDescription = file.fileType,
            tint = Color(0xFFFF9500),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = file.fileName,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = file.fileType.uppercase(),
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
