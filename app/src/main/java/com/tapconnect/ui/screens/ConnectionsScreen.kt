package com.tapconnect.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapconnect.ui.theme.*

data class ConnectionItem(
    val id: String,
    val name: String,
    val role: String,
    val organization: String,
    val date: String,
    val aiSummary: String,
    val whereMet: String?
)

data class PendingRequestItem(
    val connectionId: String,
    val peerId: String,
    val name: String,
    val role: String,
    val organization: String,
    val aiSummary: String,
    val date: String,
    val whereMet: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(viewModel: ConnectionsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val processingRequests by viewModel.processingRequests.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("My Connections", "Pending Requests")
    
    var selectedConnection by remember { mutableStateOf<ConnectionItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchConnectionsAndRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Networking", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg)
            )
        },
        containerColor = AppBg
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            val pendingCount = if (uiState is ConnectionsUiState.Success) {
                (uiState as ConnectionsUiState.Success).pendingRequests.size
            } else 0

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = AppBg,
                contentColor = AccentIndigo,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = AccentIndigo
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                                if (index == 1 && pendingCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = AccentRed) {
                                        Text(pendingCount.toString(), color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ConnectionsContent"
            ) { state ->
                when (state) {
                    is ConnectionsUiState.Loading -> {
                        ShimmerList()
                    }
                    is ConnectionsUiState.Error -> {
                        ErrorState(state.message) { viewModel.fetchConnectionsAndRequests() }
                    }
                    is ConnectionsUiState.Success -> {
                        if (selectedTabIndex == 0) {
                            ConnectionsList(
                                connections = state.connections,
                                searchQuery = searchQuery,
                                onSearchChange = { searchQuery = it },
                                onConnectionClick = { selectedConnection = it }
                            )
                        } else {
                            PendingList(state.pendingRequests, processingRequests, viewModel)
                        }
                    }
                }
            }
        }
    }

    // Show premium Profile Details overlay when clicking a connection card
    selectedConnection?.let { connection ->
        ConnectionDetailDialog(
            connection = connection,
            onDismiss = { selectedConnection = null }
        )
    }
}

@Composable
private fun ConnectionsList(
    connections: List<ConnectionItem>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onConnectionClick: (ConnectionItem) -> Unit
) {
    val filtered = connections.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.role.contains(searchQuery, ignoreCase = true) 
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search connections...", color = TextSub) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSub) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentIndigo,
                unfocusedBorderColor = Divider,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            ),
            singleLine = true
        )

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.PeopleOutline,
                title = if (searchQuery.isEmpty()) "No connections yet" else "No matches found",
                subtitle = if (searchQuery.isEmpty()) "Start scanning with radar to build your network!" else "Try a different search term"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    ConnectionCard(item, onClick = { onConnectionClick(item) })
                }
            }
        }
    }
}

@Composable
private fun PendingList(
    requests: List<PendingRequestItem>,
    processing: Map<String, String>,
    viewModel: ConnectionsViewModel
) {
    if (requests.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.MailOutline,
            title = "All caught up!",
            subtitle = "You have no pending connection requests."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(requests, key = { it.connectionId }) { item ->
                PendingRequestCard(
                    item = item,
                    processingStatus = processing[item.connectionId],
                    onAccept = { viewModel.acceptRequest(item.connectionId) { _, _ -> } },
                    onDecline = { viewModel.declineRequest(item.connectionId) { _, _ -> } }
                )
            }
        }
    }
}

@Composable
private fun ConnectionCard(item: ConnectionItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(item.name)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextH)
                Text("${item.role} @ ${item.organization}", fontSize = 14.sp, color = TextSub)
                if (!item.whereMet.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = AccentIndigo,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(item.whereMet, fontSize = 12.sp, color = AccentIndigo, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Divider)
            }
        }
    }
}

@Composable
private fun ConnectionDetailDialog(
    connection: ConnectionItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentIndigo, AccentIndigo.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = connection.name.split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = connection.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextH,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${connection.role} @ ${connection.organization}",
                    fontSize = 15.sp,
                    color = TextSub,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Text(
                    text = "Connected on ${connection.date}",
                    fontSize = 12.sp,
                    color = TextSub,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!connection.whereMet.isNullOrBlank()) {
                    Text(
                        text = "WHERE WE MET",
                        fontSize = 11.sp,
                        color = TextSub,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentLight, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Meeting Location",
                            tint = AccentIndigo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = connection.whereMet,
                            fontSize = 14.sp,
                            color = AccentIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (!connection.aiSummary.isNullOrBlank()) {
                    Text(
                        text = "AI INSIGHT & INTERESTS",
                        fontSize = 11.sp,
                        color = TextSub,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppBg.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = connection.aiSummary,
                            fontSize = 14.sp,
                            color = TextH,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo)
            ) {
                Text("Close Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    )
}


@Composable
private fun PendingRequestCard(
    item: PendingRequestItem,
    processingStatus: String?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(item.name, size = 50.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextH)
                    Text(item.role, fontSize = 13.sp, color = TextSub)
                }
                Text("Recently", fontSize = 12.sp, color = TextSub)
            }
            
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .background(AccentLight, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = item.aiSummary,
                    fontSize = 13.sp,
                    color = TextH,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = processingStatus == null
                ) {
                    if (processingStatus == "declining") {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Decline", color = TextSub)
                    }
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                    enabled = processingStatus == null
                ) {
                    if (processingStatus == "accepting") {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Accept")
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(name: String, size: androidx.compose.ui.unit.Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AccentLight),
        contentAlignment = Alignment.Center
    ) {
        val initials = name.split(" ").take(2).map { it.take(1) }.joinToString("").uppercase()
        Text(initials, color = AccentIndigo, fontWeight = FontWeight.Black, fontSize = (size.value * 0.4).sp)
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = Divider)
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextH)
        Spacer(modifier = Modifier.height(8.dp))
        Text(subtitle, fontSize = 14.sp, color = TextSub, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, size = 60.dp, tint = AccentRed)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Oops! Something went wrong", fontWeight = FontWeight.Bold, color = TextH)
        Text(message, color = TextSub, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("Retry")
        }
    }
}

@Composable
private fun ShimmerList() {
    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Divider.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}
