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
    val coroutineScope = rememberCoroutineScope()
    val mainTabs = listOf("LIVE TV", "MOVIES", "SERIES", "Favorites")
    var selectedMainTab by remember { mutableStateOf("LIVE TV") }

    val liveSubCategories = listOf("Pakistan", "India", "Turkey", "Cartoons", "All")
    val movieSubCategories = listOf("All Movies", "Hollywood", "Bollywood", "Pakistani")
    val seriesSubCategories = listOf("All Series", "Turkish Dramas", "Pakistani Dramas")

    var selectedLiveSubCategory by remember { mutableStateOf("Pakistan") }
    var selectedMovieSubCategory by remember { mutableStateOf("All Movies") }
    var selectedSeriesSubCategory by remember { mutableStateOf("All Series") }

    var allChannels by remember { mutableStateOf<List<Channel>>(ChannelRepository.initialChannels) }
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

    // Run health check & channel load on launch
    LaunchedEffect(Unit) {
        isLoading = true
        val verified = ChannelRepository.verifyAndGetActiveChannels()
        allChannels = verified
        isLoading = false
    }

    // Filter channels based on selected main tab and subcategory
    val displayedChannels = remember(selectedMainTab, selectedLiveSubCategory, selectedMovieSubCategory, selectedSeriesSubCategory, allChannels, favoritedIds) {
        when (selectedMainTab) {
            "LIVE TV" -> {
                if (selectedLiveSubCategory == "All") {
                    allChannels.filter { it.contentType == "LIVE" }
                } else {
                    allChannels.filter { it.contentType == "LIVE" && (it.country.equals(selectedLiveSubCategory, ignoreCase = true) || it.category.equals(selectedLiveSubCategory, ignoreCase = true)) }
                }
            }
            "MOVIES" -> {
                if (selectedMovieSubCategory == "All Movies") {
                    allChannels.filter { it.contentType == "MOVIE" }
                } else {
                    allChannels.filter { it.contentType == "MOVIE" && it.category.equals(selectedMovieSubCategory, ignoreCase = true) }
                }
            }
            "SERIES" -> {
                if (selectedSeriesSubCategory == "All Series") {
                    allChannels.filter { it.contentType == "SERIES" }
                } else {
                    allChannels.filter { it.contentType == "SERIES" && it.category.contains(selectedSeriesSubCategory, ignoreCase = true) }
                }
            }
            "Favorites" -> {
                allChannels.filter { favoritedIds.contains(it.id) }
            }
            else -> allChannels
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Tabs Row
        ScrollableTabRow(
            selectedTabIndex = mainTabs.indexOf(selectedMainTab),
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            edgePadding = 16.dp,
            divider = {}
        ) {
            mainTabs.forEach { tab ->
                Tab(
                    selected = selectedMainTab == tab,
                    onClick = { selectedMainTab = tab },
                    text = {
                        Text(
                            text = tab,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (selectedMainTab == tab) RedAccent else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_$tab")
                )
            }
        }

        // Sub-Category Chips Row for Live TV, Movies, or Series
        when (selectedMainTab) {
            "LIVE TV" -> {
                ScrollableTabRow(
                    selectedTabIndex = liveSubCategories.indexOf(selectedLiveSubCategory).coerceAtLeast(0),
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    liveSubCategories.forEach { sub ->
                        Tab(
                            selected = selectedLiveSubCategory == sub,
                            onClick = { selectedLiveSubCategory = sub },
                            text = {
                                Text(
                                    text = sub,
                                    fontSize = 12.sp,
                                    color = if (selectedLiveSubCategory == sub) Color.White else TextSecondary
                                )
                            }
                        )
                    }
                }
            }
            "MOVIES" -> {
                ScrollableTabRow(
                    selectedTabIndex = movieSubCategories.indexOf(selectedMovieSubCategory).coerceAtLeast(0),
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    movieSubCategories.forEach { sub ->
                        Tab(
                            selected = selectedMovieSubCategory == sub,
                            onClick = { selectedMovieSubCategory = sub },
                            text = {
                                Text(
                                    text = sub,
                                    fontSize = 12.sp,
                                    color = if (selectedMovieSubCategory == sub) Color.White else TextSecondary
                                )
                            }
                        )
                    }
                }
            }
            "SERIES" -> {
                ScrollableTabRow(
                    selectedTabIndex = seriesSubCategories.indexOf(selectedSeriesSubCategory).coerceAtLeast(0),
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    seriesSubCategories.forEach { sub ->
                        Tab(
                            selected = selectedSeriesSubCategory == sub,
                            onClick = { selectedSeriesSubCategory = sub },
                            text = {
                                Text(
                                    text = sub,
                                    fontSize = 12.sp,
                                    color = if (selectedSeriesSubCategory == sub) Color.White else TextSecondary
                                )
                            }
                        )
                    }
                }
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
                CircularProgressIndicator(color = RedAccent)
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
            // Channel Logo / Banner
            AsyncImage(
                model = channel.logoUrl.ifEmpty { "https://images.unsplash.com/photo-1518770660439-4636190af475?w=300" },
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            // Live Badge
            if (channel.contentType == "LIVE") {
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
