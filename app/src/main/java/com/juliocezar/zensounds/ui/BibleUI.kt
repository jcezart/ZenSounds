package com.juliocezar.zensounds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.Verse
import com.juliocezar.zensounds.ui.viewmodel.BibleViewModel
import com.juliocezar.zensounds.ui.viewmodel.BibleViewModelFactory

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

    // Estados de navegação da UI (Livros -> Capítulos -> Versículos)

    var stage by rememberSaveable { mutableStateOf(Stage.Books) }
    var selectedBookCode by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedBookName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapter by rememberSaveable { mutableStateOf<Int?>(null) }

    // Dados para a lista de livros (nome no idioma da UI; código usado no ViewModel)
    val booksForUi: List<Pair<String, String>> = bibleViewModel.allBooks.map { b ->
        b.code to b.namePt // pode trocar pra nameEn se preferir mostrar em inglês
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1C2C))
            .padding(top = 16.dp, bottom = innerPadding.calculateBottomPadding())
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título dinâmico
        val title = when (stage) {
            Stage.Books -> "Livros"
            Stage.Chapters -> selectedBookName ?: "Capítulos"
            Stage.Verses -> "${selectedBookName ?: ""} ${selectedChapter ?: ""}"
        }
        Text(
            text = title.ifBlank { "Trechos Bíblicos" },
            fontSize = 42.sp,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB0C4D4),
            modifier = Modifier
                .fillMaxWidth()
                .padding(42.dp),
            textAlign = TextAlign.Center
        )

        // Conteúdo por etapa
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
                        // Carrega do Room (com fallback no próprio VM, se necessário)
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



/** Lista simples de livros. */
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
            Surface(
                tonalElevation = 2.dp,
                color = Color(0xFF102233),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1A2B3D))
                    .clickable { onBookClick(code, name) }
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 18.sp
                )
            }
        }
    }
}

/** Grade de capítulos (6 colunas). */
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
            text = "← Voltar aos livros",
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
                    tonalElevation = 1.dp,
                    color = Color(0xFF12304a),
                    modifier = Modifier
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

/** Lista de versículos. */
@Composable
private fun VerseList(
    verses: List<com.juliocezar.zensounds.data.Verse>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "← Voltar aos capítulos",
            color = Color(0xFFB0C4D4),
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clickable { onBack() }
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(verses) { v ->
                Text(
                    text = "${v.book} ${v.chapter}:${v.verseNumber} — ${v.text}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/* Mantive a BottomNavigationBar se você usa em outro lugar */
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
