package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
        "Pakistan",
        "India / Bollywood",
        "Turkey",
        "Chinese Hindi Dubbed",
        "Korean Hindi Dubbed",
        "USA / International",
        "Cartoons"
    )
    var selectedCategory by remember { mutableStateOf("Pakistan") }

    var allChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var backPressedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        isLoading = true
        allChannels = ChannelRepository.fetchAllChannels()
        isLoading = false
    }

    val displayedChannels = remember(selectedCategory, allChannels) {
        allChannels.filter { it.category.equals(selectedCategory, ignoreCase = true) || it.country.equals(selectedCategory, ignoreCase = true) }
    }

    // Precise Back Navigation Logic (Steps 2, 3, 4)
    BackHandler {
        if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
            // STEP 2: Scrolled down in grid -> scroll back to top
            coroutineScope.launch {
                gridState.animateScrollToItem(0)
            }
        } else if (selectedCategory != "Pakistan") {
            // STEP 3: At top of non-Pakistan tab -> switch back to "Pakistan"
            selectedCategory = "Pakistan"
            coroutineScope.launch {
                gridState.scrollToItem(0)
            }
        } else {
            // STEP 4: At top of "Pakistan" tab -> double back press to exit
            val currentTime = System.currentTimeMillis()
            if (currentTime - backPressedTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressedTime = currentTime
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                    onClick = {
                        selectedCategory = cat
                        coroutineScope.launch {
                            gridState.scrollToItem(0)
                        }
                    },
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
                    Text(text = "Loading Live TV Channels...", color = TextSecondary, fontSize = 13.sp)
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
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(displayedChannels, key = { it.id }) { channel ->
                        ChannelCard(
                            channel = channel,
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
