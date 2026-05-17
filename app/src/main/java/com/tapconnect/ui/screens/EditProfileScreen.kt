package com.tapconnect.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tapconnect.data.remote.UserProfileUpdateDto
import com.tapconnect.ui.theme.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Form state
    var fullName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    
    // Professional Interests
    var interestsInput by remember { mutableStateOf("") }
    var interestsList by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Social Links
    var linkedin by remember { mutableStateOf("") }
    var github by remember { mutableStateOf("") }
    var twitter by remember { mutableStateOf("") }
    
    var isSaving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Initialize form with current data
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            val profile = (uiState as ProfileUiState.Success).profile
            fullName = profile.full_name
            bio = profile.bio ?: ""
            role = profile.role ?: ""
            organization = profile.organization ?: ""
            interestsList = profile.interests
            linkedin = profile.social_links["linkedin"] ?: ""
            github = profile.social_links["github"] ?: ""
            twitter = profile.social_links["twitter"] ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            // --- Profile Image Upload Section ---
            var currentImageUrl by remember { mutableStateOf("") }
            LaunchedEffect(uiState) {
                if (uiState is ProfileUiState.Success) {
                    currentImageUrl = (uiState as ProfileUiState.Success).profile.profile_image_url ?: ""
                }
            }

            val context = LocalContext.current
            var isUploadingImage by remember { mutableStateOf(false) }

            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val filePart = createMultipartBodyPart(context, it)
                    if (filePart != null) {
                        isUploadingImage = true
                        validationError = null
                        viewModel.uploadProfileImage(filePart) { success ->
                            isUploadingImage = false
                            if (!success) {
                                validationError = "Failed to upload image. Please try again."
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(85.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentImageUrl.isNotBlank()) {
                        AsyncImage(
                            model = currentImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                    }
                    if (isUploadingImage) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isUploadingImage
                    ) {
                        Text("Upload Photo", fontSize = 14.sp)
                    }
                    Text("Select a JPG or PNG picture.", fontSize = 11.sp, color = TextSub, modifier = Modifier.padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Basic Fields ---
            EditField(label = "Full Name", value = fullName, onValueChange = { fullName = it; validationError = null })
            Spacer(modifier = Modifier.height(16.dp))
            
            EditField(label = "Role", value = role, onValueChange = { role = it })
            Spacer(modifier = Modifier.height(16.dp))
            
            EditField(label = "Organization", value = organization, onValueChange = { organization = it })
            Spacer(modifier = Modifier.height(16.dp))
            
            EditField(label = "Bio", value = bio, onValueChange = { bio = it }, singleLine = false, minLines = 3)
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- Professional Interests Section ---
            Text("Professional Interests", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextH, modifier = Modifier.padding(bottom = 8.dp))
            HorizontalDivider(color = Divider, modifier = Modifier.padding(bottom = 16.dp))

            // Tag list display
            if (interestsList.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    interestsList.forEach { interest ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentLight)
                                .clickable {
                                    // Remove tag on click
                                    interestsList = interestsList - interest
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = interest, color = AccentIndigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "✕", color = AccentIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Input field to add tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = interestsInput,
                    onValueChange = { interestsInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. Compose, AI, Kotlin", color = TextSub, fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Divider,
                        focusedBorderColor = AccentIndigo,
                        unfocusedContainerColor = CardBg,
                        focusedContainerColor = CardBg
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (interestsInput.trim().isNotBlank()) {
                            val newInterests = interestsInput.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() && !interestsList.contains(it) }
                            interestsList = interestsList + newInterests
                            interestsInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Add")
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            
            // --- Social Links Fields ---
            Text("Social Profiles", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextH, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            HorizontalDivider(color = Divider, modifier = Modifier.padding(bottom = 16.dp))

            EditField(label = "LinkedIn Profile", value = linkedin, onValueChange = { linkedin = it })
            Spacer(modifier = Modifier.height(16.dp))

            EditField(label = "GitHub Profile", value = github, onValueChange = { github = it })
            Spacer(modifier = Modifier.height(16.dp))

            EditField(label = "Twitter/X Profile", value = twitter, onValueChange = { twitter = it })
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- Validation Error Banner ---
            validationError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = AccentRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // --- Action Save ---
            Button(
                onClick = {
                    if (fullName.trim().isEmpty()) {
                        validationError = "Full Name cannot be empty."
                        return@Button
                    }
                    
                    isSaving = true
                    validationError = null
                    
                    val linksMap = mutableMapOf<String, String>()
                    if (linkedin.isNotBlank()) linksMap["linkedin"] = linkedin
                    if (github.isNotBlank()) linksMap["github"] = github
                    if (twitter.isNotBlank()) linksMap["twitter"] = twitter

                    viewModel.updateProfile(
                        UserProfileUpdateDto(
                            full_name = fullName,
                            bio = bio,
                            role = role,
                            organization = organization,
                            social_links = linksMap,
                            interests = interestsList
                        )
                    ) { success ->
                        isSaving = false
                        if (success) {
                            onBack()
                        } else {
                            validationError = "Failed to update profile. Please verify your connection."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextH, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Divider,
                focusedBorderColor = AccentIndigo,
                unfocusedContainerColor = CardBg,
                focusedContainerColor = CardBg
            ),
            singleLine = singleLine,
            minLines = minLines
        )
    }
}

// Helper to handle image uploading content conversions safely
private fun createMultipartBodyPart(context: android.content.Context, uri: android.net.Uri): okhttp3.MultipartBody.Part? {
    return try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("profile_img", ".jpg", context.cacheDir)
        tempFile.deleteOnExit()
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", tempFile.name, requestBody)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
