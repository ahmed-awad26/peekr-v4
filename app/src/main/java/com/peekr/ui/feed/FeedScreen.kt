package com.peekr.ui.feed

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.peekr.core.strings.LocalStrings
import com.peekr.data.local.entities.PostEntity
import com.peekr.ui.archive.ArchiveViewModel
import com.peekr.ui.archive.SavePostDialog
import com.peekr.ui.feed.components.PostCard

data class PlatformTab(
    val id: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    feedViewModel: FeedViewModel = hiltViewModel(),
    archiveViewModel: ArchiveViewModel = hiltViewModel()
) {
    val uiState by feedViewModel.uiState.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()
    val s = LocalStrings.current
    var postToSave by remember { mutableStateOf<PostEntity?>(null) }
    val listState = rememberLazyListState()

    val platformTabs = listOf(
        PlatformTab("all",      Icons.Default.Apps,      MaterialTheme.colorScheme.primary),
        PlatformTab("youtube",  Icons.Default.PlayCircle,    Color(0xFFFF0000)),
        PlatformTab("telegram", Icons.Default.Send,          Color(0xFF0088CC)),
        PlatformTab("whatsapp", Icons.Default.Chat,          Color(0xFF25D366)),
        PlatformTab("facebook", Icons.Default.Facebook,      Color(0xFF1877F2)),
        PlatformTab("rss",      Icons.Default.RssFeed,       Color(0xFFFF6600)),
    )

    val platformNames = mapOf(
        "all" to s.allPlatforms,
        "youtube" to "يوتيوب",
        "telegram" to "تليجرام",
        "whatsapp" to "واتساب",
        "facebook" to "فيسبوك",
        "rss" to "RSS"
    )

    Scaffold(
        topBar = {
            // Modern gradient top bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            s.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                        if (uiState.unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${uiState.unreadCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    // زرار البحث
                    IconButton(onClick = { /* TODO: search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                    // زرار Sync
                    AnimatedContent(
                        targetState = uiState.isSyncing,
                        label = "sync_anim"
                    ) { syncing ->
                        if (syncing) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(onClick = { feedViewModel.syncAll() }) {
                                Icon(Icons.Default.Refresh, contentDescription = s.refreshNow)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ==============================
            // Platform filter pills (modern style)
            // ==============================
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(platformTabs) { tab ->
                    val selected = uiState.selectedPlatform == tab.id
                    PlatformPill(
                        label = platformNames[tab.id] ?: tab.id,
                        icon = tab.icon,
                        color = tab.color,
                        selected = selected,
                        onClick = { feedViewModel.selectPlatform(tab.id) }
                    )
                }
            }

            // ==============================
            // Error banner
            // ==============================
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning, null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            IconButton(
                                onClick = { feedViewModel.clearError() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close, null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // ==============================
            // Content
            // ==============================
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                            Text(
                                s.syncInProgress,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.posts.isEmpty() -> {
                    EmptyFeedView(
                        onRefresh = { feedViewModel.syncAll() },
                        onGoToSettings = { navController.navigate("settings") },
                        strings = s
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp,
                            top = 4.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stats header bar
                        item {
                            StatsBar(
                                total = uiState.posts.size,
                                unread = uiState.unreadCount,
                                platform = uiState.selectedPlatform,
                                platformNames = platformNames
                            )
                        }

                        items(uiState.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onSaveClick = { postToSave = it }
                            )
                        }
                    }
                }
            }
        }
    }

    postToSave?.let { post ->
        SavePostDialog(
            post = post,
            categories = archiveState.categories,
            onSave = { categoryId, note ->
                archiveViewModel.savePost(post, categoryId, note)
                postToSave = null
            },
            onDismiss = { postToSave = null }
        )
    }
}

// ==============================
// Platform pill chip
// ==============================
@Composable
private fun PlatformPill(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) color else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = bgColor,
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(16.dp))
            Text(
                label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ==============================
// Stats bar
// ==============================
@Composable
private fun StatsBar(
    total: Int,
    unread: Int,
    platform: String,
    platformNames: Map<String, String>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${platformNames[platform] ?: platform} · $total منشور",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (unread > 0) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "$unread غير مقروء",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ==============================
// Empty state — modern design
// ==============================
@Composable
private fun EmptyFeedView(
    onRefresh: () -> Unit,
    onGoToSettings: () -> Unit,
    strings: com.peekr.core.strings.AppStrings
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                strings.noContentYet,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                strings.connectAccountsOrRefresh,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onGoToSettings) {
                    Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.settings)
                }
                Button(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.refreshNow)
                }
            }
        }
    }
}
