package com.futurecode.voicelockscreen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futurecode.voicelockscreen.activity.MainActivity
import com.futurecode.voicelockscreen.ui.prelogin.LanguageFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises regional language expansion without changing saved preferences. */
@RunWith(AndroidJUnit4::class)
class LanguagePickerDesignTest {
    @Test fun regionalPickerExpandsAndCollapses() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            android.os.SystemClock.sleep(600)
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, LanguageFragment()).commitNow()
            }
            android.os.SystemClock.sleep(400)
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.rvLanguages)
                assertEquals(15, list.adapter!!.itemCount)
                capture(activity.findViewById(R.id.main), "language_collapsed")
                list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
                assertEquals(21, list.adapter!!.itemCount)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.rvLanguages)
                capture(activity.findViewById(R.id.main), "language_expanded")
                list.findViewHolderForAdapterPosition(2)!!.itemView.performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val list = activity.findViewById<RecyclerView>(R.id.rvLanguages)
                assertTrue(list.findViewHolderForAdapterPosition(2)!!.itemView.isSelected)
                list.findViewHolderForAdapterPosition(0)!!.itemView.performClick()
                assertEquals(15, list.adapter!!.itemCount)
            }
        }
    }

    private fun capture(root: View, name: String) {
        assertTrue(root.width > 0 && root.height > 0)
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val folder = File(root.context.filesDir, "design-preview").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}