package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.firebase.UserDataRepository
import com.example.data.local.LastListenedStore
import com.example.data.remote.QuranRepository
import com.example.player.QuranPlayerController
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.IslamicAmbientBackground
import com.example.ui.player.FullPlayerModal
import com.example.ui.player.MiniPlayerBar
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IbadatScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SurahListScreen
import com.example.ui.theme.NurTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

const val ROUTE_HOME = "home"
const val ROUTE_SURAHS = "surahs"
const val ROUTE_READER = "reader"
const val ROUTE_FAVORITES = "favorites"
const val ROUTE_IBADAT = "ibadat"

private enum class NurDestination(
    val route: String,
    val labelTr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(ROUTE_HOME, "Ana Sayfa", Icons.Rounded.Home, Icons.Outlined.Home),
    SURAHS(ROUTE_SURAHS, "Sureler", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
    READER(ROUTE_READER, "Oku", Icons.AutoMirrored.Rounded.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    FAVORITES(ROUTE_FAVORITES, "Favoriler", Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
    IBADAT(ROUTE_IBADAT, "İbadet", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome)
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun NurAppRoot(
    quranRepository: QuranRepository,
    playerController: QuranPlayerController
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        NurTheme(darkTheme = true) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            val viewModel: MainViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MainViewModel(
                            userId = user.uid,
                            userDataRepository = UserDataRepository(db, auth),
                            quranRepository = quranRepository,
                            lastListenedStore = LastListenedStore(app),
                            playerController = playerController
                        )
                    }
                }
            )
            NurAuthenticatedShell(viewModel = viewModel)
        }
    }
}

