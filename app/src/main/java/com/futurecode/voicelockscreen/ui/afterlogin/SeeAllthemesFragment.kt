package com.futurecode.voicelockscreen.ui.afterlogin
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSeeAllthemesBinding
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.adapter.ThemeAdapter
import com.futurecode.voicelockscreen.model.ThemeModel
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore

class SeeAllthemesFragment : BaseFragment<FragmentSeeAllthemesBinding>(FragmentSeeAllthemesBinding::inflate) {

    private lateinit var themeAdapter: ThemeAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back button action
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        binding.btnUpload.setOnClickListener { findNavController().navigate(R.id.customThemeFragment) }
    }

    private fun setupRecyclerView() {
        // 1. Get List of Drawable Images (Replace with your actual drawable filenames)
        val themesList = getLocalThemesList()

        themeAdapter = ThemeAdapter(
            themesList,
            ThemeWallpaperStore.getSelectedThemeId(requireContext())
        ) { selectedTheme ->
            ThemeWallpaperStore.saveSelectedTheme(
                requireContext(),
                selectedTheme.id,
                selectedTheme.imageResId
            )
            themeAdapter.setSelectedTheme(selectedTheme.id)
            Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
        }

        // 3. Configure RecyclerView with 2-column Grid
        binding.rvThemes.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = themeAdapter
            setHasFixedSize(true)
        }
    }

    private fun getLocalThemesList(): List<ThemeModel> {
        // Add your local drawable image names here
        return listOf(
            ThemeModel(ThemeWallpaperStore.THEME_ONE_ID, R.drawable.theme_one),
            ThemeModel(ThemeWallpaperStore.THEME_TWO_ID, R.drawable.theme_two),
            ThemeModel(ThemeWallpaperStore.THEME_THREE_ID, R.drawable.theme_three),
            ThemeModel(ThemeWallpaperStore.THEME_FOUR_ID, R.drawable.theme_four),
            ThemeModel(ThemeWallpaperStore.THEME_FIVE_ID, R.drawable.theme_five),
            ThemeModel(ThemeWallpaperStore.THEME_SIX_ID, R.drawable.theme_six),
            ThemeModel(ThemeWallpaperStore.THEME_PREVIEW_ID, R.drawable.lock_preview_wallpaper),
        )
    }
}
