package com.juliocezar.zensounds.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.*
import com.juliocezar.zensounds.ui.theme.LocalEssencePalette
import com.juliocezar.zensounds.ui.viewmodel.*

enum class Stage { Books, Chapters, Verses }

@Suppress("UNUSED_PARAMETER")
@Composable
fun BibleScreen(soundViewModel1: SoundViewModel,navController: NavHostController,innerPadding: PaddingValues,
    initialBookCode: String? = null, initialChapter: Int? = null, preferences: EssencePreferences? = null, initialVerseNumber: Int? = null) {
    val context=LocalContext.current
    val localPrefs=remember(context) { if(preferences==null) EssencePreferences(context.applicationContext) else null }
    DisposableEffect(localPrefs){onDispose{localPrefs?.close()}}
    val prefs=preferences ?: requireNotNull(localPrefs)
    val state by prefs.state.collectAsState()
    val vm: BibleViewModel = viewModel(factory=BibleViewModelFactory(context.applicationContext))
    val language=stringResource(R.string.zen_bible_language)
    val verses by vm.bibleVerses.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val error by vm.chapterError.collectAsState()
    val verseList=rememberLazyListState()
    var jumpedToVerse by rememberSaveable {mutableStateOf(false)}
    val p=LocalEssencePalette.current
    val initialBook=BibleCatalog.book(initialBookCode ?: "")
    val validChapter=initialChapter?.takeIf{initialBook!=null && it in 1..initialBook.chapters}
    var stage by rememberSaveable {mutableStateOf(if(validChapter!=null) Stage.Verses else if(initialBook!=null) Stage.Chapters else Stage.Books)}
    var code by rememberSaveable {mutableStateOf(initialBook?.code ?: "GEN")}
    var chapter by rememberSaveable {mutableIntStateOf(validChapter ?: 1)}
    val book=vm.bookByCode(code) ?: BibleCatalog.books.first()
    fun back() {
        when(stage) {
            Stage.Books -> navController.popBackStack()
            Stage.Chapters -> if(initialBook!=null) navController.popBackStack() else {stage=Stage.Books}
            Stage.Verses -> {stage=Stage.Chapters}
        }
    }
    BackHandler(enabled=stage!=Stage.Books){back()}
    LaunchedEffect(language,stage,code,chapter) {
        vm.setLanguage(language)
        if(stage==Stage.Verses) vm.loadChapter(code,chapter)
    }
    LaunchedEffect(verses,loading,error,stage,code,chapter) {
        if(stage==Stage.Verses && !loading && error==null && verses.isNotEmpty() && verses.first().book==book.nameEn && verses.first().chapter==chapter) {
            prefs.saveReading(code,chapter)
        }
    }
    LaunchedEffect(verses,loading,error,stage) {
        if(!jumpedToVerse && initialVerseNumber!=null && stage==Stage.Verses && !loading && error==null &&
            code==initialBookCode && chapter==initialChapter && verses.firstOrNull()?.chapter==chapter) {
            val index=verses.indexOfFirst {it.verseNumber==initialVerseNumber}
            if(index>=0) {verseList.scrollToItem(index);jumpedToVerse=true}
        }
    }
    Column(Modifier.fillMaxSize().background(p.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton(onClick=::back){Icon(EssenceIcons.Back,stringResource(R.string.zen_back),tint=p.text)}
            Column(Modifier.weight(1f)) {
                Text(when(stage){Stage.Books->stringResource(R.string.zen_bible);Stage.Chapters->book.label(language);Stage.Verses->"${book.label(language)} $chapter"},
                    style=MaterialTheme.typography.titleLarge,color=p.text)
                if(stage==Stage.Verses) Text(stringResource(R.string.ess_reading_subtitle),style=MaterialTheme.typography.bodySmall,color=p.secondaryText)
            }
        }
        when(stage) {
            Stage.Books -> LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                items(vm.allBooks,key={it.code}) {b->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surface)
                        .clickable{code=b.code;stage=Stage.Chapters}.padding(18.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(EssenceIcons.Book,null,tint=p.accent,modifier=Modifier.size(27.dp));Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.label(language),style=MaterialTheme.typography.titleMedium,color=p.text)
                            Text(pluralStringResource(R.plurals.zen_chapter_count,b.chapters,b.chapters),style=MaterialTheme.typography.bodySmall,color=p.secondaryText)
                        }
                        Icon(EssenceIcons.Arrow,null,tint=p.secondaryText)
                    }
                }
                item{TranslationCredit(language)}
            }
            Stage.Chapters -> LazyVerticalGrid(columns=GridCells.Adaptive(64.dp),contentPadding=PaddingValues(20.dp),
                horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                items(book.chapters){index->
                    val number=index+1
                    val active=state.reading?.let{it.bookCode==code && it.chapter==number}==true
                    Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(if(active)p.accent else p.surface)
                        .clickable{chapter=number;stage=Stage.Verses},contentAlignment=Alignment.Center) {
                        Text(number.toString(),style=MaterialTheme.typography.titleMedium,color=if(active)p.onAccent else p.text)
                    }
                }
            }
            Stage.Verses -> when {
                loading -> Column(Modifier.fillMaxWidth().padding(32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color=p.accent);Spacer(Modifier.height(16.dp));Text(stringResource(R.string.zen_loading_chapter),color=p.secondaryText)
                }
                error!=null -> Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text(stringResource(R.string.zen_chapter_unavailable),color=p.text)
                    TextButton(onClick={vm.loadChapter(code,chapter)}){Text(stringResource(R.string.zen_retry))}
                }
                else -> LazyColumn(state=verseList,contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    items(verses,key={it.verseNumber}) {v->
                        val saved=SavedVerse(code,chapter,v.verseNumber,v.text,language)
                        val favorite=state.favoriteVerses.any{it.key==saved.key}
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top) {
                                Text(v.verseNumber.toString(),fontSize=12.sp,fontWeight=FontWeight.SemiBold,color=p.accent,modifier=Modifier.width(25.dp).padding(top=6.dp))
                                Text(v.text,style=MaterialTheme.typography.bodyLarge,color=p.text,modifier=Modifier.weight(1f))
                            }
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                                IconButton(onClick={prefs.toggleVerse(saved)},modifier=Modifier.size(48.dp)){Icon(if(favorite)EssenceIcons.Check else EssenceIcons.Bookmark,stringResource(if(favorite)R.string.ess_remove_favorite else R.string.ess_save_verse),tint=p.accent,modifier=Modifier.size(19.dp))}
                                IconButton(onClick={shareVerse(context,saved)},modifier=Modifier.size(48.dp)){Icon(EssenceIcons.Share,stringResource(R.string.ess_share_verse),tint=p.secondaryText,modifier=Modifier.size(19.dp))}
                            }
                        }
                    }
                    item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        TextButton(onClick={chapter--},enabled=chapter>1){Icon(EssenceIcons.Back,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(stringResource(R.string.zen_previous))}
                        TextButton(onClick={chapter++},enabled=chapter<book.chapters){Text(stringResource(R.string.zen_next));Spacer(Modifier.width(6.dp));Icon(EssenceIcons.Arrow,null,modifier=Modifier.size(18.dp))}
                    }}
                    item{TranslationCredit(language)}
                }
            }
        }
    }
}

@Composable
private fun TranslationCredit(language: String) {
    val p=LocalEssencePalette.current
    val uri=LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(vertical=16.dp)) {
        Text(stringResource(R.string.zen_translation_credit),style=MaterialTheme.typography.bodySmall,color=p.secondaryText)
        TextButton(onClick={uri.openUri(if(language=="pt")"https://ebible.org/details.php?id=porbr2018" else "https://ebible.org/details.php?id=eng-kjv2006")},contentPadding=PaddingValues()) {
            Text(stringResource(R.string.zen_translation_details),style=MaterialTheme.typography.labelMedium)
        }
    }
}
