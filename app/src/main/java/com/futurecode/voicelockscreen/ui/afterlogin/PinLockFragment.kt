package com.futurecode.voicelockscreen.ui.afterlogin

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPinLockBinding
import com.futurecode.voicelockscreen.databinding.ItemPinOptionCardBinding
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.FullBleed
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

class PinLockFragment : BaseFragment<FragmentPinLockBinding>(FragmentPinLockBinding::inflate) {

    private enum class Step { MENU, ENTER, CONFIRM, TEST }

    private var step = Step.MENU
    private var pinLength = DEFAULT_PIN_LENGTH
    private var firstPin: String? = null
    /** The PIN saved in this session, used by the test screen (keeps real lockout counters untouched). */
    private var savedPin: String? = null
    private val pinInput = StringBuilder()
    private val testInput = StringBuilder()

    private var previousStatusBarColor = Color.TRANSPARENT
    private var previousNavigationBarColor = Color.TRANSPARENT
    private var previousLightStatusBar = true
    private var previousLightNavigationBar = true

    // region Lifecycle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AdPlacement.loadAndShowNative(viewLifecycleOwner, binding.pinNativeAdContainer)
        if (savedInstanceState != null) restoreState(savedInstanceState)
        applySystemInsets()
        setupHeader()
        setupMenu()
        setupPinInput()
        setupTestKeypad()
        setupBackHandling()
        render()
    }

    override fun onDestroyView() {
        if (step == Step.TEST) restoreSystemBars()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STEP, step.name)
        outState.putInt(KEY_LENGTH, pinLength)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        pinLength = savedInstanceState?.getInt(KEY_LENGTH) ?: SecurePinStore.getPinLength(requireContext())
        step = savedInstanceState?.getString(KEY_STEP)?.let { savedStep -> Step.entries.firstOrNull { it.name == savedStep } } ?: Step.MENU
        if (step == Step.TEST && savedPin == null) step = Step.MENU
        if (step == Step.CONFIRM && firstPin == null) step = Step.ENTER
        if (step == Step.ENTER && pinLength !in PIN_LENGTHS) pinLength = DEFAULT_PIN_LENGTH
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.pinRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.clSetup.updatePadding(top = bars.top, bottom = bars.bottom)
            binding.clTestContent.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    // endregion

    // region Setup

    private fun setupHeader() {
        binding.btnBack.setOnClickListener { navigateBack() }
    }

    private fun setupMenu() {
        bindOption(
            binding.cardPinSetup,
            R.drawable.ic_pin,
            R.string.pin_setup_title,
            R.string.pin_setup_card_desc,
            R.string.pin_setup_now
        ) {
            pinLength = DEFAULT_PIN_LENGTH
            goTo(Step.ENTER)
        }
        bindOption(
            binding.cardRecovery,
            R.drawable.ic_security_question,
            R.string.recovery_password_title,
            R.string.recovery_password_card_desc,
            R.string.recovery_set_question
        ) {
            findNavController().navigate(R.id.securityQuestionFragment, Bundle().apply { putBoolean("editing", true) })
        }
    }

    private fun bindOption(
        card: ItemPinOptionCardBinding,
        icon: Int,
        title: Int,
        description: Int,
        action: Int,
        onClick: () -> Unit
    ) {
        card.ivOptionIcon.setImageResource(icon)
        card.tvOptionTitle.setText(title)
        card.tvOptionDesc.setText(description)
        card.btnOption.setText(action)
        card.btnOption.setOnClickListener { onClick() }
    }

    private fun setupPinInput() {
        val entry = binding.pinEntry
        val keys = listOf(
            entry.entryKey0, entry.entryKey1, entry.entryKey2, entry.entryKey3,
            entry.entryKey4, entry.entryKey5, entry.entryKey6, entry.entryKey7,
            entry.entryKey8, entry.entryKey9
        )
        keys.forEachIndexed { digit, key ->
            key.setOnClickListener {
                if (pinInput.length >= pinLength) return@setOnClickListener
                pinInput.append(digit)
                renderPinPrompt()
                renderPinDots()
                if (pinInput.length == pinLength) onPinEntered(pinInput.toString())
            }
        }
        entry.entryKeyDelete.setOnClickListener {
            if (pinInput.isNotEmpty()) pinInput.deleteCharAt(pinInput.lastIndex)
            renderPinPrompt()
            renderPinDots()
        }
        entry.clearPin.setOnClickListener {
            pinInput.clear()
            renderPinPrompt()
            renderPinDots()
        }
        entry.pinLengths.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || step != Step.ENTER) return@addOnButtonCheckedListener
            val length = if (checkedId == R.id.pinLength6) 6 else 4
            if (length == pinLength) return@addOnButtonCheckedListener
            pinLength = length
            pinInput.clear()
            renderPinStep()
        }
    }

    private fun setupTestKeypad() {
        val keys = mapOf(
            binding.testKey0 to '0', binding.testKey1 to '1', binding.testKey2 to '2',
            binding.testKey3 to '3', binding.testKey4 to '4', binding.testKey5 to '5',
            binding.testKey6 to '6', binding.testKey7 to '7', binding.testKey8 to '8',
            binding.testKey9 to '9'
        )
        keys.forEach { (key, digit) -> key.setOnClickListener { onTestDigit(digit) } }
        binding.testKeyDelete.setOnClickListener {
            if (testInput.isNotEmpty()) testInput.deleteCharAt(testInput.lastIndex)
            renderTestDots()
        }
        binding.btnTestSkip.setOnClickListener { finishFlow() }
    }

    private fun setupBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = navigateBack()
            }
        )
    }

    // endregion

    // region Events

    private fun onPinEntered(pin: String) {
        when (step) {
            Step.ENTER -> {
                firstPin = pin
                goTo(Step.CONFIRM)
            }
            Step.CONFIRM -> {
                if (pin != firstPin) {
                    pinInput.clear()
                    renderPinDots()
                    renderPinPrompt(isError = true)
                    shake(binding.pinEntry.pinEntryDots)
                    return
                }
                SecurePinStore.savePin(requireContext(), pin)
                LockService.enableAndStart(requireContext())
                firstPin = null
                savedPin = pin
                showSetupSuccess(R.string.pin_setup_success) { goTo(Step.TEST) }
            }
            else -> Unit
        }
    }

    private fun onTestDigit(digit: Char) {
        if (testInput.length >= pinLength) return
        testInput.append(digit)
        binding.tvTestPrompt.setText(R.string.pin_please_enter)
        renderTestDots()
        if (testInput.length < pinLength) return

        val entered = testInput.toString()
        val correct = savedPin?.let { it == entered } ?: SecurePinStore.verifyPin(requireContext(), entered)
        if (correct) {
            showSetupSuccess(R.string.pin_test_success, ::finishFlow)
        } else {
            binding.tvTestPrompt.setText(R.string.pin_wrong)
            shake(binding.testDots)
            testInput.clear()
            binding.testDots.postDelayed({ if (bindingOrNull != null) renderTestDots() }, SHAKE_MS)
        }
    }

    private fun navigateBack() {
        when (step) {
            Step.MENU -> findNavController().popBackStack()
            Step.ENTER -> goTo(Step.MENU)
            Step.CONFIRM -> {
                firstPin = null
                goTo(Step.ENTER)
            }
            Step.TEST -> finishFlow()
        }
    }

    private fun finishFlow() {
        if (step == Step.TEST) restoreSystemBars()
        findNavController().popBackStack()
    }

    // endregion

    // region Rendering

    private fun goTo(next: Step) {
        val leavingTest = step == Step.TEST && next != Step.TEST
        pinInput.clear()
        step = next
        if (leavingTest) restoreSystemBars()
        render()
        binding.setupScroll.scrollTo(0, 0)
    }

    private fun render() {
        val isMenu = step == Step.MENU
        val isEntry = step == Step.ENTER || step == Step.CONFIRM

        binding.cardPinSetup.root.isVisible = isMenu
        binding.cardRecovery.root.isVisible = isMenu
        binding.pinMenuAdSlot.isVisible = isMenu
        binding.pinEntry.root.isVisible = isEntry
        binding.clTest.isVisible = step == Step.TEST
        binding.tvToolbarTitle.setText(if (isEntry) R.string.pin_setup_screen_title else R.string.pin_setup_title)

        when (step) {
            Step.ENTER, Step.CONFIRM -> renderPinStep()
            Step.TEST -> renderTest()
            Step.MENU -> Unit
        }
    }

    private fun renderPinStep() {
        val entry = binding.pinEntry
        entry.pinLengths.isVisible = step == Step.ENTER
        entry.pinLengths.check(if (pinLength == 6) R.id.pinLength6 else R.id.pinLength4)
        entry.pinLength4.setIconResource(if (pinLength == 4) R.drawable.ic_pin_check else 0)
        entry.pinLength6.setIconResource(if (pinLength == 6) R.drawable.ic_pin_check else 0)
        renderPinPrompt()
        renderPinDots()
    }

    private fun renderPinPrompt(isError: Boolean = false) {
        binding.pinEntry.pinPrompt.apply {
            text = when {
                isError -> getString(R.string.pin_mismatch)
                step == Step.CONFIRM -> getString(R.string.pin_setup_confirm_prompt)
                else -> getString(R.string.pin_setup_enter_prompt, pinLength)
            }
            setTextColor(ContextCompat.getColor(context, if (isError) R.color.error else R.color.textPrimary))
        }
    }

    private fun renderPinDots() {
        val dots = binding.pinEntry.pinEntryDots
        if (dots.childCount != pinLength) {
            dots.removeAllViews()
            val size = resources.getDimensionPixelSize(R.dimen.credential_pin_dot_size)
            val margin = resources.getDimensionPixelSize(R.dimen.spacing_sm)
            repeat(pinLength) {
                dots.addView(View(requireContext()), LinearLayout.LayoutParams(size, size).apply {
                    marginStart = margin
                    marginEnd = margin
                })
            }
        }
        dots.children.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < pinInput.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
            )
        }
        dots.contentDescription = getString(R.string.pin_setup_progress, pinInput.length, pinLength)
    }

    private fun renderTest() {
        applyTestWallpaper()
        enterImmersiveBars()
        testInput.clear()
        binding.tvTestPrompt.setText(R.string.pin_please_enter)
        buildTestDots()
        renderTestDots()
    }

    private fun buildTestDots() {
        binding.testDots.removeAllViews()
        repeat(pinLength) {
            val dot = View(requireContext())
            binding.testDots.addView(dot, LinearLayout.LayoutParams(dp(10), dp(10)).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            })
        }
    }

    private fun renderTestDots() {
        binding.testDots.children.forEachIndexed { index, dot ->
            dot.setBackgroundResource(if (index < testInput.length) R.drawable.bg_pin_test_dot_on else R.drawable.bg_pin_test_dot_off)
        }
    }

    private fun applyTestWallpaper() {
        ThemeWallpaperStore.applyTo(binding.ivTestWallpaper, requireContext())
    }

    // endregion

    // region Helpers

    private fun enterImmersiveBars() {
        val window = requireActivity().window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        previousStatusBarColor = window.statusBarColor
        previousNavigationBarColor = window.navigationBarColor
        previousLightStatusBar = controller.isAppearanceLightStatusBars
        previousLightNavigationBar = controller.isAppearanceLightNavigationBars
        // Draw the wallpaper behind the status and navigation bars, like a real lock screen.
        FullBleed.set(activity, true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        ViewCompat.requestApplyInsets(binding.pinRoot)
    }

    private fun restoreSystemBars() {
        val activity = activity ?: return
        val window = activity.window
        window.statusBarColor = if (previousStatusBarColor == Color.TRANSPARENT) {
            ContextCompat.getColor(activity, R.color.bg)
        } else {
            previousStatusBarColor
        }
        if (previousNavigationBarColor != Color.TRANSPARENT) window.navigationBarColor = previousNavigationBarColor
        WindowCompat.setDecorFitsSystemWindows(window, true)
        FullBleed.set(activity, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = previousLightStatusBar
        controller.isAppearanceLightNavigationBars = previousLightNavigationBar
        bindingOrNull?.let { ViewCompat.requestApplyInsets(it.pinRoot) }
    }

    private fun shake(view: View) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, 18f, -18f, 12f, -12f, 6f, 0f).apply {
            duration = SHAKE_MS
            start()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    // endregion

    private companion object {
        const val KEY_STEP = "pin_step"
        const val KEY_LENGTH = "pin_length"
        const val DEFAULT_PIN_LENGTH = 4
        val PIN_LENGTHS = setOf(4, 6)
        const val SHAKE_MS = 400L
    }
}
