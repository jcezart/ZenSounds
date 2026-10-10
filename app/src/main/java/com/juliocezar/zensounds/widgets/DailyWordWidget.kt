package com.juliocezar.zensounds.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.util.Log
import android.widget.RemoteViews
import com.juliocezar.zensounds.MainActivity
import com.juliocezar.zensounds.R
import com.juliocezar.zensounds.data.BibleCatalog
import com.juliocezar.zensounds.data.DailyWordStore
import com.juliocezar.zensounds.data.SavedVerse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

// O sorteio é compartilhado entre o card do app e todas as instâncias dos widgets.
data class WidgetReadingRequest(val bookCode: String, val chapter: Int, val verseNumber: Int, val requestId: Long)

open class DailyWordWidget : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DailyWordWidgets.SHUFFLE) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
            if (intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) !in ids) return
            updateAsync(context, shuffle = true)
        } else if (intent.action in listOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_LOCALE_CHANGED)) {
            updateAsync(context, shuffle = false)
        } else super.onReceive(context, intent)
    }
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) = updateAsync(context, shuffle = false)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) = updateAsync(context, shuffle = false)
    private fun updateAsync(context: Context, shuffle: Boolean) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) { DailyWordWidgets.updateAll(app, shuffle) }
            } catch (error: Exception) {
                Log.e("ZenSoundsWidget", "Widget update unavailable", error)
                DailyWordWidgets.showError(app)
            } finally { pending.finish() }
        }
    }
}
class DailyWordCompactWidget : DailyWordWidget()

object DailyWordWidgets {
    const val SHUFFLE = "com.juliocezar.zensounds.widget.SHUFFLE"
    const val EXTRA_BOOK = "zensounds_widget_book"
    const val EXTRA_CHAPTER = "zensounds_widget_chapter"
    const val EXTRA_VERSE = "zensounds_widget_verse"
    private val mutex = Mutex()
    private val providers = listOf(DailyWordWidget::class.java, DailyWordCompactWidget::class.java)

