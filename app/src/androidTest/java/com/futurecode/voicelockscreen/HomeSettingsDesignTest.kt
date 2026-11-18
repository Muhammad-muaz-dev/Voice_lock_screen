package com.futurecode.voicelockscreen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.ui.afterlogin.NotificationPermissionDialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class HomeSettingsDesignTest {
    @Test fun settingsNavigationAndNotificationLater() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("voice_lock_features", Context.MODE_PRIVATE)
        val hadAsked = prefs.contains("notif_asked")
        val asked = prefs.getBoolean("notif_asked", false)
        // Suppress the prompt for the initial home capture; restore the original value in finally.
        prefs.edit().putBoolean("notif_asked", true).commit()
        try {
            ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                }
                settle()
                scenario.onActivity { activity ->
                    assertNotNull(activity.findViewById<View>(R.id.switchUnlock))
                    assertNotNull(activity.findViewById<View>(R.id.switchLock))
                    capture(activity.findViewById(R.id.main), "home_midnight")
                    checkResponsiveLayouts(activity)
                    activity.findViewById<View>(R.id.ivSettings).performClick()
                }
                settle()
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    assertEquals(R.id.settingFragment, host.navController.currentDestination!!.id)
                    val language = activity.findViewById<View>(R.id.clChangeLanguage)
                    val rate = activity.findViewById<View>(R.id.btnRate)
                    assertTrue(language.height >= 48 * context.resources.displayMetrics.density)
                    assertNotNull(rate)
                    capture(activity.findViewById(R.id.main), "settings_midnight")
                    prefs.edit().putBoolean("notif_asked", false).commit()
                    activity.findViewById<View>(R.id.ivBack).performClick()
                }
                settle()
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val manager = host.childFragmentManager
                    val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    val existing = manager.findFragmentByTag(NotificationPermissionDialog.TAG)
                    if (needsPermission) assertNotNull("Home should show the notification explanation", existing)
                    val sheet = existing as? NotificationPermissionDialog ?: NotificationPermissionDialog().also {
                        it.showNow(manager, NotificationPermissionDialog.TAG)
                    }
                    sheet.dialog!!.window!!.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                }
                settle()
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val sheet = host.childFragmentManager.findFragmentByTag(NotificationPermissionDialog.TAG) as NotificationPermissionDialog
                    capture(sheet.requireView(), "notification_sheet_detail")
                    val screen = instrumentation.uiAutomation.takeScreenshot()
                    assertNotNull(screen)
                    save(context, screen, "notification_home_midnight")
                    sheet.requireView().findViewById<View>(R.id.btnNotificationLater).performClick()
                    assertTrue(prefs.getBoolean("notif_asked", false))
                }
                settle()
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    assertNull(host.childFragmentManager.findFragmentByTag(NotificationPermissionDialog.TAG))
                }
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                settle()
                scenario.onActivity { activity ->
                    val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    assertNull("Later must prevent repeated prompts", host.childFragmentManager.findFragmentByTag(NotificationPermissionDialog.TAG))
                }
            }
        } finally {
            prefs.edit().apply {
                if (hadAsked) putBoolean("notif_asked", asked) else remove("notif_asked")
            }.commit()
        }
    }

    private fun settle() {
        android.os.SystemClock.sleep(600)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun checkResponsiveLayouts(context: Context) {
        // Measure the production layouts at the supported widths with increased text size.
        // This changes only the test context, never the device's display or font settings.
        val configuration = Configuration(context.resources.configuration).apply { fontScale = 1.3f }
        val scaledContext = ContextThemeWrapper(context.createConfigurationContext(configuration), R.style.Theme_VoiceLockScreen)
        val density = scaledContext.resources.displayMetrics.density
        for (widthDp in listOf(360, 390, 412)) {
            for (layout in listOf(R.layout.fragment_home, R.layout.fragment_setting)) {
                val view = LayoutInflater.from(scaledContext).inflate(layout, FrameLayout(scaledContext), false)
                val width = (widthDp * density).toInt()
                val height = (844 * density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, height)
                assertTextFits(view)
            }
        }
    }

    private fun assertTextFits(view: View) {
        if (view is TextView && view.visibility == View.VISIBLE && view.text.isNotEmpty()) {
            val layout = view.layout
            assertNotNull("Missing text layout for ${view.text}", layout)
            assertTrue(
                "Clipped text: ${view.text}, width=${view.width}, height=${view.height}, " +
                    "lines=${layout.lineCount}, textBottom=${layout.getLineBottom(layout.lineCount - 1)}",
                layout.getLineBottom(layout.lineCount - 1) <=
                    view.height - view.compoundPaddingTop - view.compoundPaddingBottom
            )
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) assertTextFits(view.getChildAt(i))
    }

    private fun capture(root: View, name: String) {
        assertTrue(root.width > 0 && root.height > 0)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        save(root.context, bitmap, name)
    }

    private fun save(context: Context, bitmap: Bitmap, name: String) {
        val folder = File(context.filesDir, "design-preview").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
