package com.futurecode.voicelockscreen.ui.afterlogin

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPinLockBinding
import com.futurecode.voicelockscreen.databinding.ItemPinOptionCardBinding
import com.futurecode.voicelockscreen.databinding.ItemPinTypeCardBinding
import com.futurecode.voicelockscreen.databinding.ViewPinStepperBinding
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.FullBleed
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

/**
 * PIN setup, matching the reference flow:
 * menu (Pin Setup / Recovery Password) -> step 1 choose length -> step 2 enter ->
 * step 3 confirm -> step 4 success -> PIN lock test on the lock wallpaper.
 */
class PinLockFragment : BaseFragment<FragmentPinLockBinding>(FragmentPinLockBinding::inflate) {

    private enum class Step { MENU, TYPE, ENTER, CONFIRM, SUCCESS, TEST }

    private var step = Step.MENU
    private var pinLength = DEFAULT_PIN_LENGTH
    private var firstPin: String? = null
    /** The PIN saved in this session, used by the test screen (keeps real lockout counters untouched). */
    private var savedPin: String? = null
    private var pinVisible = false
    private val testInput = StringBuilder()

    private var previousStatusBarColor = Color.TRANSPARENT
    private var previousNavigationBarColor = Color.TRANSPARENT
    private var previousLightStatusBar = true
    private var previousLightNavigationBar = true