    fun readIntent(intent: Intent?): WidgetReadingRequest? {
        val code = intent?.getStringExtra(EXTRA_BOOK) ?: return null
        val book = BibleCatalog.book(code) ?: return null
        val chapter = intent.getIntExtra(EXTRA_CHAPTER, 0)
        val verse = intent.getIntExtra(EXTRA_VERSE, 0)
        if (chapter !in 1..book.chapters || verse !in 1..200) return null
        return WidgetReadingRequest(code, chapter, verse, System.nanoTime())
    }
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        for (provider in providers) {
            val ids = manager.getAppWidgetIds(ComponentName(context, provider))
            if (ids.isNotEmpty()) context.sendBroadcast(Intent(context, provider).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            })
        }
    }
    fun requestPin(context: Context, compact: Boolean): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(
            ComponentName(context, if (compact) DailyWordCompactWidget::class.java else DailyWordWidget::class.java), null, null)
    }
    suspend fun updateAll(context: Context, shuffle: Boolean = false) = mutex.withLock {
        val manager = AppWidgetManager.getInstance(context)
        val instances = providers.flatMap { provider ->
            manager.getAppWidgetIds(ComponentName(context, provider)).map { it to provider }
        }
        if (instances.isEmpty()) return@withLock
        val language = context.getString(R.string.zen_bible_language)
        val verse = DailyWordStore.load(context, language, shuffle)
        for ((id, provider) in instances) render(context, manager, id, provider, verse)
    }
    private fun render(context: Context, manager: AppWidgetManager, id: Int, provider: Class<out DailyWordWidget>, verse: SavedVerse?) {
        val compact = provider == DailyWordCompactWidget::class.java
        val reference = verse?.let { "${BibleCatalog.book(it.bookCode)?.label(it.language)} ${it.chapter}:${it.verseNumber}" }
        val shuffleIntent = Intent(context, provider).apply {
            action = SHUFFLE
            data = android.net.Uri.parse("zensounds://widget/shuffle/$id")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        val shuffle = PendingIntent.getBroadcast(context, id, shuffleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data = android.net.Uri.parse("zensounds://widget/open/$id")
            if (verse != null) {
                putExtra(EXTRA_BOOK, verse.bookCode); putExtra(EXTRA_CHAPTER, verse.chapter); putExtra(EXTRA_VERSE, verse.verseNumber)
            }
        }
        val openReading = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun layout(heightDp: Float): RemoteViews {
            val views = RemoteViews(context.packageName, if (compact) R.layout.zen_word_widget_compact else R.layout.zen_word_widget)
            views.setTextViewText(R.id.widget_verse, verse?.text ?: context.getString(R.string.ess_word_error))
            views.setTextViewText(R.id.widget_reference, reference ?: context.getString(R.string.ess_widget_retry))
            val lines = visibleLines(context, heightDp, compact)
            views.setInt(R.id.widget_verse, "setMaxLines", lines.coerceAtLeast(1))
            // Em tamanhos extremos, preserve a referência e o acesso ao capítulo.
            views.setViewVisibility(R.id.widget_verse, if (lines > 0) View.VISIBLE else View.INVISIBLE)
            views.setOnClickPendingIntent(R.id.widget_shuffle, shuffle)
            views.setOnClickPendingIntent(R.id.widget_content, openReading)
            views.setOnClickPendingIntent(R.id.widget_reference, openReading)
            return views
        }
        val options = manager.getAppWidgetOptions(id)
        @Suppress("DEPRECATION")
        val sizes = if (Build.VERSION.SDK_INT >= 31) options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            ?.filter { it.width.isFinite() && it.height.isFinite() && it.width > 0f && it.height > 0f }
            ?.distinct()?.take(16).orEmpty() else emptyList()
        val views = if (Build.VERSION.SDK_INT >= 31 && sizes.isNotEmpty()) {
            RemoteViews(sizes.associateWith { layout(it.height) })
        } else {
            val fallback = if (compact) 170 else 220
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, fallback).takeIf { it > 0 } ?: fallback
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight).coerceAtLeast(minHeight)
            // Android 11: a altura menor corresponde à orientação horizontal.
            RemoteViews(layout(minHeight.toFloat()), layout(maxHeight.toFloat()))
        }
        manager.updateAppWidget(id, views)
    }
    private fun visibleLines(context: Context, heightDp: Float, compact: Boolean): Int {
        val metrics = context.resources.displayMetrics
        fun dp(value: Float) = value * metrics.density
        fun font(size: Float, medium: Boolean = false): Paint.FontMetricsInt {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            paint.typeface = Typeface.create(if (medium) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, metrics)
            return paint.fontMetricsInt
        }
        fun fontHeight(size: Float, medium: Boolean = false): Float {
            val fm = font(size, medium)
            return (fm.bottom - fm.top).toFloat()
        }
        // No compacto, referência e sorteio dividem uma única linha de rodapé.
        // Inclui o padding inferior do texto e uma folga para arredondamento em pixels.
        val reserved = if (compact) {
            dp(24f) + fontHeight(10f, true) + dp(6f) +
                    maxOf(dp(48f), fontHeight(12f, true)) + dp(6f)
        } else {
            dp(40f) + fontHeight(10f, true) + dp(18f) +
                    fontHeight(12f, true) + dp(6f) + maxOf(dp(48f), fontHeight(11f)) + dp(6f)
        }
        val minimum = font(if (compact) 11f else 12f)
        return WidgetLineBudget.lines(dp(heightDp) - reserved,
            (minimum.bottom - minimum.top).toFloat(), (minimum.descent - minimum.ascent).toFloat() + dp(2f),
            if (compact) 12 else 10)
    }

    fun showError(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        for (provider in providers) for (id in manager.getAppWidgetIds(ComponentName(context, provider))) render(context, manager, id, provider, null)
    }
}

// Limita a prévia a linhas completas. O texto bíblico original permanece intacto.
internal object WidgetLineBudget {
    fun lines(available: Float, firstLine: Float, lineAdvance: Float, limit: Int): Int {
        if (!available.isFinite() || !firstLine.isFinite() || !lineAdvance.isFinite() ||
            firstLine <= 0f || lineAdvance <= 0f || limit <= 0 || available < firstLine) return 0
        return (1 + kotlin.math.floor(((available - firstLine) / lineAdvance).toDouble()).toInt()).coerceIn(1, limit)
    }
}
