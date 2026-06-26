package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ImageStorageManager
import com.example.viewmodel.BucketScoreViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    viewModel: BucketScoreViewModel,
    onNavigateBack: () -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🏀") }

    // Media support states
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var uploadedLogoUrl by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isUploading = true
                val resultUrl = ImageStorageManager.saveAndUploadImage(context, uri, "logos")
                uploadedLogoUrl = resultUrl
                isUploading = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraFile?.let { file ->
                coroutineScope.launch {
                    isUploading = true
                    val resultUrl = ImageStorageManager.saveAndUploadImage(context, android.net.Uri.fromFile(file), "logos")
                    uploadedLogoUrl = resultUrl
                    isUploading = false
                }
            }
        }
    }

    val logoEmojis = listOf(
        "🏀", "🦁", "🦅", "⚡", "🔥", "🏆", "🦈", "🐺", "🐻", "🐉", "⚔️", "🔱", "⭐", "👑"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register New Team", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logo photo preview or fallback mascot emoji
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (!uploadedLogoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = uploadedLogoUrl,
                        contentDescription = "Uploaded Logo Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(selectedEmoji, fontSize = 54.sp)
                }
            }

            Text(
                text = if (!uploadedLogoUrl.isNullOrBlank()) "CUSTOM BRAND LOGO ACTIVE" else "MASCOT & BRAND PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Image Upload Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val file = ImageStorageManager.createTempImageFile(context)
                        tempCameraFile = file
                        val cameraUri = ImageStorageManager.getUriForFile(context, file)
                        cameraLauncher.launch(cameraUri)
                    },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("camera_logo_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("gallery_logo_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (!uploadedLogoUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = { uploadedLogoUrl = null },
                        modifier = Modifier.height(44.dp).testTag("reset_logo_button")
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Name input
            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Team Name / Mascot") },
                placeholder = { Text("e.g. Green Tigers") },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_name_input")
            )

            if (uploadedLogoUrl.isNullOrBlank()) {
                Text(
                    text = "Or Select Team Emblem / Mascot",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                // Mascot Emoji grid selector
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    logoEmojis.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { emoji ->
                                val isSelected = selectedEmoji == emoji
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedEmoji = emoji }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 24.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save team button
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        viewModel.createTeam(teamName, selectedEmoji, uploadedLogoUrl) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_team_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = teamName.isNotBlank() && !isUploading
            ) {
                Text("Register Team", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
