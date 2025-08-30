package com.juliocezar.zensounds.ui

import SoundPlayerScreen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.ui.viewmodel.CarouselItem
import com.juliocezar.zensounds.ui.viewmodel.SoundCategory
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModel
import com.juliocezar.zensounds.ui.viewmodel.SoundViewModelFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PromotionalCarousel(
    items: List<CarouselItem>,
    onItemClick: (index: Int, item: CarouselItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(pagerState.pageCount) {
        while (true) {
            delay(3000L)
            if (pagerState.pageCount > 0) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 32.dp),
        pageSpacing = 16.dp
    ) { page ->
        val item = items[page]
        CarouselCard(item = item, onClick = { onItemClick(page, item) })
    }
}

@Composable
fun CarouselCard(item: CarouselItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth().padding(vertical = 10.dp)
            .aspectRatio(16 / 9f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = item.backgroundResId),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 100f
                    )
                )
        )
        Text(
            text = item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

@Composable
fun MainScreen(soundViewModel: SoundViewModel = viewModel(factory = SoundViewModelFactory(LocalContext.current))) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = Color(0xFF0C1C2C)
    ) { innerPadding ->
        NavHost(navController, startDestination = "zenSounds") {
            composable("zenSounds") { ZenSoundsApp(navController, soundViewModel, innerPadding) }
            composable("bible") { BibleScreen(soundViewModel, navController, innerPadding) }
            composable("soundPlayer/{soundName}") { backStackEntry ->
                val soundName = backStackEntry.arguments?.getString("soundName")
                if (soundName != null) {
                    SoundPlayerScreen(navController, soundViewModel, soundName)
                }
            }
        }
    }
}

@Composable
fun ZenSoundsApp(
    navController: NavController,
    soundViewModel: SoundViewModel,
    innerPadding: PaddingValues
) {
    val soundCategories by soundViewModel.soundCategories.collectAsState()
    val selectedSoundName by soundViewModel.selectedSound.collectAsState()

    val carouselItems = listOf(
        CarouselItem("Bible Versicles", R.drawable.promo_1),
        CarouselItem("Coming Soon", R.drawable.promo_2),
        CarouselItem("Coming Soon", R.drawable.promo_3)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ZenSoundsBannerAd(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ZenSounds",
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB0C4D4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }

        item {
            PromotionalCarousel(
                items = carouselItems,
                onItemClick = { index, item ->
                    if (index == 0) {
                        navController.navigate("bible")
                    }
                    // Futuras ações para outros slides podem ser adicionadas aqui
                    // else if (index == 1) { /* Outra ação */ }
                },
                modifier = Modifier
                    .height(200.dp)
                    .padding(vertical = 25.dp)
            )
        }

        items(soundCategories) { category ->
            SoundCategoryRow(
                category = category,
                selectedSoundName = selectedSoundName,
                onSoundClicked = { soundName ->
                    navController.navigate("soundPlayer/$soundName")
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SoundCategoryRow(
    category: SoundCategory,
    selectedSoundName: String?,
    onSoundClicked: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            text = category.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.sounds) { sound ->
                SoundCard(
                    sound = sound,
                    isSelected = selectedSoundName == sound.name,
                    onClick = { onSoundClicked(sound.name) }
                )
            }
        }
    }
}

@Composable
fun SoundCard(sound: com.juliocezar.zensounds.ui.viewmodel.Sound, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF89CFF0) else Color(0xFF24364D)

    Box(
        modifier = Modifier
            .size(width = 150.dp, height = 200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A2B3D))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = sound.backgroundResId),
            contentDescription = "${sound.name} background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 300f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sound.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun ZenSoundsBannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adView = remember { AdView(context) }
    //val adUnitId = "ca-app-pub-5167159527096735/1656937187" //ID real
    val adUnitId = "ca-app-pub-3940256099942544/9214589741" //ID de teste

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