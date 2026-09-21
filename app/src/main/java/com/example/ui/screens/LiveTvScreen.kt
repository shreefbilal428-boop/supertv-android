package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.RedAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONObject

@Composable
fun LiveTvScreen(
    onChannelSelected: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    val mainTabs = listOf("LIVE TV", "MOVIES", "Favorites")
    var selectedMainTab by remember { mutableStateOf("LIVE TV") }

    val liveSubCategories = listOf("Pakistan", "India", "Cartoons", "Turkey", "China", "Korea", "USA / International")
    val movieSubCategories = listOf(
        "All Movies",
        "All Series",
        "Hollywood",
        "Chinese",
        "Korean",
        "Punjabi",
        "Pakistani",
        "Cartoons",
        "Turkish Dramas (Hindi/Urdu Dubbed)"
    )

    var selectedLiveSubCategory by remember { mutableStateOf("Pakistan") }
    var selectedMovieSubCategory by remember { mutableStateOf("All Movies") }

    val liveSubCategoryUrls = remember {
        mapOf(
            "Pakistan" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/countries/pk.m3u"),
            "India" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/countries/in.m3u"),
            "Cartoons" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/categories/animation.m3u"),
            "Turkey" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/countries/tr.m3u"),
            "China" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/countries/cn.m3u"),
            "Korea" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/countries/kr.m3u"),
            "USA / International" to listOf("https://iptv-org.github.io/iptv/index.m3u", "https://iptv-org.github.io/iptv/countries/us.m3u")
        )
    }

    val movieSubCategoryUrls = remember {
        mapOf(
            "All Movies" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "All Series" to "https://raw.githubusercontent.com/yurimv/iptv/main/playlist.m3u",
            "Hollywood" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "Chinese" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "Korean" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "Punjabi" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "Pakistani" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "Cartoons" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u",
            "Turkish Dramas (Hindi/Urdu Dubbed)" to "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u"
        )
    }

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

        if (updated.contains(channel.id)) {
            val json = JSONObject().apply {
                put("id", channel.id)
                put("name", channel.name)
                put("logoUrl", channel.logoUrl)
                put("streamUrl", channel.streamUrl)
                put("country", channel.country)
                put("category", channel.category)
                put("isLive", channel.isLive)
            }
            sharedPreferences.edit().putString("channel_${channel.id}", json.toString()).apply()
        } else {
            sharedPreferences.edit().remove("channel_${channel.id}").apply()
        }
    }

    val favoriteChannels = remember(favoritedIds) {
        favoritedIds.mapNotNull { id ->
            val jsonStr = sharedPreferences.getString("channel_$id", null)
            if (jsonStr != null) {
                try {
                    val json = JSONObject(jsonStr)
                    Channel(
                        id = json.getString("id"),
                        name = json.getString("name"),
                        logoUrl = json.getString("logoUrl"),
                        streamUrl = json.getString("streamUrl"),
                        country = json.getString("country"),
                        category = json.getString("category"),
                        isLive = json.getBoolean("isLive")
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }.sortedBy { it.name }
    }

    var dynamicLiveChannels by remember { mutableStateOf<Map<String, List<Channel>>>(emptyMap()) }
    var dynamicMoviesChannels by remember { mutableStateOf<Map<String, List<Channel>>>(emptyMap()) }
    var m3uCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(selectedMainTab) {
        if (selectedMainTab == "MOVIES") {
            selectedMovieSubCategory = "All Movies"
        }
    }

    LaunchedEffect(selectedMainTab, selectedLiveSubCategory, selectedMovieSubCategory, retryTrigger) {
        isError = false
        if (selectedMainTab == "LIVE TV") {
            val currentChannels = dynamicLiveChannels[selectedLiveSubCategory]
            if (currentChannels.isNullOrEmpty()) {
                isLoading = true
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val urls = liveSubCategoryUrls[selectedLiveSubCategory] ?: emptyList()
                    val allParsed = mutableListOf<Channel>()
                    val newCacheUpdates = mutableMapOf<String, String>()
                    var hasNetworkError = false

                    for (u in urls) {
                        try {
                            val text = if (m3uCache.containsKey(u)) {
                                m3uCache[u]!!
                            } else {
                                val url = java.net.URL(u)
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.connectTimeout = 10000
                                connection.readTimeout = 10000
                                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                val content = connection.inputStream.readBytes().toString(Charsets.UTF_8)
                                newCacheUpdates[u] = content
                                content
                            }
                            val parsed = parseM3U(text, selectedLiveSubCategory)
                            allParsed.addAll(parsed)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            hasNetworkError = true
                        }
                    }

                    if (newCacheUpdates.isNotEmpty()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            m3uCache = m3uCache + newCacheUpdates
                        }
                    }

                    val processed = filterLiveChannels(allParsed.distinctBy { it.streamUrl }, selectedLiveSubCategory)

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (hasNetworkError && processed.isEmpty()) {
                            // Fallback to sample data so UI never hangs
                            val fallback = com.example.data.SampleData.channels
                            dynamicLiveChannels = dynamicLiveChannels + (selectedLiveSubCategory to fallback)
                        } else if (processed.isEmpty()) {
                            isError = true
                        } else {
                            dynamicLiveChannels = dynamicLiveChannels + (selectedLiveSubCategory to processed)
                        }
                        isLoading = false
                    }
                }
            }
        } else if (selectedMainTab == "MOVIES") {
            val targetUrl = movieSubCategoryUrls[selectedMovieSubCategory] ?: "https://raw.githubusercontent.com/free-tv/iptv/master/playlist.m3u"
            val currentChannels = dynamicMoviesChannels[selectedMovieSubCategory]
            if (currentChannels.isNullOrEmpty()) {
                isLoading = true
                isError = false
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val allParsed = mutableListOf<Channel>()
                    var fetchedText: String? = null

                    // 1. Try Direct Fetch with 10s timeout
                    try {
                        val url = java.net.URL(targetUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                        fetchedText = connection.inputStream.readBytes().toString(Charsets.UTF_8)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 2. Try Fallback Proxy
                    if (fetchedText.isNullOrEmpty()) {
                        try {
                            val proxyUrl = "https://api.allorigins.win/raw?url=" + java.net.URLEncoder.encode(targetUrl, "UTF-8")
                            val url = java.net.URL(proxyUrl)
                            val connection = url.openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000
                            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                            fetchedText = connection.inputStream.readBytes().toString(Charsets.UTF_8)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (!fetchedText.isNullOrEmpty()) {
                        val parsed = parseM3U(fetchedText, selectedMovieSubCategory)
                        allParsed.addAll(parsed)
                    }

                    val processed = filterAndSortMovies(allParsed.distinctBy { it.streamUrl }, selectedMovieSubCategory)

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (processed.isEmpty()) {
                            // Smooth fallback to Live TV array items if Movies/Series request fails or times out
                            val fallbackLive = com.example.data.SampleData.channels
                            dynamicMoviesChannels = dynamicMoviesChannels + (selectedMovieSubCategory to fallbackLive)
                        } else {
                            dynamicMoviesChannels = dynamicMoviesChannels + (selectedMovieSubCategory to processed)
                        }
                        isLoading = false
                    }
                }
            }
        }
    }

    val displayChannels = remember(selectedMainTab, selectedLiveSubCategory, selectedMovieSubCategory, dynamicLiveChannels, dynamicMoviesChannels, favoriteChannels) {
        when (selectedMainTab) {
            "LIVE TV" -> dynamicLiveChannels[selectedLiveSubCategory] ?: emptyList()
            "MOVIES" -> dynamicMoviesChannels[selectedMovieSubCategory] ?: emptyList()
            else -> favoriteChannels
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Navigation (LIVE TV, MOVIES, Favorites)
        TabRow(
            selectedTabIndex = mainTabs.indexOf(selectedMainTab),
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            indicator = { tabPositions ->
                val currentTab = mainTabs.indexOf(selectedMainTab)
                if (currentTab in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                        color = RedAccent,
                        height = 3.dp
                    )
                }
            },
            divider = {}
        ) {
            mainTabs.forEach { tab ->
                val isSelected = selectedMainTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedMainTab = tab },
                    modifier = Modifier.testTag("main_tab_${tab.replace(" ", "_")}"),
                    text = {
                        Text(
                            text = tab.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) RedAccent else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Sub-Categories Horizontal Scroll Bar (LIVE TV)
        if (selectedMainTab == "LIVE TV") {
            ScrollableTabRow(
                selectedTabIndex = liveSubCategories.indexOf(selectedLiveSubCategory).coerceAtLeast(0),
                containerColor = DarkSurfaceVariant,
                contentColor = TextPrimary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    val currentTab = liveSubCategories.indexOf(selectedLiveSubCategory)
                    if (currentTab in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = RedAccent,
                            height = 2.dp
                        )
                    }
                },
                divider = {}
            ) {
                liveSubCategories.forEach { subCat ->
                    val isSelected = selectedLiveSubCategory == subCat
                    Tab(
                        selected = isSelected,
                        onClick = { selectedLiveSubCategory = subCat },
                        modifier = Modifier.testTag("sub_tab_${subCat.replace(" ", "_")}"),
                        text = {
                            Text(
                                text = subCat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    )
                }
            }
        } else if (selectedMainTab == "MOVIES") {
            // Sub-Categories Horizontal Scroll Bar (MOVIES)
            ScrollableTabRow(
                selectedTabIndex = movieSubCategories.indexOf(selectedMovieSubCategory).coerceAtLeast(0),
                containerColor = DarkSurfaceVariant,
                contentColor = TextPrimary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    val currentTab = movieSubCategories.indexOf(selectedMovieSubCategory)
                    if (currentTab in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                            color = RedAccent,
                            height = 2.dp
                        )
                    }
                },
                divider = {}
            ) {
                movieSubCategories.forEach { subCat ->
                    val isSelected = selectedMovieSubCategory == subCat
                    Tab(
                        selected = isSelected,
                        onClick = { selectedMovieSubCategory = subCat },
                        modifier = Modifier.testTag("sub_tab_${subCat.replace(" ", "_")}"),
                        text = {
                            Text(
                                text = subCat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Debugging / VOD Information UI Header
        if (selectedMainTab == "MOVIES" && !isLoading && !isError) {
            Text(
                text = "Total Movies: ${displayChannels.size}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Grid View of Channel/Movie Cards or Error/Loading Fallback
        if (isError) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Connection Failed. Please check your network.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                    ) {
                        Text("Retry Connection", color = Color.White)
                    }
                }
            }
        } else if ((selectedMainTab == "LIVE TV" || selectedMainTab == "MOVIES") && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = RedAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Fetching Live M3U Playlist...",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (displayChannels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedMainTab == "LIVE TV") "No streams available for $selectedLiveSubCategory" else if (selectedMainTab == "MOVIES") "No movies available for $selectedMovieSubCategory" else "No favorite channels added yet.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("channels_grid")
            ) {
                items(displayChannels, key = { it.id }) { channel ->
                    val isFav = favoritedIds.contains(channel.id)
                    ChannelCard(
                        channel = channel,
                        isFavorite = isFav,
                        onFavoriteToggle = { toggleFavorite(channel) },
                        onClick = { onChannelSelected(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() }
            .testTag("channel_card_${channel.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Channel Thumbnail / Brand Logo Card
            if (channel.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ChannelLogoDisplay(
                    channel = channel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Gradient Overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Top Row (LIVE/VOD Tag and Favorite Toggle Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LIVE or VOD Tag
                val isVod = channel.country == "Movies" || 
                            channel.category.lowercase().contains("movie") || 
                            channel.category.lowercase().contains("series") || 
                            channel.category.lowercase().contains("vod") ||
                            channel.id.contains("movie") || 
                            channel.id.contains("series") || 
                            channel.streamUrl.lowercase().contains(".mp4") || 
                            channel.streamUrl.lowercase().contains(".mkv")

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isVod) Color(0xFFE65100) else RedAccent)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isVod) "VOD" else "LIVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Heart Favorite Button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("fav_btn_${channel.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) RedAccent else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Center Play Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(RedAccent.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Bottom Information
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = channel.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = channel.category,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChannelLogoDisplay(
    channel: Channel,
    modifier: Modifier = Modifier
) {
    val initials = remember(channel.name) {
        channel.name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
    }

    val gradientColors = remember(channel.id) {
        when {
            channel.id.contains("geo") -> listOf(Color(0xFFB71C1C), Color(0xFF0D47A1))
            channel.id.contains("ary") -> listOf(Color(0xFFE65100), Color(0xFF1B5E20))
            channel.id.contains("green") -> listOf(Color(0xFF1B5E20), Color(0xFF004D40))
            channel.id.contains("goldmines") -> listOf(Color(0xFF4A148C), Color(0xFF880E4F))
            channel.id.contains("ertugrul") || channel.id.contains("osman") -> listOf(Color(0xFF3E2723), Color(0xFFBF360C))
            channel.id.contains("kids") || channel.id.contains("baby") -> listOf(Color(0xFF880E4F), Color(0xFF4A148C))
            channel.id.contains("music") || channel.id.contains("9xm") -> listOf(Color(0xFF006064), Color(0xFF4A148C))
            else -> listOf(Color(0xFF1E1E2C), Color(0xFF2D2D44))
        }
    }

    Box(
        modifier = modifier.background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = initials,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }
    }
}

private fun parseM3U(m3uContent: String, defaultCountry: String = "Pakistan"): List<Channel> {
    val channels = mutableListOf<Channel>()
    val lines = m3uContent.lines()
    var currentName = "Live Channel"
    var currentLogo = ""
    var currentGroup = defaultCountry

    val deadDomains = listOf("dead.", "offline.", "test.", "localhost", "127.0.0.1", "expired.", "dummy", "invalid", "example.", "sample.")
    val badKeywords = listOf("test", "dummy", "offline", "expired", "not working", "broken")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("#EXTINF:")) {
            val commaIndex = trimmed.lastIndexOf(',')
            if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                currentName = trimmed.substring(commaIndex + 1).trim()
            }
            if (trimmed.contains("tvg-logo=\"")) {
                currentLogo = trimmed.substringAfter("tvg-logo=\"").substringBefore("\"")
            }
            if (trimmed.contains("group-title=\"")) {
                currentGroup = trimmed.substringAfter("group-title=\"").substringBefore("\"")
            }
        } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
            val streamUrl = trimmed
            val isDead = deadDomains.any { streamUrl.contains(it, ignoreCase = true) }
            val isBadName = badKeywords.any { currentName.contains(it, ignoreCase = true) }
            val hasValidExt = streamUrl.contains(".m3u8", ignoreCase = true) || 
                              streamUrl.contains(".ts", ignoreCase = true) || 
                              streamUrl.contains(".mp4", ignoreCase = true) || 
                              streamUrl.contains(".mkv", ignoreCase = true) || 
                              streamUrl.contains("live", ignoreCase = true) || 
                              streamUrl.contains("stream", ignoreCase = true) ||
                              streamUrl.contains("playlist", ignoreCase = true) ||
                              streamUrl.contains("hls", ignoreCase = true)

            if (!isDead && !isBadName && hasValidExt && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
                val finalLogo = if (currentLogo.isNotBlank()) currentLogo else {
                    if (defaultCountry == "Movies") {
                        "https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?w=500&auto=format&fit=crop"
                    } else {
                        ""
                    }
                }
                val id = "${defaultCountry.lowercase().replace(" ", "_").replace("/", "_")}_dyn_${channels.size + 1}"
                channels.add(
                    Channel(
                        id = id,
                        name = currentName,
                        logoUrl = finalLogo,
                        streamUrl = streamUrl,
                        country = defaultCountry,
                        category = if (currentGroup.isNotBlank()) currentGroup else "VOD / Movie",
                        isLive = true
                    )
                )
            }
            currentName = "Live Channel"
            currentLogo = ""
        }
    }
    val limit = if (defaultCountry == "Movies") 500 else 150
    return channels.sortedBy { it.name }.take(limit)
}

private fun filterLiveChannels(rawChannels: List<Channel>, subCategory: String): List<Channel> {
    return when (subCategory) {
        "Pakistan" -> rawChannels.filter { 
            it.country.equals("Pakistan", ignoreCase = true) || 
            it.name.contains("Pakistan", ignoreCase = true) || 
            it.streamUrl.contains("/pk/", ignoreCase = true) 
        }
        "India" -> rawChannels.filter { 
            it.country.equals("India", ignoreCase = true) || 
            it.name.contains("India", ignoreCase = true) || 
            it.streamUrl.contains("/in/", ignoreCase = true) 
        }
        "Cartoons" -> rawChannels.filter {
            it.category.contains("Kids", ignoreCase = true) || 
            it.category.contains("Animation", ignoreCase = true) || 
            it.name.contains("Cartoon", ignoreCase = true) || 
            it.name.contains("Kids", ignoreCase = true) || 
            it.name.contains("Disney", ignoreCase = true) || 
            it.name.contains("Nickelodeon", ignoreCase = true)
        }
        "Turkey" -> rawChannels.filter { 
            it.country.equals("Turkey", ignoreCase = true) || 
            it.name.contains("Turkey", ignoreCase = true) || 
            it.streamUrl.contains("/tr/", ignoreCase = true) 
        }
        "China" -> rawChannels.filter { 
            it.country.equals("China", ignoreCase = true) || 
            it.name.contains("China", ignoreCase = true) || 
            it.streamUrl.contains("/cn/", ignoreCase = true) 
        }
        "Korea" -> rawChannels.filter { 
            it.country.equals("Korea", ignoreCase = true) || 
            it.name.contains("Korea", ignoreCase = true) || 
            it.streamUrl.contains("/kr/", ignoreCase = true) 
        }
        "USA / International" -> rawChannels.filter { 
            it.country.equals("USA", ignoreCase = true) || 
            it.name.contains("USA", ignoreCase = true) || 
            it.streamUrl.contains("/us/", ignoreCase = true) 
        }
        else -> rawChannels
    }
}

private fun filterAndSortMovies(rawMovies: List<Channel>, subCategory: String): List<Channel> {
    // Strictly filter out Live TV channels and only include actual VOD/Movie streams (.mp4, .mkv, or VOD m3u8 links)
    val baseMovies = rawMovies.filter { ch ->
        val streamUrl = ch.streamUrl.lowercase()
        val nameLower = ch.name.lowercase()
        val groupLower = ch.category.lowercase()

        val isVodExtension = streamUrl.contains(".mp4") || streamUrl.contains(".mkv")
        val isVodUrl = streamUrl.contains("vod") || streamUrl.contains("/movie") || streamUrl.contains("/series") || streamUrl.contains("/play/") || streamUrl.contains("/movies/")
        val isMovieGroup = groupLower.contains("movie") || groupLower.contains("cinema") || groupLower.contains("film") || groupLower.contains("vod") || groupLower.contains("series") || groupLower.contains("dramas") || groupLower.contains("hollywood") || groupLower.contains("chinese") || groupLower.contains("korean") || groupLower.contains("punjabi") || groupLower.contains("pakistani") || groupLower.contains("cartoons")
        
        // Check if it is a live TV channel name
        val isLiveTvChannel = nameLower.contains("nickelodeon") || 
                              nameLower.contains("nick jr") || 
                              nameLower.contains("sony yay") || 
                              nameLower.contains("disney") || 
                              nameLower.contains("pogo") || 
                              nameLower.contains("hungama") || 
                              nameLower.contains("cartoon network") || 
                              nameLower.contains("sports") || 
                              nameLower.contains("news") || 
                              nameLower.contains("live tv") ||
                              nameLower.contains("geotv") ||
                              nameLower.contains("arytv")

        // Must be a VOD stream and NOT a Live TV channel
        (isVodExtension || isVodUrl || isMovieGroup) && !isLiveTvChannel
    }

    return when (subCategory) {
        "All Movies" -> baseMovies
        "All Series" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                val streamUrl = ch.streamUrl.lowercase()
                nameLower.contains("series") || groupLower.contains("series") || streamUrl.contains("series") ||
                nameLower.contains("season") || nameLower.contains("episode") || nameLower.contains("ep ") || nameLower.contains("ep.") ||
                nameLower.contains("turkish") || groupLower.contains("turkish") || nameLower.contains("complete") || nameLower.contains("drama") || groupLower.contains("dramas")
            }
        }
        "Hollywood" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("hollywood") || groupLower.contains("hollywood") || nameLower.contains("english") || groupLower.contains("english") || nameLower.contains("dark knight") || nameLower.contains("avatar") || nameLower.contains("iron man")
            }
        }
        "Chinese" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("chinese") || groupLower.contains("chinese") || nameLower.contains("china") || groupLower.contains("china") || nameLower.contains("ip man") || nameLower.contains("crouching tiger") || nameLower.contains("hero")
            }
        }
        "Korean" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("korean") || groupLower.contains("korean") || nameLower.contains("korea") || groupLower.contains("korea") || nameLower.contains("k-drama") || nameLower.contains("squid game") || nameLower.contains("star") || nameLower.contains("dead")
            }
        }
        "Punjabi" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("punjabi") || groupLower.contains("punjabi") || nameLower.contains("punjab") || nameLower.contains("carry on jatta") || nameLower.contains("saunkan") || nameLower.contains("jatt")
            }
        }
        "Pakistani" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("pakistani") || groupLower.contains("pakistani") || nameLower.contains("lollywood") || groupLower.contains("lollywood") || nameLower.contains("pakistan") || nameLower.contains("maula jatt") || nameLower.contains("jawani") || nameLower.contains("jaungi")
            }
        }
        "Cartoons" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("cartoon") || groupLower.contains("cartoon") || nameLower.contains("animated") || groupLower.contains("animated") || nameLower.contains("animation") || groupLower.contains("animation") || nameLower.contains("kids") || groupLower.contains("kids") || nameLower.contains("disney") || nameLower.contains("anime") || nameLower.contains("panda") || nameLower.contains("spirited away") || nameLower.contains("bunny")
            }
        }
        "Turkish Dramas (Hindi/Urdu Dubbed)" -> {
            baseMovies.filter { ch ->
                val nameLower = ch.name.lowercase()
                val groupLower = ch.category.lowercase()
                nameLower.contains("turkish") || groupLower.contains("turkish") || nameLower.contains("turkey") || nameLower.contains("ertugrul") || nameLower.contains("osman") || nameLower.contains("kurulus") || nameLower.contains("alparslan") || nameLower.contains("selcuklu") || nameLower.contains("drama") || groupLower.contains("dramas")
            }
        }
        else -> baseMovies
    }
}
