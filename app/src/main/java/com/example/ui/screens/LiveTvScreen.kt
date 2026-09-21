package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Channel
import com.example.data.ChannelRepository
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun LiveTvScreen(
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        "All",
        "Pakistan",
        "India",
        "Turkey",
        "Chinese Hindi Dubbed",
        "Korean Hindi Dubbed",
        "USA / International",
        "Cartoons",
        "Favorites"
    )
    var selectedCategory by remember { mutableStateOf("All") }

    var allChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Persistent Favorites Setup
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("supertv_favorites", android.content.Context.MODE_PRIVATE) }
    var favoritedIds by remember {
        mutableStateOf(sharedPreferences.getStringSet("fav_ids", emptySet()) ?: emptySet())
    }

    val toggleFavorite: (Channel) -> Unit = { channel ->
        val updated = if (favoritedIds.contains(channel.id)) {
            favoritedIds - channel.id
        } else {
            favoritedIds + channel.id
        }
        favoritedIds = updated
        sharedPreferences.edit().putStringSet("fav_ids", updated).apply()
    }

    // Fetch channels from M3U sources on launch
    LaunchedEffect(Unit) {
        isLoading = true
        val fetched = ChannelRepository.fetchAllChannels()
        allChannels = fetched
        isLoading = false
    }

    // Filter channels based on selected category tab
    val displayedChannels = remember(selectedCategory, allChannels, favoritedIds) {
        when (selectedCategory) {
            "All" -> allChannels
            "Favorites" -> allChannels.filter { favoritedIds.contains(it.id) }
            "Pakistan", "India", "Turkey", "USA / International", "Cartoons" -> {
                allChannels.filter { it.country.equals(selectedCategory, ignoreCase = true) || it.category.equals(selectedCategory, ignoreCase = true) }
            }
            "Chinese Hindi Dubbed", "Korean Hindi Dubbed" -> {
                allChannels.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
            else -> allChannels.filter { it.category.equals(selectedCategory, ignoreCase = true) || it.country.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Category Tabs
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            edgePadding = 16.dp,
            divider = {}
        ) {
            categories.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = {
                        Text(
                            text = cat,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedCategory == cat) RedAccent else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_$cat")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content Area / Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RedAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Loading Super TV Channels...", color = TextSecondary, fontSize = 13.sp)
                }
            } else if (displayedChannels.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No channels found in this category.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(displayedChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
                            isFavorited = favoritedIds.contains(channel.id),
                            onFavoriteClick = { toggleFavorite(channel) },
                            onClick = { onChannelSelected(channel) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    isFavorited: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    var imageFailed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick)
            .testTag("channel_card_${channel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Channel Logo or Initial Fallback Badge
            if (channel.logoUrl.isNotEmpty() && !imageFailed) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            } else {
                // Stylized Initial Fallback Badge so cards are never pitch-black
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(RedAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = channel.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Live Badge
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RedAccent)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Favorite Icon Button
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorited) RedAccent else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bottom info bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = channel.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.category,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = RedAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
