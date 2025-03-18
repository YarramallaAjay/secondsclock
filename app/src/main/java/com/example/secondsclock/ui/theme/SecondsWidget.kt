package com.example.secondsclock.ui.theme

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.example.secondsclock.R
import java.time.LocalTime

/**
 * Implementation of App Widget functionality.
 */
class SecondsWidget : AppWidgetProvider() {
    private val handler = Handler(Looper.getMainLooper())
    private var displayMode = DisplayMode.SECONDS

    enum class DisplayMode { MILLISECONDS, SECONDS, NORMAL_TIME }

    companion object {
        private const val ACTION_SWIPE_LEFT = "com.example.secondsclock.ACTION_SWIPE_LEFT"
        private const val ACTION_SWIPE_RIGHT = "com.example.secondsclock.ACTION_SWIPE_RIGHT"
        private const val PREFS_NAME = "com.example.secondsclock.ClockWidget"
        private const val PREF_DISPLAY_MODE = "display_mode"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        loadDisplayMode(context)

        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
        startUpdateRunnable(context)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.seconds_widget)
        updateTimeDisplay(views)

        views.setOnClickPendingIntent(R.id.button_left, getPendingIntent(context, ACTION_SWIPE_LEFT, appWidgetId))
        views.setOnClickPendingIntent(R.id.button_right, getPendingIntent(context, ACTION_SWIPE_RIGHT, appWidgetId))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            ACTION_SWIPE_LEFT -> {
                cycleDisplayMode(next = true, context)
                restartUpdateCycle(context)
            }
            ACTION_SWIPE_RIGHT -> {
                cycleDisplayMode(next = false, context)
                restartUpdateCycle(context)
            }
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SecondsWidget::class.java))
        appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }
    }

    private fun cycleDisplayMode(next: Boolean, context: Context) {
        displayMode = if (next) {
            when (displayMode) {
                DisplayMode.SECONDS -> DisplayMode.NORMAL_TIME
                DisplayMode.NORMAL_TIME -> DisplayMode.MILLISECONDS
                DisplayMode.MILLISECONDS -> DisplayMode.SECONDS
            }
        } else {
            when (displayMode) {
                DisplayMode.SECONDS -> DisplayMode.MILLISECONDS
                DisplayMode.MILLISECONDS -> DisplayMode.NORMAL_TIME
                DisplayMode.NORMAL_TIME -> DisplayMode.SECONDS
            }
        }

        // Save mode persistently
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_DISPLAY_MODE, displayMode.name)
            .apply()
    }

    private fun loadDisplayMode(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        displayMode = DisplayMode.valueOf(prefs.getString(PREF_DISPLAY_MODE, DisplayMode.SECONDS.name)!!)
    }

    private fun updateTimeDisplay(views: RemoteViews) {
        val now = LocalTime.now()
        val seconds = now.toSecondOfDay()
        val milliseconds = now.toNanoOfDay() / 1_000_000 % 1000

        val displayText = when (displayMode) {
            DisplayMode.MILLISECONDS -> "${seconds * 1000 + milliseconds}"
            DisplayMode.SECONDS -> "$seconds"
            DisplayMode.NORMAL_TIME ->"${now.hour} : ${now.minute}"
        }

        views.setTextViewText(R.id.clock_text, displayText)
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            val context = CurrentContextHolder.currentContext ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SecondsWidget::class.java))

            loadDisplayMode(context) // Reload display mode in each cycle

            appWidgetIds.forEach { updateAppWidget(context, appWidgetManager, it) }

            handler.postDelayed(this, if (displayMode == DisplayMode.MILLISECONDS) 50L else 1000L)
        }
    }

    private fun startUpdateRunnable(context: Context) {
        CurrentContextHolder.currentContext = context.applicationContext
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    private fun restartUpdateCycle(context: Context) {
        handler.removeCallbacks(updateRunnable) // Stop the existing loop
        startUpdateRunnable(context) // Restart it with the new display mode
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        handler.removeCallbacks(updateRunnable)
    }

    private fun getPendingIntent(context: Context, action: String, appWidgetId: Int): PendingIntent {
        val intent = Intent(context, SecondsWidget::class.java).apply {
            this.action = action
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private object CurrentContextHolder { var currentContext: Context? = null }
}
