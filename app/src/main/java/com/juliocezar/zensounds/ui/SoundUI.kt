package com.juliocezar.zensounds.ui

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.services.PlaybackCommands
import com.juliocezar.zensounds.widgets.WidgetReadingRequest
import com.juliocezar.zensounds.data.*
import com.juliocezar.zensounds.ui.theme.*
import com.juliocezar.zensounds.ui.viewmodel.*
import com.juliocezar.zensounds.ui.viewmodel.Sound as UiSound
import kotlinx.coroutines.delay

@Composable
fun MainScreen(soundViewModel: SoundViewModel = viewModel(factory=SoundViewModelFactory(LocalContext.current.applicationContext)), widgetReading: WidgetReadingRequest? = null) {
    val home: HomeViewModel = viewModel(factory=HomeViewModelFactory(LocalContext.current.applicationContext))
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "zenSounds"
    val selected by soundViewModel.selectedSound.collectAsState()
    val p=LocalEssencePalette.current
    var themeOpen by rememberSaveable { mutableStateOf(false) }
    var widgetsOpen by rememberSaveable { mutableStateOf(false) }
    val language=stringResource(R.string.zen_bible_language)
    val lifecycle=LocalContext.current as? androidx.lifecycle.LifecycleOwner
    LaunchedEffect(language) {
        home.refresh(language)
        // Atualiza o versículo se o app ficar aberto durante a virada do dia.
        while(true) { delay(60_000); home.refresh(language) }
    }
    DisposableEffect(lifecycle, language) {
        val observer=LifecycleEventObserver { _, event -> if(event==Lifecycle.Event.ON_RESUME) home.refresh(language) }
        lifecycle?.lifecycle?.addObserver(observer)
        onDispose { lifecycle?.lifecycle?.removeObserver(observer) }
    }
    fun top(destination: String) {
        if (nav.currentDestination?.route == destination) return

        if (nav.popBackStack(destination, inclusive = false)) return

        nav.navigate(destination) {
            popUpTo(nav.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }
    fun chapter(code: String, number: Int) { nav.navigate("chapter/$code/$number") { launchSingleTop=true } }
    fun player(name: String) { nav.navigate("soundPlayer/${Uri.encode(name)}") { launchSingleTop=true } }
    fun mixPlayer(id: String = "current") { nav.navigate("mixPlayer/${Uri.encode(id)}") { launchSingleTop = true } }
    fun mix() { nav.navigate("mix") { launchSingleTop = true } }
    LaunchedEffect(widgetReading?.requestId) {
        widgetReading?.let { request ->
            nav.navigate("widgetChapter/${request.bookCode}/${request.chapter}/${request.verseNumber}") {
                popUpTo(nav.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }
    fun exploreBook(code: String) { nav.navigate("explore/$code") { launchSingleTop=true } }
    Scaffold(containerColor=p.background,
        bottomBar={Column(Modifier.background(p.background).navigationBarsPadding()) {
            if(selected!=null && !route.startsWith("soundPlayer") && route!="mix" && !route.startsWith("mixPlayer")) MiniSoundPlayer(soundViewModel) { if(selected==PlaybackCommands.MIX_ID) mixPlayer(soundViewModel.activeMixId.value ?: "current") else selected?.let(::player) }
            EssenceBottomNavigation(route, ::top)
        }}) { padding ->
        NavHost(navController=nav, startDestination="zenSounds", modifier=Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("zenSounds") {
                EssenceHome(home,soundViewModel,{themeOpen=true},::mix,{widgetsOpen=true},::chapter,
                    ::exploreBook, {top("sounds")})
            }
            composable("bible") { BibleScreen(soundViewModel,nav,PaddingValues(), preferences=home.preferences) }
            composable("psalms") { BibleScreen(soundViewModel,nav,PaddingValues(), initialBookCode="PSA",preferences=home.preferences) }
            composable("explore/{bookCode}") { backStack ->
                BibleScreen(soundViewModel,nav,PaddingValues(),initialBookCode=backStack.arguments?.getString("bookCode"),preferences=home.preferences)
            }
            composable("chapter/{bookCode}/{chapter}") { backStack ->
                BibleScreen(soundViewModel,nav,PaddingValues(),
                    initialBookCode=backStack.arguments?.getString("bookCode"),
                    initialChapter=backStack.arguments?.getString("chapter")?.toIntOrNull(),preferences=home.preferences)
            }
            composable("widgetChapter/{bookCode}/{chapter}/{verse}") { backStack ->
                BibleScreen(soundViewModel,nav,PaddingValues(),
                    initialBookCode=backStack.arguments?.getString("bookCode"),
                    initialChapter=backStack.arguments?.getString("chapter")?.toIntOrNull(),
                    preferences=home.preferences,
                    initialVerseNumber=backStack.arguments?.getString("verse")?.toIntOrNull())
            }
            composable("mix") { SoundMixScreen(soundViewModel,home.preferences, { id -> mixPlayer(id) }) { nav.popBackStack() } }
            composable("sounds") { SoundsLibrary(soundViewModel,home.preferences,::player,::mix, { id -> mixPlayer(id) }) }
            composable("favorites") { FavoritesScreen(soundViewModel,home.preferences,::player,::chapter) }
            composable("mixPlayer/{mixId}") { backStack ->
                SoundPlayerScreen(nav,soundViewModel,"",home.preferences,::mix,
                    initialMixId=backStack.arguments?.getString("mixId") ?: "current")
            }
            composable("soundPlayer/{soundName}") { backStack ->
                SoundPlayerScreen(nav,soundViewModel,backStack.arguments?.getString("soundName") ?: "Rain",home.preferences,::mix)
            }
        }
    }
    if(themeOpen) ThemePicker {themeOpen=false}
    if(widgetsOpen) WidgetPicker {widgetsOpen=false}
}

@Composable
private fun EssenceBottomNavigation(route: String, navigate: (String)->Unit) {
    val p=LocalEssencePalette.current
    val selected=when {
        route=="bible" || route=="psalms" || route.startsWith("chapter") || route.startsWith("explore/") || route.startsWith("widgetChapter/") -> "bible"
        route=="sounds" || route=="mix" || route.startsWith("mixPlayer") || route.startsWith("soundPlayer") -> "sounds"
        route=="favorites" -> "favorites"
        else -> "zenSounds"
    }
    val tabs=listOf(Triple("zenSounds",R.string.ess_home,EssenceIcons.Home),Triple("bible",R.string.zen_bible,EssenceIcons.Book),
        Triple("sounds",R.string.ess_sounds,EssenceIcons.Leaf),Triple("favorites",R.string.ess_favorites,EssenceIcons.Heart))
    Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=4.dp)) {
        tabs.forEach { (destination,label,icon) ->
            val active=selected==destination
            val tint=if(active) p.accent else p.secondaryText
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(onClickLabel=stringResource(label)){navigate(destination)}.padding(vertical=8.dp),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(icon,null,tint=tint,modifier=Modifier.size(25.dp))
                Spacer(Modifier.height(5.dp))
                Text(stringResource(label),color=tint,style=MaterialTheme.typography.labelMedium,maxLines=1)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(26.dp).height(2.dp).background(if(active)p.accent else Color.Transparent,CircleShape))
            }
        }
    }
}

@Composable
private fun EssenceHome(home: HomeViewModel, sounds: SoundViewModel, appearance:()->Unit, mix:()->Unit, widgets:()->Unit,
                        chapter:(String,Int)->Unit, exploreBook:(String)->Unit, allSounds:()->Unit) {
    val prefs by home.preferences.state.collectAsState()
    val daily by home.daily.collectAsState()
    val categories by sounds.soundCategories.collectAsState()
    val selected by sounds.selectedSound.collectAsState()
    val playing by sounds.isPlaying.collectAsState()
    val language=stringResource(R.string.zen_bible_language)
    val context=LocalContext.current
    val p=LocalEssencePalette.current
    var readingMode by rememberSaveable { mutableStateOf(true) }
    val choices=categories.flatMap {it.sounds}
    val featured=listOfNotNull(choices.find {it.name=="Rain"},choices.find {it.name=="Forest"})
    val explorationBook=BibleCatalog.book(daily.bookCode ?: daily.verse?.bookCode ?: "")
    LazyColumn(contentPadding=PaddingValues(start=20.dp,end=20.dp,top=12.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    Text("Zen",fontFamily=EssenceFont,fontSize=25.sp,fontWeight=FontWeight.Bold,color=p.text,letterSpacing=(-1).sp)
                    Text("Sounds",fontFamily=EssenceFont,fontSize=25.sp,fontWeight=FontWeight.Bold,color=p.accent,letterSpacing=(-1).sp)
                }
                IconButton(onClick=appearance) {Icon(EssenceIcons.Moon,stringResource(R.string.ess_appearance),tint=p.secondaryText,modifier=Modifier.size(21.dp))}
                IconButton(onClick=widgets) { Icon(EssenceIcons.Widgets,stringResource(R.string.ess_widgets),tint=p.secondaryText,modifier=Modifier.size(22.dp)) }
            }
        }
        item {Column(Modifier.padding(top=8.dp,bottom=2.dp)) {
            Row {Text(stringResource(R.string.ess_presence),style=MaterialTheme.typography.displaySmall,color=p.text);Text(".",style=MaterialTheme.typography.displaySmall,color=p.accent)}
            Row {Text(stringResource(R.string.ess_peace),style=MaterialTheme.typography.displaySmall,color=p.text);Text(".",style=MaterialTheme.typography.displaySmall,color=p.accent)}
        }}
        item {
            Row(Modifier.fillMaxWidth().clip(CircleShape).background(p.lilac)) {
                listOf(true to R.string.ess_read,false to R.string.ess_relax).forEach {(read,label)->
                    val active=readingMode==read
                    Row(Modifier.weight(1f).clip(CircleShape).background(if(active)p.accent else Color.Transparent)
                        .clickable {readingMode=read}.padding(vertical=13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                        Icon(if(read)EssenceIcons.Book else EssenceIcons.Leaf,null,tint=if(active)p.onAccent else p.text,modifier=Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp));Text(stringResource(label),style=MaterialTheme.typography.titleMedium,color=if(active)p.onAccent else p.text)
                    }
                }
            }
        }
        if(readingMode) {
            item {
                VerseHero(daily,prefs.favoriteVerses,home.preferences::toggleVerse,{v->shareVerse(context,v)},
                    {v->chapter(v.bookCode,v.chapter)}, {home.refresh(language,true)}, {home.shuffle(language)})
            }
            item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                val reading=prefs.reading
                val book=BibleCatalog.book(reading?.bookCode ?: "GEN")!!
                Column(Modifier.weight(1.25f).heightIn(min=140.dp).clip(RoundedCornerShape(20.dp)).background(p.surface)
                    .clickable {chapter(book.code,reading?.chapter ?: 1)}.padding(16.dp)) {
                    Icon(EssenceIcons.Book,null,tint=p.text,modifier=Modifier.size(27.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(if(reading==null)R.string.ess_start_bible else R.string.ess_resume_bible),style=MaterialTheme.typography.titleMedium,color=p.text)
                    Text(stringResource(R.string.ess_reading_position,book.label(language),reading?.chapter ?: 1),style=MaterialTheme.typography.bodySmall,color=p.secondaryText,modifier=Modifier.padding(top=4.dp))
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        ThinProgress((reading?.chapter ?: 1).toFloat()/book.chapters,Modifier.weight(1f))
                        Spacer(Modifier.width(10.dp))
                        Icon(EssenceIcons.Arrow,null,tint=p.secondaryText,modifier=Modifier.size(20.dp))
                    }
                }
                Box(Modifier.weight(1f).heightIn(min=140.dp).clip(RoundedCornerShape(20.dp)).background(p.lilac).clickable(enabled=explorationBook!=null){explorationBook?.let{exploreBook(it.code)}}) {
                    BotanicalArtwork(Modifier.align(Alignment.CenterEnd).width(75.dp).height(145.dp),p.foliage.copy(alpha=.8f))
                    Column(Modifier.padding(16.dp)) {
                        Text(if(explorationBook!=null)stringResource(R.string.ess_explore_book,explorationBook.label(language)) else stringResource(R.string.zen_bible),style=MaterialTheme.typography.titleMedium,color=p.text,modifier=Modifier.padding(end=20.dp))
                        Spacer(Modifier.height(32.dp))
                        Icon(EssenceIcons.Arrow,null,tint=p.secondaryText,modifier=Modifier.size(23.dp))
                    }
                }
            }}
        } else {
            item {Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.lilac).padding(20.dp)) {
                Text(stringResource(R.string.ess_relax_title),style=MaterialTheme.typography.titleLarge,color=p.text)
                Text(stringResource(R.string.ess_relax_description),style=MaterialTheme.typography.bodyMedium,color=p.secondaryText,modifier=Modifier.padding(top=8.dp))
                Spacer(Modifier.height(16.dp))
                TextButton(onClick=allSounds,contentPadding=PaddingValues()){Text(stringResource(R.string.ess_explore_sounds));Spacer(Modifier.width(8.dp));Icon(EssenceIcons.Arrow,null)}
            }}
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(stringResource(R.string.ess_sound_pause),style=MaterialTheme.typography.titleMedium,color=p.text,modifier=Modifier.weight(1f))
                TextButton(onClick=allSounds,contentPadding=PaddingValues(start=8.dp)){Text(stringResource(R.string.ess_see_all),style=MaterialTheme.typography.labelMedium)}
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                featured.forEach {sound->
                    SoundCircle(sound,selected==sound.name && playing,Modifier.weight(1f)){sounds.onSoundClicked(sound.name)}
                }
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(p.outline).clickable(onClick=mix),contentAlignment=Alignment.Center) {
                        Icon(EssenceIcons.Sliders,null,tint=p.secondaryText,modifier=Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(8.dp));Text(stringResource(R.string.ess_my_mix),style=MaterialTheme.typography.bodySmall,color=p.text,maxLines=2)
                }
            }
        }
    }
}

