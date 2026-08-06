package com.futurecode.voicelockscreen.notification

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.view.View
import com.bumptech.glide.Glide
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.databinding.PromoDialogBinding
import com.futurecode.voicelockscreen.utils.Utils
import com.futurecode.voicelockscreen.utils.Utils.getPromosListFromPrefs
import java.util.Objects


data class PromoData(
    val title: String,
    val description: String,
    val button: String,
    val image: Int
)

object InAppUtils {
    private var bannerTimer: CountDownTimer? = null // keep reference to current timer

    fun showInAppBanner(activity: Activity, isShowEveryTime: Boolean) {
        val myPreferenceHelper = MyApplication.app.prefManager
        val url = myPreferenceHelper.clickUrl/*.getString(AppConstants.CLICK_URL)*/
        if (url.isEmpty()) {
            return
        }
        val clickLimit = myPreferenceHelper.clickLimit
        val count: Int = myPreferenceHelper.clickCount
        if (isShowEveryTime) {
            showBanner(activity)
        } else {
            if (count % clickLimit == 0) {
                showBanner(activity)
            }
        }
        myPreferenceHelper.clickCount = count + 1
    }

    private fun showBanner(activity: Activity) {
        // Cancel old timer if exists
        InAppUtils.bannerTimer?.cancel()

        val builder = AlertDialog.Builder(activity)
        val view: View = activity.layoutInflater.inflate(R.layout.promo_dialog, null)
        builder.setView(view)
        builder.setCancelable(false)
        val bind: PromoDialogBinding = PromoDialogBinding.bind(view)
        val dialog = builder.create()
        Objects.requireNonNull(dialog.window)
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val list = getPromosListFromPrefs()
        if (list.isNotEmpty()) {
            val data = list[InAppUtils.getNextPromoIndex(
                list.size
            )]
            data.let {
                bind.tvTitle.text = it.title
                bind.tvTitle2.text = it.description
                bind.btnUpgrade.text = it.buttonText
                Glide.with(activity).load(it.image)
                    .into(bind.imgBanner)
                // Start countdown timer for 2 minutes (120000 ms)
                val totalTime = 2 * 60 * 1000L // 2 minutes
                InAppUtils.bannerTimer = object : CountDownTimer(totalTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val minutes = (millisUntilFinished / 1000) / 60
                        val seconds = (millisUntilFinished / 1000) % 60
                        bind.tvTimer.text = activity.getString(R.string.promo_countdown, minutes, seconds)
                    }

                    override fun onFinish() {
                        bind.tvTimer.text = ""
                    }
                }.start()

                bind.btnUpgrade.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }
                bind.btnMaybeLater.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }
                bind.btnClose.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }
                bind.root.setOnClickListener { v ->
                    Utils.openCustomChrome(activity, it.link ?: "")
                    InAppUtils.bannerTimer?.cancel()
                    dialog.dismiss()
                }

                dialog.show()
            }
        }

    }

    fun getNextPromoIndex(total: Int): Int {
        val prefs = MyApplication.app.prefManager
        val currentIndex = prefs.promoIndex
        val nextIndex = (currentIndex + 1) % total
        prefs.promoIndex = nextIndex
        return currentIndex
    }

    fun getPromoList(index: Int): PromoData {
        val promoList = listOf(
            PromoData(
                title = MyApplication.app.getString(R.string.promo_upgrade_diwali_bundle),
                description = MyApplication.app.getString(R.string.promo_blue_flame_dra),
                button = MyApplication.app.getString(R.string.upgrade_now),
                image = R.drawable.inapp1
            ),

            PromoData(
                title = MyApplication.app.getString(R.string.promo_upgrade_diwali_bundle),
                description = MyApplication.app.getString(R.string.promo_wall_ninja),
                button = MyApplication.app.getString(R.string.upgrade_now),
                image = R.drawable.inapp1
            ),

            PromoData(
                title = MyApplication.app.getString(R.string.promo_upgrade_diwali_bundle),
                description = MyApplication.app.getString(R.string.promo_eternal_essence),
                button = MyApplication.app.getString(R.string.upgrade_now),
                image = R.drawable.inapp1
            ),

            PromoData(
                title = MyApplication.app.getString(R.string.promo_upgrade_diwali_bundle),
                description = MyApplication.app.getString(R.string.promo_avenger),
                button = MyApplication.app.getString(R.string.upgrade_now),
                image = R.drawable.inapp1
            ),
            PromoData(
                title = MyApplication.app.getString(R.string.promo_upgrade_diwali_bundle),
                description = MyApplication.app.getString(R.string.promo_trouble),
                button = MyApplication.app.getString(R.string.upgrade_now),
                image = R.drawable.inapp1
            ),
            PromoData(
                title = MyApplication.app.getString(R.string.promo_upgrade_diwali_bundle),
                description = MyApplication.app.getString(R.string.promo_malachite_ninja),
                button = MyApplication.app.getString(R.string.upgrade_now),
                image = R.drawable.inapp1
            )
        )
        return promoList[index]
    }
}