@Composable
private fun NurAuthenticatedShell(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val prefsState by viewModel.preferencesState.collectAsStateWithLifecycle()
    val favsState by viewModel.favoritesState.collectAsStateWithLifecycle()
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val ibadatState by viewModel.dailyIbadatState.collectAsStateWithLifecycle()
    val lastListenedState by viewModel.lastListenedState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val readerState by viewModel.readerState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val dailyVerse by viewModel.dailyVerse.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.flushListeningProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isDarkTheme = (prefsState as? UiState.Success)?.data?.darkTheme ?: true
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    var isFullPlayerOpen by remember { mutableStateOf(false) }
    var askedNotifPermission by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Playback works regardless */ }
    )

    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !askedNotifPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotifPermission = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            askedNotifPermission = true
        }
    }

    val playSurahWithPermission: (Int, Int) -> Unit = { surahNum, ayahNum ->
        requestNotificationPermissionIfNeeded()
        viewModel.playSurah(surahNum, ayahNum)
    }

    val playAyahWithPermission: (Int, Int) -> Unit = { surahNum, ayahNum ->
        requestNotificationPermissionIfNeeded()
        viewModel.playAyahInSurah(surahNum, ayahNum)
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        val msg = playbackState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(playbackState.audioWarning) {
        val msg = playbackState.audioWarning
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.playerController.clearAudioWarning()
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openReader: (Int, Int?) -> Unit = { surahNumber, ayahNumber ->
        viewModel.loadSurahForReading(surahNumber, ayahNumber)
        navigateToTab(ROUTE_READER)
    }

    val resumeLastListenedWithPermission: () -> Unit = {
        requestNotificationPermissionIfNeeded()
        viewModel.resumeLastListened()
        navigateToTab(ROUTE_READER)
    }

    val handleSignOut: () -> Unit = {
        viewModel.playerController.stopPlayback()
        signOutUser(credentialManager, scope)
    }

    BackHandler(enabled = isFullPlayerOpen) {
        isFullPlayerOpen = false
    }

    val favoritesList = (favsState as? UiState.Success)?.data.orEmpty()
    val isCurrentSurahFav = playbackState.currentSurah?.let { s ->
        favoritesList.any { it.kind == "surah" && it.surahNumber.toInt() == s.number }
    } ?: false

    val miniPlayerVisible = playbackState.currentSurah != null
    val extraBottomForMiniPlayer = if (miniPlayerVisible) 84.dp else 8.dp

    NurTheme(darkTheme = isDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            IslamicAmbientBackground(isDark = isDarkTheme) {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useRail) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                if (miniPlayerVisible) {
                                    MiniPlayerBar(
                                        playback = playbackState,
                                        onExpand = { isFullPlayerOpen = true },
                                        onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                                        onNextSurah = { viewModel.playerController.nextSurah() },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    NurDestination.entries.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateToTab(dest.route) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                    contentDescription = dest.labelTr
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = dest.labelTr,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                            ),
                                            modifier = Modifier.testTag("nav_${dest.route}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useRail) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                NurDestination.entries.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateToTab(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.labelTr
                                            )
                                        },
                                        label = { Text(dest.labelTr) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                                            indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                        ),
                                        modifier = Modifier.testTag("rail_${dest.route}")
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 860.dp)
                                    .fillMaxWidth()
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_HOME,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(ROUTE_HOME) {
                                        HomeScreen(
                                            isDarkTheme = isDarkTheme,
                                            dailyVerse = dailyVerse,
                                            lastProgress = lastListenedState,
                                            preferencesState = prefsState,
                                            historyState = historyState,
                                            favoritesState = favsState,
                                            dailyIbadatState = ibadatState,
                                            activeReciter = playbackState.currentReciter,
                                            onToggleTheme = viewModel::toggleTheme,
                                            onOpenSearch = { navigateToTab(ROUTE_SURAHS) },
                                            onSignOut = handleSignOut,
                                            onResumeLastListened = resumeLastListenedWithPermission,
                                            onPlaySurah = playSurahWithPermission,
                                            onOpenSurahReader = openReader,
                                            onSelectReciter = viewModel::selectReciter,
                                            onToggleFavoriteSurah = viewModel::toggleFavoriteSurah,
                                            onViewAllSurahs = { navigateToTab(ROUTE_SURAHS) },
                                            onOpenIbadat = { navigateToTab(ROUTE_IBADAT) },
                                            bottomContentPadding = if (useRail && miniPlayerVisible) 88.dp else extraBottomForMiniPlayer
                                        )
                                    }

                                    composable(ROUTE_SURAHS) {
                                        SurahListScreen(
                                            searchState = searchState,
                                            favoritesState = favsState,
                                            activeSurahNumber = playbackState.currentSurah?.number,
                                            activeReciter = playbackState.currentReciter,
                                            onSearchQueryChange = viewModel::onSearchQueryChanged,
                                            onSelectReciter = viewModel::selectReciter,
                                            onOpenSurahReader = openReader,
                                            onPlaySurah = playSurahWithPermission,
                                            onToggleFavoriteSurah = viewModel::toggleFavoriteSurah,
                                            bottomContentPadding = if (useRail && miniPlayerVisible) 88.dp else extraBottomForMiniPlayer
                                        )
                                    }

                                    composable(ROUTE_READER) {
                                        ReaderScreen(
                                            readerState = readerState,
                                            playbackState = playbackState,
                                            favoritesState = favsState,
                                            onLoadSurah = { num, ayah -> viewModel.loadSurahForReading(num, ayah) },
                                            onConsumeScrollTarget = viewModel::consumeReaderScrollTarget,
                                            onPlaySurah = playSurahWithPermission,
                                            onPlayAyah = playAyahWithPermission,
                                            onToggleFavoriteSurah = viewModel::toggleFavoriteSurah,
                                            onToggleFavoriteAyah = viewModel::toggleFavoriteAyah,
                                            onToggleArabic = viewModel::toggleReaderArabic,
                                            onToggleTurkish = viewModel::toggleReaderTurkish,
                                            onAdjustFontScale = viewModel::adjustArabicFontScale,
                                            onMarkAyahsRead = viewModel::markAyahsRead,
                                            bottomContentPadding = if (useRail && miniPlayerVisible) 88.dp else extraBottomForMiniPlayer
                                        )
                                    }

                                    composable(ROUTE_FAVORITES) {
                                        FavoritesScreen(
                                            lastProgress = lastListenedState,
                                            favoritesState = favsState,
                                            historyState = historyState,
                                            dailyIbadatState = ibadatState,
                                            onResumeLastListened = resumeLastListenedWithPermission,
                                            onOpenSurahReader = openReader,
                                            onPlaySurah = playSurahWithPermission,
                                            onRemoveFavorite = viewModel::removeFavorite,
                                            bottomContentPadding = if (useRail && miniPlayerVisible) 88.dp else extraBottomForMiniPlayer
                                        )
                                    }

                                    composable(ROUTE_IBADAT) {
                                        IbadatScreen(
                                            isDarkTheme = isDarkTheme,
                                            preferencesState = prefsState,
                                            dailyIbadatState = ibadatState,
                                            activeReciter = playbackState.currentReciter,
                                            onToggleJuz = viewModel::toggleJuzCompleted,
                                            onUpdateGoals = viewModel::updateDailyGoals,
                                            onOpenJuzInReader = openReader,
                                            onSelectReciter = viewModel::selectReciter,
                                            onToggleTheme = viewModel::toggleTheme,
                                            onSignOut = handleSignOut,
                                            bottomContentPadding = if (useRail && miniPlayerVisible) 88.dp else extraBottomForMiniPlayer
                                        )
                                    }
                                }

                                if (useRail && miniPlayerVisible) {
                                    MiniPlayerBar(
                                        playback = playbackState,
                                        onExpand = { isFullPlayerOpen = true },
                                        onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                                        onNextSurah = { viewModel.playerController.nextSurah() },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isFullPlayerOpen && playbackState.currentSurah != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FullPlayerModal(
                    playback = playbackState,
                    isFavoriteSurah = isCurrentSurahFav,
                    onCollapse = { isFullPlayerOpen = false },
                    onToggleFavoriteSurah = {
                        playbackState.currentSurah?.let { viewModel.toggleFavoriteSurah(it) }
                    },
                    onOpenInReader = { sNum, aNum ->
                        isFullPlayerOpen = false
                        openReader(sNum, aNum)
                    },
                    onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                    onStop = { viewModel.playerController.stopPlayback() },
                    onPrevSurah = { viewModel.playerController.previousSurah() },
                    onNextSurah = { viewModel.playerController.nextSurah() },
                    onRewind10s = { viewModel.playerController.seekBack10s() },
                    onForward10s = { viewModel.playerController.seekForward10s() },
                    onSeekFraction = { frac -> viewModel.playerController.seekToSurahFraction(frac) },
                    onVolumeChange = { vol -> viewModel.playerController.setVolume(vol) },
                    onCycleRepeat = { viewModel.playerController.cycleRepeatMode() },
                    onToggleAutoNext = { viewModel.playerController.toggleAutoNextSurah() },
                    onSelectReciter = { reciter -> viewModel.selectReciter(reciter) },
                    onClearError = {
                        viewModel.playerController.clearError()
                        viewModel.playerController.clearAudioWarning()
                    }
                )
            }
        }
    }
}
