package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Kural
import com.example.ui.ThirukkuralViewModel
import com.example.ui.ThirukkuralViewModel.Tab
import com.example.ui.ThirukkuralViewModel.DbLoadState
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkThemeState by remember {
                mutableStateOf(prefs.getBoolean("dark_theme", systemDark))
            }

            MyApplicationTheme(darkTheme = darkThemeState) {
                ThirukkuralApp(
                    isDarkTheme = darkThemeState,
                    onToggleTheme = {
                        val newValue = !darkThemeState
                        darkThemeState = newValue
                        prefs.edit().putBoolean("dark_theme", newValue).apply()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirukkuralApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: ThirukkuralViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val dbLoadState by viewModel.dbLoadState.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val randomKural by viewModel.randomKural.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val favoriteKurals by viewModel.favoriteKurals.collectAsStateWithLifecycle()
    val allKurals by viewModel.allKurals.collectAsStateWithLifecycle()
    val selectedKuralForDetails by viewModel.selectedKuralForDetails.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "திருக்குறள்",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "THIRUKKURAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light/Dark Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            if (dbLoadState is DbLoadState.Success) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == Tab.RANDOM,
                        onClick = { viewModel.selectTab(Tab.RANDOM) },
                        icon = { Icon(Icons.Default.Shuffle, contentDescription = "Random Kural") },
                        label = { Text("Random", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_random")
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.SEARCH,
                        onClick = { viewModel.selectTab(Tab.SEARCH) },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search Kural") },
                        label = { Text("Search", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.BROWSE,
                        onClick = { viewModel.selectTab(Tab.BROWSE) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "Browse Chapters") },
                        label = { Text("Browse", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_browse")
                    )
                    NavigationBarItem(
                        selected = currentTab == Tab.FAVORITES,
                        onClick = { viewModel.selectTab(Tab.FAVORITES) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorite Kurals") },
                        label = { Text("Favorites", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_favorites")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = dbLoadState) {
                is DbLoadState.Loading -> {
                    LoadingScreen()
                }
                is DbLoadState.Error -> {
                    ErrorScreen(message = state.message, onRetry = { viewModel.initDatabase() })
                }
                is DbLoadState.Success -> {
                    when (currentTab) {
                        Tab.RANDOM -> {
                            RandomKuralScreen(
                                kural = randomKural,
                                onGenerateRandom = { viewModel.generateRandomKural() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onShare = { shareKural(context, it) }
                            )
                        }
                        Tab.SEARCH -> {
                            SearchKuralScreen(
                                query = searchQuery,
                                result = searchResult,
                                error = searchError,
                                onQueryChanged = { viewModel.setSearchQuery(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onShare = { shareKural(context, it) }
                            )
                        }
                        Tab.BROWSE -> {
                            BrowseChaptersScreen(
                                allKurals = allKurals,
                                onKuralClick = { viewModel.selectKuralForDetails(it) }
                            )
                        }
                        Tab.FAVORITES -> {
                            FavoritesScreen(
                                favoriteKurals = favoriteKurals,
                                onKuralClick = { viewModel.selectKuralForDetails(it) },
                                onClearAll = { viewModel.clearAllFavorites() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }
                }
                else -> {}
            }

            selectedKuralForDetails?.let { kural ->
                KuralDetailDialog(
                    kural = kural,
                    onDismiss = { viewModel.selectKuralForDetails(null) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onShare = { shareKural(context, it) }
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "பாங்காகத் தரவிறக்கம் செய்யப்படுகிறது...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Downloading Thirukkural database for offline access. This happens only once.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "அமைப்பு பிழை / Connection Error",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry")
            Spacer(modifier = Modifier.width(8.dp))
            Text("மீண்டும் முயல்க / Retry")
        }
    }
}

@Composable
fun KuralCard(
    kural: Kural,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    showAllExplanations: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("kural_card_${kural.number}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "குறள் / Kural ${kural.number}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("kural_fav_btn_${kural.number}")
                    ) {
                        Icon(
                            imageVector = if (kural.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (kural.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("kural_share_btn_${kural.number}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val sectionStr = kural.section ?: ""
            val chapterStr = kural.chapter ?: ""
            val separator = if (sectionStr.isNotEmpty() && chapterStr.isNotEmpty()) "  |  " else ""
            Text(
                text = "$sectionStr$separator$chapterStr",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = kural.line1,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    lineHeight = 26.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = kural.line2,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    lineHeight = 26.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "TRANSLATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = kural.translation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (showAllExplanations) {
                kural.mv?.let {
                    ExplanationItem(title = "மு. வரதராசன் விளக்கம் (M.V.)", text = it)
                }
                kural.sp?.let {
                    ExplanationItem(title = "சாலமன் பாப்பையா விளக்கம் (S.P.)", text = it)
                }
                kural.mk?.let {
                    ExplanationItem(title = "மு. கருணாநிதி விளக்கம் (M.K.)", text = it)
                }
            } else {
                kural.mv?.let {
                    ExplanationItem(title = "விளக்கம் (Mu. Varatharasan)", text = it)
                }
            }
        }
    }
}

@Composable
fun ExplanationItem(title: String, text: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        lineHeight = 22.sp
    )
}

@Composable
fun RandomKuralScreen(
    kural: Kural?,
    onGenerateRandom: () -> Unit,
    onToggleFavorite: (Kural) -> Unit,
    onShare: (Kural) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (kural != null) {
            KuralCard(
                kural = kural,
                onToggleFavorite = { onToggleFavorite(kural) },
                onShare = { onShare(kural) }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onGenerateRandom,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("next_random_button")
        ) {
            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "அடுத்த குறள் / Next Kural",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SearchKuralScreen(
    query: String,
    result: Kural?,
    error: String?,
    onQueryChanged: (String) -> Unit,
    onToggleFavorite: (Kural) -> Unit,
    onShare: (Kural) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "எண்ணின் அடிப்படையில் தேடுக / Search by Number",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            label = { Text("Kural Number (1 - 1330)") },
            placeholder = { Text("e.g. 55") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary
            )
        )

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (result != null) {
            KuralCard(
                kural = result,
                onToggleFavorite = { onToggleFavorite(result) },
                onShare = { onShare(result) },
                showAllExplanations = true
            )
        } else if (query.isNotBlank() && error == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "தேட எண்ணை உள்ளிடவும் (1-1330)\nEnter a number from 1 to 1330 to search.",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "தேடல் வழிமுறை",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "திருக்குறளின் 1 முதல் 1330 வரையிலான எந்தவொரு எண்ணையும் உள்ளிட்டு, அதன் முழு விளக்கங்களையும் (மு.வ, சாலமன் பாப்பையா, கலைஞர்) அறிந்து கொள்ள தேடவும்.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BrowseChaptersScreen(
    allKurals: List<Kural>,
    onKuralClick: (Kural) -> Unit
) {
    var selectedSection by remember { mutableStateOf("Virtue") }
    var expandedChapter by remember { mutableStateOf<String?>(null) }

    val sections = remember(allKurals) {
        allKurals.map { it.section ?: "Unknown" }.distinct()
    }

    val chapters = remember(allKurals, selectedSection) {
        allKurals.filter { (it.section ?: "Unknown") == selectedSection }.map { it.chapter ?: "Unknown" }.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "இயல் பகுப்பு / Browse by Chapter",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = sections.indexOf(selectedSection).coerceAtLeast(0),
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            sections.forEach { sectionName ->
                Tab(
                    selected = selectedSection == sectionName,
                    onClick = {
                        selectedSection = sectionName
                        expandedChapter = null
                    },
                    text = {
                        Text(
                            text = when (sectionName) {
                                "Virtue" -> "அறத்துப்பால்\nVirtue"
                                "Wealth" -> "பொருட்பால்\nWealth"
                                "Love" -> "இன்பத்துப்பால்\nLove"
                                else -> sectionName
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chapters) { chapterName ->
                val isExpanded = expandedChapter == chapterName

                val kuralsInChapter = remember(allKurals, chapterName) {
                    allKurals.filter { (it.chapter ?: "Unknown") == chapterName }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedChapter = if (isExpanded) null else chapterName
                        }
                        .border(
                            width = 1.dp,
                            color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapterName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "10 குறள்கள் / 10 Couplets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(4.dp))

                                kuralsInChapter.forEach { kural ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onKuralClick(kural) }
                                            .border(
                                                width = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "குறள் / Kural ${kural.number}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                                if (kural.isFavorite) {
                                                    Icon(
                                                        imageVector = Icons.Default.Favorite,
                                                        contentDescription = "Favorite",
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = kural.line1,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = kural.line2,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favoriteKurals: List<Kural>,
    onKuralClick: (Kural) -> Unit,
    onClearAll: () -> Unit,
    onToggleFavorite: (Kural) -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "விருப்பமான குறள்கள் / Favorites",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (favoriteKurals.isNotEmpty()) {
                TextButton(
                    onClick = { showConfirmDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("clear_favorites_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("நீக்கு / Clear All", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (favoriteKurals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "No Favorites",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "விருப்பங்கள் எதுவும் இல்லை",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "உங்களுக்கு பிடித்த குறள்களைப் படிக்கும் போது இதயக் குறியீட்டை அழுத்தி இங்கு சேமித்துக்கொள்ளலாம்.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoriteKurals) { kural ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onKuralClick(kural) }
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val chapterStr = kural.chapter ?: ""
                                val separator = if (chapterStr.isNotEmpty()) "  |  " else ""
                                Text(
                                    text = "குறள் / Kural ${kural.number}$separator$chapterStr",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = kural.line1,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = kural.line2,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onToggleFavorite(kural) }) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Remove Favorite",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("விருப்பங்களை நீக்கவா? / Clear Favorites?") },
                text = { Text("விருப்பமான குறள்கள் அனைத்தையும் பட்டியலிலிருந்து நீக்க வேண்டுமா?\n\nAre you sure you want to clear all favorite Kurals?") },
                confirmButton = {
                    Button(
                        onClick = {
                            onClearAll()
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("நீக்கு / Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("ரத்து / Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun KuralDetailDialog(
    kural: Kural,
    onDismiss: () -> Unit,
    onToggleFavorite: (Kural) -> Unit,
    onShare: (Kural) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "குறள் / Kural ${kural.number}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Row {
                        IconButton(onClick = { onToggleFavorite(kural) }) {
                            Icon(
                                imageVector = if (kural.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (kural.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onShare(kural) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "அதிகாரம் / Chapter:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Text(
                        text = kural.chapter ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "பால் / Section:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Text(
                        text = when (kural.section) {
                            "Virtue" -> "அறத்துப்பால் / Virtue"
                            "Wealth" -> "பொருட்பால் / Wealth"
                            "Love" -> "இன்பத்துப்பால் / Love"
                            else -> kural.section ?: "Unknown"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = kural.line1,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 19.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = kural.line2,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = 19.sp,
                                    lineHeight = 28.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "ENGLISH TRANSLATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = kural.translation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "உரை விளக்கங்கள் / Scholarly Explanations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    kural.mv?.let {
                        ExplanationItem(title = "மு. வரதராசன் உரை (M. Varatharasan)", text = it)
                    }
                    kural.sp?.let {
                        ExplanationItem(title = "சாலமன் பாப்பையா உரை (Solomon Pappaiah)", text = it)
                    }
                    kural.mk?.let {
                        ExplanationItem(title = "மு. கருணாநிதி உரை (M. Karunanidhi)", text = it)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("மூடுக / Close")
                }
            }
        }
    }
}

private fun shareKural(context: Context, kural: Kural) {
    val shareText = """
        Thirukkural #${kural.number}

        ${kural.line1}
        ${kural.line2}

        Translation:
        ${kural.translation}

        Explanation (Mu.Varatharasan):
        ${kural.mv ?: "N/A"}

        Shared via Thirukkural App
    """.trimIndent()

    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Thirukkural", shareText)
    clipboardManager.setPrimaryClip(clip)

    Toast.makeText(context, "Kural copied to clipboard!", Toast.LENGTH_SHORT).show()
}
