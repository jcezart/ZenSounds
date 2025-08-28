package com.juliocezar.zensounds.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.ui.viewmodel.BibleViewModel
import com.juliocezar.zensounds.ui.viewmodel.BibleViewModelFactory
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding

enum class Stage { Books, Chapters, Verses }

@Suppress("UNUSED_PARAMETER")
@Composable
fun BibleScreen(
    soundViewModel1: com.juliocezar.zensounds.ui.viewmodel.SoundViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {

    val bibleViewModel: BibleViewModel = viewModel(factory = BibleViewModelFactory(LocalContext.current))
    val verses by bibleViewModel.bibleVerses.collectAsState()
    val uiLang by bibleViewModel.languageFlow.collectAsState()

    var stage by rememberSaveable { mutableStateOf(Stage.Books) }
    var selectedBookCode by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBookName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapter by rememberSaveable { mutableStateOf<Int?>(null) }

    val booksForUi: List<Pair<String, String>> = remember(uiLang) {
        bibleViewModel.allBooks.map { meta ->
            val label = if (uiLang == "pt") meta.namePt else meta.nameEn
            meta.code to label
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1C2C))
            .padding(top = 16.dp, bottom = innerPadding.calculateBottomPadding())
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .displayCutoutPadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BannerAd(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        val title = when (stage) {
            Stage.Books -> "Books"
            Stage.Chapters -> selectedBookName ?: "Chapters"
            Stage.Verses -> "${selectedBookName ?: ""} ${selectedChapter ?: ""}"
        }
        Text(
            text = title.ifBlank { "Biblical Passages" },
            fontSize = 42.sp,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB0C4D4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(42.dp),
            textAlign = TextAlign.Center
        )

        when (stage) {
            Stage.Books -> {
                BookList(
                    books = booksForUi,
                    onBookClick = { code, name ->
                        selectedBookCode = code
                        selectedBookName = name
                        stage = Stage.Chapters
                    }
                )
            }

            Stage.Chapters -> {
                val chapters = bibleViewModel.bookByCode(selectedBookCode ?: "")?.chapters ?: 1
                ChapterGrid(
                    chapters = chapters,
                    onChapterClick = { chap ->
                        val book = selectedBookCode ?: return@ChapterGrid
                        selectedChapter = chap
                        bibleViewModel.loadChapter(book, chap)
                        stage = Stage.Verses
                    },
                    onBack = { stage = Stage.Books }
                )
            }

            Stage.Verses -> {
                VerseList(
                    verses = verses,
                    onBack = { stage = Stage.Chapters }
                )
            }
        }
    }
}

@Composable
private fun BookList(
    books: List<Pair<String, String>>,
    onBookClick: (code: String, name: String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books) { (code, name) ->

            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "scale")

            val bgColor = Color(0xFF12304a)
            val border = Color(0xFF2A3F59)
            val title = Color(0xFFB0C4D4)
            val muted = Color(0xFF8EA3B5)

            val chapters = when (code) {
                "GEN" -> 50
                "EXO" -> 40
                "LEV" -> 27
                "NUM" -> 36
                "DEU" -> 34
                else -> null
            }

            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = Color(0xFF12304a),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { this.scaleX = scale; this.scaleY = scale }
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, border.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .background(bgColor, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onBookClick(code, name) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                color = title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (chapters != null) {
                                Text(
                                    text = "$chapters capítulos",
                                    color = muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "›",
                        color = title.copy(alpha = 0.8f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterGrid(
    chapters: Int,
    onChapterClick: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "← Back to books",
            color = Color(0xFFB0C4D4),
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clickable { onBack() }
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items((1..chapters).toList()) { chap ->
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = Color(0xFF12304a),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .height(44.dp)
                        .clickable { onChapterClick(chap) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = chap.toString(), color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VerseList(
    verses: List<com.juliocezar.zensounds.data.Verse>,
    onBack: () -> Unit,
) {
    val cardBg = Color(0xFF102233)
    val border = Color(0xFF1A2B3D)
    val textPrimary = Color.White
    val textSecondary = Color(0xFFB0C4D4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "← Back to chapters",
            color = textSecondary,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clickable { onBack() }
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = verses,
                key = { "${it.language}-${it.book}-${it.chapter}-${it.verseNumber}" }
            ) { v ->
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    color = cardBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, border.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Badge com o número do versículo
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2F46))
                                .border(1.dp, border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = v.verseNumber.toString(),
                                color = textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${v.book} ${v.chapter}:${v.verseNumber}",
                                color = textSecondary,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = v.text,
                                color = textPrimary,
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) } // respiro no fim da lista
        }
    }
}


@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "zenSounds"
    val selectedTab = when (currentRoute) {
        "zenSounds" -> 0
        "bible" -> 1
        else -> 0
    }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = Color(0xFF0C1C2C),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .border(1.dp, Color(0xFF1A2B3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                iconId = R.drawable.music,
                isSelected = selectedTab == 0,
                onClick = {
                    navController.navigate("zenSounds") {
                        launchSingleTop = true
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    }
                }
            )
            PlayPauseButton(
                isPlaying = isPlaying,
                onClick = onPlayPauseClick
            )
            BottomNavItem(
                iconId = R.drawable.bible,
                isSelected = selectedTab == 1,
                onClick = {
                    navController.navigate("bible") {
                        launchSingleTop = true
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    }
                })
        }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember { AdView(context) }
    val adUnitId = "ca-app-pub-5167159527096735/1656937187" //ID real
    //val adUnitId = "ca-app-pub-3940256099942544/9214589741" //ID de teste

    AndroidView(
        modifier = modifier,
        factory = { adView },
        update = { view ->
            view.adUnitId = adUnitId
            view.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 320))
            val adRequest = AdRequest.Builder().build()
            view.loadAd(adRequest)

            view.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    // Anúncio carregado
                }
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Erro no carregamento
                }
            }
        }
    )
}
