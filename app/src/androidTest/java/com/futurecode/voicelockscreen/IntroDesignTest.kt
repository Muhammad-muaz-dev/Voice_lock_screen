package com.futurecode.voicelockscreen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.voicelockscreen.activity.MainActivity
import com.futurecode.voicelockscreen.ui.prelogin.LanguageFragment
import com.futurecode.voicelockscreen.ui.prelogin.OnBoardingFragment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises regional expansion and onboarding paging without changing saved preferences. */
@RunWith(AndroidJUnit4::class)
class IntroDesignTest {
    @Test fun regionalPickerAndOnboardingPages() {
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
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, OnBoardingFragment()).commitNow()
            }
            android.os.SystemClock.sleep(400)
            instrumentation.waitForIdleSync()
            for (page in 0..2) {
                scenario.onActivity { activity ->
                    val pager = activity.findViewById<ViewPager2>(R.id.onBoardingPager)
                    assertEquals(page, pager.currentItem)
                    val pageRecycler = pager.getChildAt(0) as RecyclerView
                    val pageView = pageRecycler.findViewHolderForAdapterPosition(page)!!.itemView
                    val title = pageView.findViewById<TextView>(R.id.tvOnBoardingTitle)
                    val description = pageView.findViewById<TextView>(R.id.tvOnBoardingDescription)
                    val artwork = pageView.findViewById<View>(R.id.imgOnBoarding)
                    val copy = pageView.findViewById<View>(R.id.onboardingCopy)
                    assertTrue(title.text.isNotEmpty())
                    assertTrue(description.top >= title.bottom)
                    assertTrue(artwork.height > pager.height / 2)
                    assertTrue(artwork.bottom <= copy.top)
                    capture(activity.findViewById(R.id.main), "onboarding_${page + 1}")
                    if (page < 2) activity.findViewById<View>(R.id.btnNext).performClick()
                }
                // ViewPager's smooth scrolling is asynchronous; bounded settling before capture.
                android.os.SystemClock.sleep(600)
                instrumentation.waitForIdleSync()
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
