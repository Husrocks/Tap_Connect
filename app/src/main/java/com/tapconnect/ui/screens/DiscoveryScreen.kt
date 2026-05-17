package com.tapconnect.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.tapconnect.data.remote.UserProfileDto
import com.tapconnect.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiscoveryScreen(viewModel: DiscoveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val discoveredUsers by viewModel.discoveredUsers.collectAsState()
    val discoveredProfiles by viewModel.discoveredProfiles.collectAsState()
    val isSearching by viewModel.isScanning.collectAsState()
    var selectedProfile by remember { mutableStateOf<UserProfileDto?>(null) }
    val context = LocalContext.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            handleRadarScanActivation(context, viewModel)
        } else {
            Toast.makeText(context, "Permissions are required for Radar scanning!", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section
        HeaderSection()

        Spacer(modifier = Modifier.height(60.dp))

        // Radar Section
        RadarView(
            isSearching = isSearching,
            profiles = discoveredProfiles.values.toList(),
            onProfileClick = { selectedProfile = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Stats Section
        StatsSection(foundCount = discoveredUsers.size)

        Spacer(modifier = Modifier.weight(1f))

        // Action Button
        ToggleButton(
            isSearching = isSearching,
            onClick = {
                if (isSearching) {
                    viewModel.stopDiscovery()
                } else {
                    val hasPermissions = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPermissions) {
                        handleRadarScanActivation(context, viewModel)
                    } else {
                        permissionsLauncher.launch(permissionsToRequest)
                    }
                }
            }
        )
    }

    // Profile Detail Overlay
    selectedProfile?.let { profile ->
        ProfileDetailDialog(
            profile = profile,
            onDismiss = { selectedProfile = null },
            viewModel = viewModel
        )
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier.padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Networking Radar",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextH,
            letterSpacing = (-0.5).sp
        )
        Text(
            "Scanning for nearby professionals",
            fontSize = 15.sp,
            color = TextSub,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun RadarView(
    isSearching: Boolean,
    profiles: List<UserProfileDto>,
    onProfileClick: (UserProfileDto) -> Unit
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(320.dp)) {
        if (isSearching) {
            RadarRings()
            RotatingSweep()
        }

        // Central Orb
        CentralOrb(isSearching)

        // Peer Bubbles
        AnimatedVisibility(
            visible = isSearching,
            enter = fadeIn() + expandIn(),
            exit = fadeOut() + shrinkOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                profiles.forEachIndexed { index, profile ->
                    PeerBubble(
                        index = index,
                        profile = profile,
                        onClick = { onProfileClick(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RadarRings() {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarRings")
    val rings = listOf(0.4f, 0.7f, 1.0f)
    
    rings.forEach { delayFactor ->
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, delayMillis = (delayFactor * 1000).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "PulseScale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, delayMillis = (delayFactor * 1000).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "PulseAlpha"
        )
        
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(pulseScale)
                .border(1.5.dp, AccentIndigo.copy(alpha = pulseAlpha), CircleShape)
        )
    }
}

@Composable
private fun RotatingSweep() {
    val infiniteTransition = rememberInfiniteTransition(label = "Sweep")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Canvas(modifier = Modifier.size(280.dp).scale(1.1f).graphicsLayer { rotationZ = rotation }) {
        val sweepGradient = Brush.sweepGradient(
            0f to Color.Transparent,
            0.5f to AccentIndigo.copy(alpha = 0.1f),
            1f to AccentIndigo.copy(alpha = 0.3f)
        )
        drawCircle(
            brush = sweepGradient,
            radius = size.minDimension / 2,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun CentralOrb(isSearching: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "Orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSearching) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSearching) 1200 else 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbScale"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(AccentIndigo, AccentIndigo.copy(alpha = 0.8f))
                )
            )
            .border(4.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        ConnectRadarIcon(
            modifier = Modifier.size(54.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun ConnectRadarIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val strokeWidth = 3.5.dp.toPx()

        // 1. Draw Left Person Figure
        // Head
        drawCircle(
            color = tint,
            radius = width * 0.08f,
            center = Offset(center.x - width * 0.32f, center.y - height * 0.15f)
        )
        // Torso
        val leftTorsoPath = Path().apply {
            moveTo(center.x - width * 0.44f, center.y + height * 0.22f)
            cubicTo(
                center.x - width * 0.44f, center.y + height * 0.02f,
                center.x - width * 0.20f, center.y + height * 0.02f,
                center.x - width * 0.20f, center.y + height * 0.08f
            )
        }
        drawPath(
            path = leftTorsoPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Draw Right Person Figure
        // Head
        drawCircle(
            color = tint,
            radius = width * 0.08f,
            center = Offset(center.x + width * 0.32f, center.y - height * 0.15f)
        )
        // Torso
        val rightTorsoPath = Path().apply {
            moveTo(center.x + width * 0.44f, center.y + height * 0.22f)
            cubicTo(
                center.x + width * 0.44f, center.y + height * 0.02f,
                center.x + width * 0.20f, center.y + height * 0.02f,
                center.x + width * 0.20f, center.y + height * 0.08f
            )
        }
        drawPath(
            path = rightTorsoPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Draw Center Signals (Sharing Waves)
        // Central core dot
        drawCircle(
            color = tint,
            radius = width * 0.045f,
            center = center
        )

        // Inner Wave Left & Right
        val r1 = width * 0.12f
        drawArc(
            color = tint,
            startAngle = 125f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - r1, center.y - r1),
            size = Size(r1 * 2f, r1 * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = -55f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - r1, center.y - r1),
            size = Size(r1 * 2f, r1 * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Outer Wave Left & Right
        val r2 = width * 0.22f
        drawArc(
            color = tint,
            startAngle = 125f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - r2, center.y - r2),
            size = Size(r2 * 2f, r2 * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = -55f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(center.x - r2, center.y - r2),
            size = Size(r2 * 2f, r2 * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PeerBubble(index: Int, profile: UserProfileDto, onClick: () -> Unit) {
    // 1. Fixed angular positioning instead of orbiting movement
    val baseAngle = (index * 72f + 35f) % 360f
    val angleRad = baseAngle * (Math.PI / 180f).toFloat()
    
    // Distribute radii dynamically so bubbles don't stack on top of each other
    val radius = when (index % 3) {
        0 -> 70.dp
        1 -> 105.dp
        else -> 140.dp
    }
    
    val xOffset = (cos(angleRad.toDouble()) * radius.value).dp
    val yOffset = (sin(angleRad.toDouble()) * radius.value).dp

    val animTransition = rememberInfiniteTransition(label = "Animations")

    // Sonar radar pulse halo animations
    val sonarScale by animTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarScale"
    )
    val sonarAlpha by animTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SonarAlpha"
    )

    // Gentle micro-breathing scale animation using GPU graphicsLayer for extreme smooth performance
    val breathingScale by animTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600 + (index * 250), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    // Premium micro-animations: gentle organic floating / drifting hover effect
    val floatX by animTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2300 + (index * 300), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatX"
    )
    val floatY by animTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2900 + (index * 200), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FloatY"
    )

    val finalX = xOffset + floatX.dp
    val finalY = yOffset + floatY.dp

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 150L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        modifier = Modifier.offset(x = finalX, y = finalY)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // 1. Soft Sonar Radar ping expanding behind the bubble
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(
                        scaleX = sonarScale,
                        scaleY = sonarScale,
                        alpha = sonarAlpha
                    )
                    .border(2.dp, AccentIndigo.copy(alpha = 0.6f), CircleShape)
            )

            // 2. The main Interactive Profile Bubble
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer(
                        scaleX = breathingScale,
                        scaleY = breathingScale
                    )
                    .clip(CircleShape)
                    .background(CardBg)
                    .border(2.5.dp, AccentIndigo, CircleShape)
                    .clickable { onClick() }
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!profile.profile_image_url.isNullOrEmpty()) {
                    AsyncImage(
                        model = profile.profile_image_url,
                        contentDescription = profile.full_name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    val initials = profile.full_name.split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
                    Text(
                        text = initials,
                        color = AccentIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSection(foundCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(AccentLight, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AccentGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            "$foundCount Peers Found",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentIndigo
        )
    }
}

@Composable
private fun ToggleButton(isSearching: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp)
            .height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSearching) AccentRed else AccentIndigo
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text(
            if (isSearching) "Stop Radar" else "Start Radar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileDetailDialog(
    profile: UserProfileDto,
    onDismiss: () -> Unit,
    viewModel: DiscoveryViewModel
) {
    val statusMap by viewModel.connectionStatus.collectAsState()
    val status = statusMap[profile.user_id.toString()] ?: ConnectionStatus.Idle
    var whereMet by remember(profile) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            ProfileHeader(profile)
        },
        text = {
            ProfileContent(profile, whereMet, onWhereMetChange = { whereMet = it })
        },
        confirmButton = {
            ConnectionAction(
                status = status,
                onConnect = { viewModel.sendConnectionRequest(profile.user_id.toString(), whereMet) }
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Text("Maybe Later", color = TextSub, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun ProfileHeader(profile: UserProfileDto) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(AccentLight)
                .border(3.dp, AccentIndigo, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!profile.profile_image_url.isNullOrEmpty()) {
                AsyncImage(
                    model = profile.profile_image_url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                val initials = profile.full_name.split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
                Text(
                    text = initials,
                    color = AccentIndigo,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = profile.full_name,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextH
        )

        if (!profile.role.isNullOrEmpty()) {
            Text(
                text = "${profile.role}${if (!profile.organization.isNullOrEmpty()) " @ ${profile.organization}" else ""}",
                fontSize = 15.sp,
                color = TextSub,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfileDto,
    whereMet: String,
    onWhereMetChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!profile.bio.isNullOrEmpty()) {
            Text(
                text = profile.bio,
                fontSize = 14.sp,
                color = TextH,
                lineHeight = 20.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        if (profile.interests.isNotEmpty()) {
            SectionLabel("Interests")
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile.interests.take(3).forEach { interest ->
                    InterestTag(interest)
                }
            }
        }

        if (profile.social_links.isNotEmpty()) {
            SectionLabel("Social Presence")
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                profile.social_links.forEach { (platform, url) ->
                    Box(modifier = Modifier.clickable { 
                        try {
                            val cleanUrl = if (platform.equals("email", ignoreCase = true)) {
                                if (url.startsWith("mailto:")) url else "mailto:$url"
                            } else {
                                if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                            }
                            uriHandler.openUri(cleanUrl)
                        } catch (e: Exception) {}
                    }) {
                        SocialIcon(platform = platform)
                    }
                }
            }
        }

        SectionLabel("Context (Where did you meet?)")
        OutlinedTextField(
            value = whereMet,
            onValueChange = onWhereMetChange,
            placeholder = { Text("e.g., Tech Meetup, Coffee Shop", fontSize = 14.sp, color = TextSub) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentIndigo,
                unfocusedBorderColor = Divider,
                focusedContainerColor = AppBg.copy(alpha = 0.5f),
                unfocusedContainerColor = AppBg.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        color = TextSub,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
    )
}

@Composable
private fun ConnectionAction(status: ConnectionStatus, onConnect: () -> Unit) {
    Button(
        onClick = onConnect,
        colors = ButtonDefaults.buttonColors(
            containerColor = when (status) {
                is ConnectionStatus.Connected -> AccentGreen
                is ConnectionStatus.Error -> AccentRed
                else -> AccentIndigo
            }
        ),
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = (status is ConnectionStatus.Idle || status is ConnectionStatus.Error)
    ) {
        when (status) {
            is ConnectionStatus.Sending -> CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            is ConnectionStatus.Connected -> Text("✓ Request Sent!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            is ConnectionStatus.Error -> Text("Retry Connection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            else -> Text("Connect Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private fun handleRadarScanActivation(context: Context, viewModel: DiscoveryViewModel) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
    val isBtOn = bluetoothManager.adapter?.isEnabled == true
    
    if (!isBtOn) {
        try {
            val enableBtIntent = Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(enableBtIntent)
        } catch (e: SecurityException) {
            Toast.makeText(context, "Please enable Bluetooth in Quick Settings!", Toast.LENGTH_LONG).show()
        }
        return
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    val isLocationOn = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

    if (!isLocationOn) {
        try {
            val locationIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(locationIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please enable GPS manually!", Toast.LENGTH_LONG).show()
        }
        return
    }

    viewModel.startDiscovery()
}
