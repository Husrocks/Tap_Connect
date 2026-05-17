package com.tapconnect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapconnect.ui.theme.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onEditClick: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Header Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentIndigo)
                .padding(top = 40.dp, bottom = 24.dp)
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val imageUrl = (uiState as? ProfileUiState.Success)?.profile?.profile_image_url
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                when (val state = uiState) {
                    is ProfileUiState.Success -> {
                        Text(state.profile.full_name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(state.profile.role ?: "Professional", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    else -> {
                        Text("Loading...", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // --- Content Section ---
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentIndigo)
                }
            }
            is ProfileUiState.Error -> {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = AccentRed)
                }
            }
            is ProfileUiState.Updating -> {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentIndigo)
                }
            }
            is ProfileUiState.Success -> {
                val profile = state.profile
                
                Column(modifier = Modifier.padding(20.dp)) {
                    // Bio
                    ProfileSection(title = "About Me", icon = Icons.Rounded.Info) {
                        Text(
                            profile.bio ?: "No bio added yet.",
                            fontSize = 15.sp,
                            color = TextSub,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Organization
                    ProfileSection(title = "Organization", icon = Icons.Rounded.Business) {
                        Text(
                            profile.organization ?: "Independent",
                            fontSize = 15.sp,
                            color = TextH,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Interests
                    ProfileSection(title = "Interests", icon = Icons.Rounded.AutoAwesome) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (profile.interests.isEmpty()) {
                                Text("No interests listed.", fontSize = 14.sp, color = TextSub)
                            } else {
                                profile.interests.forEach { interest ->
                                    InterestTag(interest)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contact info
                    ProfileSection(title = "Contact Info", icon = Icons.Rounded.ContactPage) {
                        Column {
                            ContactItem(
                                icon = { SocialIcon("email") },
                                label = "Email",
                                value = profile.email
                            )
                            profile.social_links.forEach { (platform, link) ->
                                ContactItem(
                                    icon = { SocialIcon(platform) },
                                    label = platform.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    value = link
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Edit Button
                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.fillMaxWidth().height(50.dp),

                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentIndigo)
                    ) {
                        Text("Edit Profile", color = AccentIndigo, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Logout Button
                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed.copy(alpha = 0.15f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Logout, contentDescription = null, tint = AccentRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out", color = AccentRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextH)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun InterestTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AccentLight)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp, color = AccentIndigo, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ContactItem(icon: @Composable () -> Unit, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppBg.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextSub, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, color = TextH, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BrandIconCanvas(paths: List<String>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        withTransform({
            scale(scaleX, scaleY, pivot = androidx.compose.ui.geometry.Offset.Zero)
        }) {
            paths.forEach { pathStr ->
                try {
                    val path = PathParser().parsePathString(pathStr).toPath()
                    drawPath(path, color)
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
    }
}

@Composable
fun SocialIcon(platform: String) {
    val cleanPlatform = platform.trim().lowercase()
    val gradientColors = when {
        cleanPlatform == "email" -> listOf(AccentIndigo, AccentIndigo.copy(alpha = 0.8f))
        cleanPlatform.contains("linkedin") -> listOf(Color(0xFF0077B5), Color(0xFF0A66C2))
        cleanPlatform.contains("github") -> listOf(Color(0xFF24292F), Color(0xFF181717))
        cleanPlatform.contains("instagram") -> listOf(Color(0xFFF91880), Color(0xFFE50914), Color(0xFFFFD400))
        cleanPlatform.contains("twitter") || cleanPlatform == "x" -> listOf(Color(0xFF2d2d2d), Color(0xFF000000))
        cleanPlatform.contains("facebook") -> listOf(Color(0xFF1877F2), Color(0xFF3B5998))
        else -> listOf(Color(0xFF00A896), Color(0xFF028090))
    }

    val paths = when {
        cleanPlatform == "email" -> listOf("M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z")
        cleanPlatform.contains("linkedin") -> listOf("M8 5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z", "M9 9h3v1.5h.05c.42-.8 1.43-1.64 2.95-1.64 3.16 0 3.75 2.1 3.75 4.83V20h-3v-4.75c0-1.13-.02-2.58-1.57-2.58-1.57 0-1.81 1.23-1.81 2.5V20H9V9z M4 9h3v11H4V9z")
        cleanPlatform.contains("github") -> listOf("M12 2C6.477 2 2 6.477 2 12c0 4.42 2.865 8.166 6.839 9.489.5.092.682-.217.682-.482 0-.237-.008-.866-.013-1.7-2.782.603-3.369-1.34-3.369-1.34-.454-1.156-1.11-1.464-1.11-1.464-.908-.62.069-.608.069-.608 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.831.092-.646.35-1.086.636-1.336-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.025A9.564 9.564 0 0 1 12 6.844c.85.004 1.705.115 2.504.337 1.909-1.294 2.747-1.025 2.747-1.025.546 1.377.203 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.743 0 .267.18.578.688.48C19.137 20.162 22 16.418 22 12c0-5.523-4.477-10-10-10z")
        cleanPlatform.contains("instagram") -> listOf("M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z", "M17.5 6.5h.01", "M2 17.5c0 2.485 2.015 4.5 4.5 4.5h11c2.485 0 4.5-2.015 4.5-4.5v-11C22 4.015 19.985 2 17.5 2h-11C4.015 2 2 4.015 2 6.5v11z")
        cleanPlatform.contains("twitter") || cleanPlatform == "x" -> listOf("M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z")
        cleanPlatform.contains("facebook") -> listOf("M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z")
        else -> listOf("M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z")
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        BrandIconCanvas(
            paths = paths,
            color = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