    // region Lifecycle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreState(savedInstanceState)
        applySystemInsets()
        setupHeader()
        setupMenu()
        setupTypeCards()
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
        outState.putString(KEY_FIRST_PIN, firstPin)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        pinLength = savedInstanceState?.getInt(KEY_LENGTH) ?: SecurePinStore.getPinLength(requireContext())
        if (pinLength !in PIN_LENGTHS) pinLength = DEFAULT_PIN_LENGTH
        firstPin = savedInstanceState?.getString(KEY_FIRST_PIN)
        step = savedInstanceState?.getString(KEY_STEP)?.let(Step::valueOf) ?: Step.MENU
        // The test needs the PIN in memory; after process death go back to the success step.
        if (step == Step.TEST) step = Step.SUCCESS
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
        binding.btnHeaderNext.setOnClickListener { goTo(Step.ENTER) }
        binding.btnHeaderDone.setOnClickListener { goTo(Step.TEST) }
    }

    private fun setupMenu() {
        bindOption(
            binding.cardPinSetup,
            R.drawable.img_pin_setup_icon,
            R.string.pin_setup_title,
            R.string.pin_setup_card_desc,
            R.string.pin_setup_now
        ) { goTo(Step.TYPE) }
        bindOption(
            binding.cardRecovery,
            R.drawable.img_pin_recovery_icon,
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

    private fun typeCards(): List<Pair<Int, ItemPinTypeCardBinding>> = listOf(
        4 to binding.typeFour,
        5 to binding.typeFive,
        6 to binding.typeSix,
        8 to binding.typeEight
    )

    private fun setupTypeCards() {
        typeCards().forEach { (length, card) ->
            card.tvTypeTitle.text = getString(R.string.pin_type_label, length)
            card.tvTypeMask.text = "*".repeat(length)
            card.root.contentDescription = getString(R.string.pin_type_label, length)
            card.root.setOnClickListener {
                pinLength = length
                renderTypeSelection()
            }
        }
    }

    private fun setupPinInput() {
        binding.etPin.doAfterTextChanged { text ->
            val value = text?.toString().orEmpty()
            clearInputError()
            if (value.length == pinLength) onPinEntered(value)
        }
        binding.btnTogglePin.setOnClickListener {
            pinVisible = !pinVisible
            renderPinVisibility()
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
                    showInputError()
                    return
                }
                SecurePinStore.savePin(requireContext(), pin)
                LockService.enableAndStart(requireContext())
                savedPin = pin
                goTo(Step.SUCCESS)
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
            Toast.makeText(requireContext(), R.string.pin_test_success, Toast.LENGTH_SHORT).show()
            finishFlow()
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
            Step.TYPE, Step.SUCCESS -> goTo(Step.MENU)
            Step.ENTER -> goTo(Step.TYPE)
            Step.CONFIRM -> goTo(Step.ENTER)
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
        step = next
        if (leavingTest) restoreSystemBars()
        render()
        binding.setupScroll.scrollTo(0, 0)
    }

    private fun render() {
        val isMenu = step == Step.MENU
        val isType = step == Step.TYPE
        val isCard = step in setOf(Step.ENTER, Step.CONFIRM, Step.SUCCESS)

        binding.cardPinSetup.root.isVisible = isMenu
        binding.cardRecovery.root.isVisible = isMenu
        binding.tvTypeStep.isVisible = isType
        binding.stepperType.root.isVisible = isType
        typeCards().forEach { (_, card) -> card.root.isVisible = isType }
        binding.cardStep.isVisible = isCard
        binding.btnHeaderNext.isVisible = isType
        binding.btnHeaderDone.isVisible = step == Step.SUCCESS
        binding.clTest.isVisible = step == Step.TEST

        when (step) {
            Step.TYPE -> renderType()
            Step.ENTER, Step.CONFIRM -> renderPinStep()
            Step.SUCCESS -> renderSuccess()
            Step.TEST -> renderTest()
            Step.MENU -> hideKeyboard()
        }
    }

    private fun renderType() {
        hideKeyboard()
        renderStepper(binding.stepperType, 1)
        renderTypeSelection()
    }

    private fun renderTypeSelection() {
        typeCards().forEach { (length, card) ->
            val selected = length == pinLength
            card.root.setBackgroundResource(
                if (selected) R.drawable.bg_pin_type_card_selected else R.drawable.bg_pin_type_card
            )
            card.root.isSelected = selected
        }
    }

    private fun renderPinStep() {
        val confirming = step == Step.CONFIRM
        binding.tvStepTitle.setText(if (confirming) R.string.pin_step_confirm else R.string.pin_step_input)
        binding.ivStepArt.setImageResource(R.drawable.img_pin_lock_illustration)
        binding.ivStepArt.layoutParams = binding.ivStepArt.layoutParams.apply {
            width = dp(104)
            height = dp(122)
        }
        binding.tvSuccessTitle.isVisible = false
        binding.pinInputField.isVisible = true
        binding.tvStepHint.setText(if (confirming) R.string.pin_confirm_input_hint else R.string.pin_input_hint)
        binding.tvStepHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        binding.etPin.filters = arrayOf(android.text.InputFilter.LengthFilter(pinLength))
        binding.etPin.text?.clear()
        clearInputError()
        renderPinVisibility()
        renderStepper(binding.stepperStep, if (confirming) 3 else 2)
        showKeyboard()
    }

    private fun renderSuccess() {
        hideKeyboard()
        binding.tvStepTitle.setText(R.string.pin_step_done)
        binding.ivStepArt.layoutParams = binding.ivStepArt.layoutParams.apply {
            width = dp(112)
            height = dp(112)
        }
        playSuccessTick()
        binding.tvSuccessTitle.isVisible = true
        binding.pinInputField.isVisible = false
        binding.tvStepHint.setText(R.string.pin_setup_success_desc)
        binding.tvStepHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary))
        renderStepper(binding.stepperStep, 4)
    }

    /** Vector tick animation: crisp at any density, seal pops in and the check draws itself. */
    private fun playSuccessTick() {
        val tick = AnimatedVectorDrawableCompat.create(requireContext(), R.drawable.avd_pin_success)
        if (tick == null) {
            binding.ivStepArt.setImageResource(R.drawable.ic_pin_success_seal)
            return
        }
        binding.ivStepArt.setImageDrawable(tick)
        tick.start()
    }

    private fun renderTest() {
        hideKeyboard()
        applyTestWallpaper()
        enterImmersiveBars()
        testInput.clear()
        binding.tvTestPrompt.setText(R.string.pin_please_enter)
        buildTestDots()
        renderTestDots()
    }

    private fun renderStepper(stepper: ViewPinStepperBinding, current: Int) {
        val dots = listOf(stepper.dot1, stepper.dot2, stepper.dot3, stepper.dot4)
        val lines = listOf(stepper.line1, stepper.line2, stepper.line3)
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(if (index < current) R.drawable.bg_pin_step_dot_on else R.drawable.bg_pin_step_dot_off)
        }
        val on = ContextCompat.getColor(requireContext(), R.color.primary)
        val off = ContextCompat.getColor(requireContext(), R.color.primaryGlow)
        lines.forEachIndexed { index, line -> line.setBackgroundColor(if (index < current - 1) on else off) }
        stepper.root.contentDescription = getString(R.string.pin_step_progress, current)
    }

    private fun renderPinVisibility() {
        binding.etPin.transformationMethod =
            if (pinVisible) HideReturnsTransformationMethod.getInstance() else PasswordTransformationMethod.getInstance()
        binding.etPin.setSelection(binding.etPin.text?.length ?: 0)
        binding.btnTogglePin.setImageResource(if (pinVisible) R.drawable.ic_eye_off_outline else R.drawable.ic_eye_outline)
        binding.btnTogglePin.contentDescription = getString(if (pinVisible) R.string.pin_hide else R.string.pin_show)
    }

    private fun showInputError() {
        binding.pinInputField.setBackgroundResource(R.drawable.bg_pin_input_field_error)
        binding.tvStepHint.setText(R.string.pin_mismatch)
        binding.tvStepHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
        shake(binding.pinInputField)
        binding.etPin.text?.clear()
    }

    private fun clearInputError() {
        if (step != Step.CONFIRM && step != Step.ENTER) return
        binding.pinInputField.setBackgroundResource(R.drawable.bg_pin_input_field)
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

    private fun showKeyboard() {
        binding.etPin.requestFocus()
        binding.etPin.post {
            val imm = context?.getSystemService(InputMethodManager::class.java) ?: return@post
            imm.showSoftInput(bindingOrNull?.etPin ?: return@post, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(InputMethodManager::class.java) ?: return
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        binding.etPin.clearFocus()
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
        const val KEY_FIRST_PIN = "pin_first"
        const val DEFAULT_PIN_LENGTH = 4
        val PIN_LENGTHS = setOf(4, 5, 6, 8)
        const val SHAKE_MS = 400L
    }
}
