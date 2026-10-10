package com.juliocezar.zensounds.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.*
import com.juliocezar.zensounds.ui.theme.*

object EssenceIcons {
    private fun icon(name: String, data: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(data).toNodes(), stroke=SolidColor(Color.Black), strokeLineWidth=1.65f,
            strokeLineCap=StrokeCap.Round, strokeLineJoin=StrokeJoin.Round).build()
    val Home = icon("Home", "M3,10 L12,3 L21,10 M5,9 L5,21 L10,21 L10,15 L14,15 L14,21 L19,21 L19,9")
    val Book = icon("Book", "M12,5 C8,2 4,3 2,4 L2,20 C6,18 9,18 12,21 C15,18 18,18 22,20 L22,4 C19,3 15,2 12,5 L12,21")
    val Leaf = icon("Leaf", "M5,20 C1,9 11,2 21,3 C20,15 14,20 5,20 M5,20 L16,8 M5,20 L3,23")
    val Heart = icon("Heart", "M12,21 C8,18 2,13 2,7 C2,1 9,1 12,6 C15,1 22,1 22,7 C22,13 16,18 12,21 Z")
    val Bookmark = icon("Bookmark", "M6,3 L18,3 L18,21 L12,17 L6,21 Z")
    val Share = icon("Share", "M12,16 L12,2 M7,7 L12,2 L17,7 M5,11 L3,11 L3,22 L21,22 L21,11 L19,11")
    val Arrow = icon("Arrow", "M4,12 L20,12 M14,6 L20,12 L14,18")
    val Back = icon("Back", "M20,12 L4,12 M10,6 L4,12 L10,18")
    val Play = icon("Play", "M8,4 L20,12 L8,20 Z")
    val Pause = icon("Pause", "M8,4 L8,20 M16,4 L16,20")
    val Sliders = icon("Sliders", "M3,6 L9,6 M13,6 L21,6 M3,12 L15,12 M19,12 L21,12 M3,18 L5,18 M9,18 L21,18 M9,4 L13,4 L13,8 L9,8 Z M15,10 L19,10 L19,14 L15,14 Z M5,16 L9,16 L9,20 L5,20 Z")
    val Moon = icon("Moon", "M20,15 C12,18 6,12 9,4 C2,7 1,16 7,20 C13,24 20,21 20,15 Z")
    val Sun = icon("Sun", "M16,12 A4,4 0,1 1,8,12 A4,4 0,1 1,16,12 M12,1 L12,3 M12,21 L12,23 M1,12 L3,12 M21,12 L23,12 M4,4 L6,6 M18,18 L20,20 M4,20 L6,18 M18,6 L20,4")
    val Check = icon("Check", "M4,12 L9,17 L20,6")
    val Close = icon("Close", "M6,6 L18,18 M18,6 L6,18")
    val Next = icon("Next", "M5,5 L15,12 L5,19 Z M19,4 L19,20")
    val Previous = icon("Previous", "M19,5 L9,12 L19,19 Z M5,4 L5,20")
    val Volume = icon("Volume", "M3,9 L7,9 L12,4 L12,20 L7,15 L3,15 Z M16,8 C19,10 19,14 16,16 M19,4 C24,8 24,16 19,20")
    val Crown = icon("Crown", "M3,8 L7,12 L12,4 L17,12 L21,8 L19,19 L5,19 Z M5,22 L19,22")
    val Delete = icon(
        "Delete",
        "M3,6 L21,6 M9,6 L9,3 L15,3 L15,6 M5,6 L6,21 L18,21 L19,6 M10,10 L10,17 M14,10 L14,17"
    )
}

@Composable
fun BotanicalArtwork(modifier: Modifier = Modifier, color: Color = LocalEssencePalette.current.foliage) {
    Canvas(modifier) {
        val w=size.width; val h=size.height
        drawPath(Path().apply { moveTo(w*.50f,h*1.1f); cubicTo(w*.58f,h*.67f,w*.55f,h*.4f,w*.60f,h*.07f) }, color, style=Stroke(w*.015f))
        val leaves=listOf(floatArrayOf(.47f,.18f,-25f),floatArrayOf(.67f,.28f,24f),floatArrayOf(.40f,.39f,-35f),floatArrayOf(.68f,.51f,28f),floatArrayOf(.40f,.61f,-40f),floatArrayOf(.64f,.78f,35f))
        leaves.forEach { (x,y,angle) ->
            rotate(angle, androidx.compose.ui.geometry.Offset(w*x,h*y)) {
                val path=Path().apply {
                    moveTo(w*x,h*(y+.21f))
                    cubicTo(w*(x-.21f),h*(y+.05f),w*(x-.07f),h*(y-.10f),w*x,h*(y-.18f))
                    cubicTo(w*(x+.13f),h*(y+.02f),w*(x+.09f),h*(y+.13f),w*x,h*(y+.21f))
                }
                drawPath(path,color)
            }
        }
    }
}

@Composable
fun ThinProgress(fraction: Float, modifier: Modifier = Modifier, color: Color = LocalEssencePalette.current.accent) {
    val p=LocalEssencePalette.current
    Box(modifier.height(3.dp).clip(CircleShape).background(p.track)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().background(color))
    }
}

fun verseReference(verse: SavedVerse): String = "${BibleCatalog.book(verse.bookCode)?.label(verse.language) ?: verse.bookCode} ${verse.chapter}:${verse.verseNumber}"
fun shareVerse(context: android.content.Context, verse: SavedVerse) {
    val intent=Intent(Intent.ACTION_SEND).apply {
        type="text/plain"
        putExtra(Intent.EXTRA_TEXT,"${verse.text}\n\n${verseReference(verse)}\nZenSounds\n\n${context.getString(R.string.zen_translation_credit)}\n" + if(verse.language=="pt") "https://ebible.org/details.php?id=porbr2018" else "https://ebible.org/details.php?id=eng-kjv2006")
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.ess_share_verse)))
}

@Composable
fun ThemePicker(onDismiss: () -> Unit) {
    val controller=LocalThemeController.current
    AlertDialog(onDismissRequest=onDismiss,
        title={ Text(stringResource(R.string.ess_appearance)) },
        text={ Column {
            Text(stringResource(R.string.ess_appearance_help), style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            ThemeMode.entries.forEach { mode ->
                TextButton(onClick={ controller.setMode(mode); onDismiss() }, modifier=Modifier.fillMaxWidth()) {
                    Icon(if (mode==ThemeMode.LIGHT) EssenceIcons.Sun else EssenceIcons.Moon, null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(when(mode){ThemeMode.SYSTEM->R.string.ess_theme_system;ThemeMode.LIGHT->R.string.ess_theme_light;ThemeMode.DARK->R.string.ess_theme_dark}), modifier=Modifier.weight(1f))
                    if(controller.mode==mode) Icon(EssenceIcons.Check,null)
                }
            }
        }},
        confirmButton={ TextButton(onClick=onDismiss){Text(stringResource(R.string.ess_close))} })
}

@Composable
fun ProInformation(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest=onDismiss, icon={Icon(EssenceIcons.Crown,null)},
        title={Text("ZenSounds Pro")},
        text={Text(stringResource(R.string.ess_pro_planned))},
        confirmButton={TextButton(onClick=onDismiss){Text(stringResource(R.string.ess_understood))}})
}