@Composable
private fun VerseHero(state: DailyWordState, saved: List<SavedVerse>, favorite:(SavedVerse)->Unit,
                      share:(SavedVerse)->Unit, open:(SavedVerse)->Unit,retry:()->Unit,shuffle:()->Unit) {
    val p=LocalEssencePalette.current
    val verse=state.verse
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.verseBackground)) {
        Canvas(Modifier.matchParentSize()) {drawCircle(p.onVerse.copy(alpha=.08f),radius=size.width*.38f,center=androidx.compose.ui.geometry.Offset(size.width*.97f,size.height*.83f))}
        BotanicalArtwork(Modifier.align(Alignment.CenterEnd).width(110.dp).height(235.dp),Color(0xFF3C241C).copy(alpha=.28f))
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(if(verse==null)stringResource(R.string.ess_word_today) else stringResource(R.string.ess_today_reference,verseReference(verse)),
                fontFamily=EssenceFont,fontWeight=FontWeight.Medium,fontSize=10.sp,letterSpacing=1.5.sp,color=p.onVerse)
            Spacer(Modifier.height(18.dp))
            when {
                state.loading -> Row(Modifier.fillMaxWidth().heightIn(min=100.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    CircularProgressIndicator(color=p.onVerse,modifier=Modifier.size(24.dp));Spacer(Modifier.width(12.dp));Text(stringResource(R.string.zen_loading_chapter),color=p.onVerse)
                }
                verse==null -> Column(Modifier.heightIn(min=100.dp)) {
                    Text(stringResource(R.string.ess_word_error),color=p.onVerse,style=MaterialTheme.typography.bodyMedium)
                    TextButton(onClick=retry,colors=ButtonDefaults.textButtonColors(contentColor=p.onVerse)){Text(stringResource(R.string.zen_retry))}
                }
                else -> {
                    if(state.failed) Text(stringResource(R.string.ess_shuffle_error),color=p.onVerse,style=MaterialTheme.typography.bodySmall)
                    Text(verse.text,color=p.onVerse,fontFamily=EssenceFont,fontWeight=FontWeight.Normal,fontSize=26.sp,lineHeight=38.sp,letterSpacing=(-.6).sp)
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        val isSaved=saved.any{it.key==verse.key}
                        IconButton(onClick={favorite(verse)}){Icon(if(isSaved)EssenceIcons.Check else EssenceIcons.Bookmark,stringResource(if(isSaved)R.string.ess_remove_favorite else R.string.ess_save_verse),tint=p.onVerse)}
                        IconButton(onClick={share(verse)}){Icon(EssenceIcons.Share,stringResource(R.string.ess_share_verse),tint=p.onVerse)}
                        IconButton(onClick=shuffle,enabled=!state.loading) { Icon(EssenceIcons.Shuffle,stringResource(R.string.ess_shuffle_word),tint=p.onVerse) }
                        Spacer(Modifier.weight(1f))
                        Row(Modifier.clip(CircleShape).clickable{open(verse)}.padding(start=6.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(stringResource(R.string.ess_open_chapter),color=p.onVerse,style=MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(9.dp))
                            Box(Modifier.size(38.dp).background(p.onVerse,CircleShape),contentAlignment=Alignment.Center) {
                                Icon(EssenceIcons.Arrow,null,tint=p.verseBackground,modifier=Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundCircle(sound: UiSound,isPlaying: Boolean,modifier: Modifier=Modifier,click:()->Unit) {
    val p=LocalEssencePalette.current
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).clickable(onClickLabel=stringResource(if(isPlaying)R.string.zen_pause else R.string.zen_play),onClick=click)) {
            Image(painterResource(sound.backgroundResId),null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom=8.dp).size(30.dp).background(p.surface.copy(alpha=.96f),CircleShape),contentAlignment=Alignment.Center) {
                Icon(if(isPlaying)EssenceIcons.Pause else EssenceIcons.Play,null,tint=p.text,modifier=Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp));Text(localizedSoundName(sound.name),style=MaterialTheme.typography.bodySmall,color=p.text,maxLines=2)
    }
}

@Composable
private fun MiniSoundPlayer(viewModel: SoundViewModel,open:()->Unit) {
    val mixName by viewModel.activeMixName.collectAsState()
    val selected by viewModel.selectedSound.collectAsState()
    val categories by viewModel.soundCategories.collectAsState()
    val playing by viewModel.playWhenReady.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()
    val layers by viewModel.mixLayers.collectAsState()
    val remaining by viewModel.timerRemaining.collectAsState()
    val isMix=selected==PlaybackCommands.MIX_ID
    val sound=categories.flatMap {it.sounds}.find {it.name==selected}
    if(selected==null) return
    val artwork=if(isMix) SoundCatalog.artwork(layers.firstOrNull()?.soundName ?: "Rain") else sound?.backgroundResId ?: SoundCatalog.artwork(selected ?: "Rain")
    val p=LocalEssencePalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal=5.dp,vertical=4.dp).clip(RoundedCornerShape(18.dp)).background(p.surface).clickable(onClick=open).padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        Image(painterResource(artwork),null,Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if(isMix)mixName ?: stringResource(R.string.ess_mix_title) else localizedSoundName(selected ?: ""),style=MaterialTheme.typography.titleMedium,color=p.text,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(if(remaining>0)stringResource(R.string.ess_timer_remaining,timerLabel(remaining)) else stringResource(R.string.ess_ambient_sound),style=MaterialTheme.typography.bodySmall,color=p.secondaryText)
            ThinProgress(progress,Modifier.fillMaxWidth().padding(top=6.dp))
        }
        Spacer(Modifier.width(12.dp))
        FilledIconButton(onClick=viewModel::onPlayPauseClicked,modifier=Modifier.size(44.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=p.accent,contentColor=p.onAccent)) {
            Icon(if(playing)EssenceIcons.Pause else EssenceIcons.Play,stringResource(if(playing)R.string.zen_pause else R.string.zen_play),modifier=Modifier.size(23.dp))
        }
        IconButton(onClick=viewModel::onStopClicked,modifier=Modifier.size(48.dp)) {
            Icon(EssenceIcons.Close,stringResource(R.string.ess_close),tint=p.secondaryText,modifier=Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SoundsLibrary(viewModel: SoundViewModel,prefs: EssencePreferences,open:(String)->Unit,mix:()->Unit,openSavedMix:(String)->Unit) {
    val categories by viewModel.soundCategories.collectAsState()
    val state by prefs.state.collectAsState()
    val p=LocalEssencePalette.current
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item {Text(stringResource(R.string.ess_sounds),style=MaterialTheme.typography.headlineMedium,color=p.text)}
        item {Text(stringResource(R.string.ess_relax_description),style=MaterialTheme.typography.bodyMedium,color=p.secondaryText)}
        item { TextButton(onClick=mix) { Icon(EssenceIcons.Sliders,null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ess_my_mix)) } }
        if (state.savedMixes.isNotEmpty()) {
            item { Text(stringResource(R.string.ess_my_mixes),style=MaterialTheme.typography.titleLarge,color=p.text) }
            items(state.savedMixes,key={"mix:${it.id}"}) { saved ->
                SavedMixLibraryRow(saved,{openSavedMix(saved.id)},{prefs.removeMix(saved.id)})
            }
        }
        categories.forEach {category->
            item {Text(localizedCategoryName(category.title),style=MaterialTheme.typography.titleLarge,color=p.text)}
            items(category.sounds,key={it.name}) {sound->SoundLibraryRow(sound,state.favoriteSounds.contains(sound.name),{open(sound.name)},{prefs.toggleSound(sound.name)})}
        }
    }
}

@Composable
private fun SavedMixLibraryRow(mix: SavedMix,open:()->Unit,remove:()->Unit) {
    val p=LocalEssencePalette.current
    val context=LocalContext.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surface).clickable(onClick=open).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Image(painterResource(SoundCatalog.artwork(mix.layers.firstOrNull()?.soundName.orEmpty())),null,Modifier.size(65.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(mix.name,style=MaterialTheme.typography.titleMedium,color=p.text)
            Text(mix.layers.joinToString(" + "){LocalizedLabels.soundName(context,it.soundName)},style=MaterialTheme.typography.bodySmall,color=p.secondaryText,maxLines=2,overflow=TextOverflow.Ellipsis)
        }
        IconButton(onClick=remove){Icon(EssenceIcons.Delete,stringResource(R.string.ess_mix_delete),tint=p.accent)}
        Icon(EssenceIcons.Arrow,null,tint=p.secondaryText,modifier=Modifier.size(20.dp))
    }
}

@Composable
private fun SoundLibraryRow(sound: UiSound,favorite:Boolean,open:()->Unit,toggleFavorite:()->Unit) {
    val p=LocalEssencePalette.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surface).clickable(onClick=open).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
        Image(painterResource(sound.backgroundResId),null,Modifier.size(65.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
        Spacer(Modifier.width(14.dp));Text(localizedSoundName(sound.name),style=MaterialTheme.typography.titleMedium,color=p.text,modifier=Modifier.weight(1f))
        IconButton(onClick=toggleFavorite){Icon(if(favorite)EssenceIcons.Delete else EssenceIcons.Heart,stringResource(if(favorite)R.string.ess_remove_favorite else R.string.ess_add_favorite),tint=p.accent)}
        Icon(EssenceIcons.Arrow,null,tint=p.secondaryText,modifier=Modifier.size(20.dp))
    }
}

@Composable
private fun FavoritesScreen(viewModel: SoundViewModel,prefs: EssencePreferences,openSound:(String)->Unit,openChapter:(String,Int)->Unit) {
    val state by prefs.state.collectAsState()
    val categories by viewModel.soundCategories.collectAsState()
    val sounds=categories.flatMap{it.sounds}.filter{it.name in state.favoriteSounds}
    val language=stringResource(R.string.zen_bible_language)
    val verses=state.favoriteVerses.filter{it.language==language}
    val p=LocalEssencePalette.current
    val context=LocalContext.current
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {Text(stringResource(R.string.ess_favorites),style=MaterialTheme.typography.headlineMedium,color=p.text)}
        if(sounds.isEmpty() && verses.isEmpty()) item {Text(stringResource(R.string.ess_favorites_empty),style=MaterialTheme.typography.bodyLarge,color=p.secondaryText)}
        if(verses.isNotEmpty()) item {Text(stringResource(R.string.ess_saved_verses),style=MaterialTheme.typography.titleMedium,color=p.text)}
        items(verses,key={it.key}) {verse->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surface).padding(18.dp)) {
                Text(verseReference(verse),style=MaterialTheme.typography.labelMedium,color=p.accent)
                Text(verse.text,style=MaterialTheme.typography.bodyLarge,color=p.text,modifier=Modifier.padding(top=8.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={prefs.toggleVerse(verse)}){Icon(EssenceIcons.Delete,stringResource(R.string.ess_remove_favorite),tint=p.accent)}
                    IconButton(onClick={shareVerse(context,verse)}){Icon(EssenceIcons.Share,stringResource(R.string.ess_share_verse),tint=p.secondaryText)}
                    Spacer(Modifier.weight(1f));TextButton(onClick={openChapter(verse.bookCode,verse.chapter)}){Text(stringResource(R.string.ess_open_chapter))}
                }
            }
        }
        if(sounds.isNotEmpty()) item {Text(stringResource(R.string.ess_sounds),style=MaterialTheme.typography.titleMedium,color=p.text)}
        items(sounds,key={it.name}) {sound->SoundLibraryRow(sound,true,{openSound(sound.name)},{prefs.toggleSound(sound.name)})}
    }
}
