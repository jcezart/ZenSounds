package com.juliocezar.zensounds.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.ui.viewmodel.BibleViewModel
import com.juliocezar.zensounds.ui.viewmodel.BibleViewModelFactory
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel

enum class Stage { Books, Chapters, Verses }

@Suppress("UNUSED_PARAMETER")
@Composable
fun BibleScreen(
    soundViewModel1: SoundViewModel,
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
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (stage == Stage.Books) {
                    navController.popBackStack()
                } else {
                    stage = when (stage) {
                        Stage.Chapters -> Stage.Books
                        Stage.Verses -> Stage.Chapters
                        else -> Stage.Books
                    }
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Voltar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            val title = when (stage) {
                Stage.Books -> "Bíblia"
                Stage.Chapters -> selectedBookName ?: "Capítulos"
                Stage.Verses -> "${selectedBookName ?: ""} ${selectedChapter ?: ""}"
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        BannerAd(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

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
                    }
                )
            }
            Stage.Verses -> {
                VerseList(verses = verses)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(books) { (code, name) ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "scale")

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
                    .border(1.dp, Color(0xFF2A3F59).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onBookClick(code, name) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (chapters != null) {
                                Text(
                                    text = "$chapters capítulos",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Text(
                        text = "›",
                        color = Color(0xFFB0C4D4).copy(alpha = 0.8f),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterGrid(
    chapters: Int,
    onChapterClick: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 50.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items((1..chapters).toList()) { chap ->
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = Color(0xFF12304a),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .aspectRatio(1f)
                    .clickable { onChapterClick(chap) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = chap.toString(), color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun VerseList(
    verses: List<com.juliocezar.zensounds.data.Verse>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(
            items = verses,
            key = { "${it.language}-${it.book}-${it.chapter}-${it.verseNumber}" }

        ) { v ->
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                color = Color(0xFF102233),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Color(0xFF1A2B3D).copy(alpha = 0.7f),
                        RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2F46))
                            .border(1.dp, Color(0xFF1A2B3D), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = v.verseNumber.toString(),
                            color = Color(0xFFB0C4D4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        text = v.text,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember { AdView(context) }
    val adUnitId = "ca-app-pub-3940256099942544/9214589741" //ID de teste

    AndroidView(
        modifier = modifier.height(AdSize.BANNER.height.dp),
        factory = { adView },
        update = { view ->
            view.adUnitId = adUnitId
            view.setAdSize(AdSize.BANNER)
            view.loadAd(AdRequest.Builder().build())
        }
    )
}

@Composable
fun BottomNavItem(
    iconId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) Color(0xFF89CFF0) else Color.Gray
    IconButton(onClick = onClick) {
        Icon(painter = painterResource(id = iconId),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp))
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF89CFF0))
    ) {
        Icon(imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play", tint = Color.Black, modifier = Modifier.size(36.dp))
    }
}