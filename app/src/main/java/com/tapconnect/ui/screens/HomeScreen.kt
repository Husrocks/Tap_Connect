package com.tapconnect.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapconnect.ui.theme.*

@Composable
fun HomeScreen(viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val isLive by viewModel.isLive.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val incomingRequest by viewModel.incomingRequest.collectAsState()
    val nfcSimulating by viewModel.nfcSimulating.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBg)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Modern Premium Brand Header
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(
                    text = "Tap Connect",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentIndigo,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Instant Professional Networking",
                    fontSize = 14.sp,
                    color = TextSub,
                    fontWeight = FontWeight.Medium
                )
            }

            // 1. Status Section
            StatusCard(
                isLive = isLive,
                onToggle = { viewModel.toggleLiveStatus() }
            )

            // 2. Simulate Hardware Section
            SimulateHardwareSection(
                isSimulating = nfcSimulating,
                onSimulate = { viewModel.simulateNfcTap() }
            )

            // 3. My Stats Section
            MyStatsSection(
                connections = stats.connectionsCount
            )
        }

        // Real-time Connection Request Popup
        AnimatedVisibility(
            visible = incomingRequest is IncomingRequest.NewRequest,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (incomingRequest is IncomingRequest.NewRequest) {
                val request = incomingRequest as IncomingRequest.NewRequest
                ConnectionRequestPopup(
                    peerName = request.peerName,
                    onAccept = { viewModel.acceptIncomingRequest() },
                    onDecline = { viewModel.declineIncomingRequest() }
                )
            }
        }
    }
}

@Composable
private fun StatusCard(isLive: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ONLINE indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                val infiniteTransition = rememberInfiniteTransition(label = "AntennaPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseAlpha"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(if (isLive) pulseScale else 1f)
                        .clip(CircleShape)
                        .background((if (isLive) AccentGreen else Color.Gray).copy(alpha = if (isLive) pulseAlpha else 1f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLive) "ONLINE" else "OFFLINE",
                    color = if (isLive) AccentGreen else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Large Circular Power Button
            Button(
                onClick = onToggle,
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentIndigo.copy(alpha = 0.1f),
                    contentColor = AccentIndigo
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status Toggle Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = if (isLive) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    tint = TextSub,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLive) "You are live. Tap to go hidden." else "You are hidden. Tap to go live.",
                    color = TextSub,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SimulateHardwareSection(isSimulating: Boolean, onSimulate: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Simulate Hardware",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextH
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Since this is a dev build, use this to simulate an NFC hardware tap between two devices.",
            color = TextSub,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        
        // Antenna wave animations
        val infiniteTransition = rememberInfiniteTransition(label = "AntennaSignal")
        val signalScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseOutQuad),
                repeatMode = RepeatMode.Restart
            ),
            label = "SignalScale"
        )
        val signalAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseOutQuad),
                repeatMode = RepeatMode.Restart
            ),
            label = "SignalAlpha"
        )

        Button(
            onClick = onSimulate,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSimulating) AccentIndigo.copy(alpha = 0.8f) else Color(0xFF2D2E32)
            ),
            enabled = !isSimulating
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                    if (isSimulating) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .scale(signalScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = signalAlpha * 0.5f))
                        )
                    }
                    Icon(
                        Icons.Rounded.SettingsInputAntenna,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isSimulating) "Simulating Tapping..." else "Simulate NFC Tap",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MyStatsSection(connections: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "My Stats",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextH
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatCard(value = connections.toString(), label = "Connections Established", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentIndigo
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextSub,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ConnectionRequestPopup(peerName: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = AccentIndigo)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "New Connection Request",
                        fontSize = 14.sp,
                        color = TextSub,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$peerName wants to connect",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextH
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Decline", color = TextSub)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
                ) {
                    Text("Accept")
                }
            }
        }
    }
}
