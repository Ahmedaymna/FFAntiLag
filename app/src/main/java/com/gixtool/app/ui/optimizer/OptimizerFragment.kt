package com.gixtool.app.ui.optimizer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gixtool.app.MainActivity
import com.gixtool.app.R
import com.gixtool.app.databinding.FragmentOptimizerBinding
import com.gixtool.app.util.DeviceUtils
import com.gixtool.app.viewmodel.OptimizerState
import com.gixtool.app.viewmodel.OptimizerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class OptimizerFragment : Fragment() {

    private var _binding: FragmentOptimizerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OptimizerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOptimizerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load specs immediately
        viewModel.loadDeviceSpecs()

        observeState()
        setupButtons()
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OptimizerState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.cardSpecs.visibility = View.GONE
                }
                is OptimizerState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.cardSpecs.visibility = View.VISIBLE

                    val specs = state.specs
                    binding.tvDeviceModel.text = specs.deviceModel
                    binding.tvRamInfo.text = getString(
                        R.string.ram_info,
                        DeviceUtils.formatBytes(specs.availableRam),
                        DeviceUtils.formatBytes(specs.totalRam)
                    )
                    binding.tvCpuInfo.text = getString(R.string.cpu_info, specs.cpuModel, specs.cpuCores, specs.cpuFreqMhz)
                    binding.tvStorageInfo.text = getString(
                        R.string.storage_info,
                        DeviceUtils.formatBytes(specs.freeStorage),
                        DeviceUtils.formatBytes(specs.totalStorage)
                    )
                    binding.tvAndroidVersion.text = specs.androidVersion
                    binding.tvRecommendations.text = state.recommendations

                    // RAM progress bar
                    val ramPercent = ((specs.totalRam - specs.availableRam).toFloat() / specs.totalRam * 100).toInt()
                    binding.progressRam.progress = ramPercent

                    val storagePercent = ((specs.totalStorage - specs.freeStorage).toFloat() / specs.totalStorage * 100).toInt()
                    binding.progressStorage.progress = storagePercent
                }
                is OptimizerState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

        viewModel.optimizeResult.observe(viewLifecycleOwner) { result ->
            binding.progressBar.visibility = View.GONE
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("✅ نتيجة التحسين")
                .setMessage(result)
                .setPositiveButton("حسناً", null)
                .show()
        }
    }

    private fun setupButtons() {
        // Open Free Fire directly
        binding.btnOpenFreeFire.setOnClickListener {
            showAdThenAction {
                launchApp("com.dts.freefireth")
            }
        }

        // Open any game
        binding.btnOpenCustomGame.setOnClickListener {
            showCustomGameDialog()
        }

        // Optimize button
        binding.btnOptimize.setOnClickListener {
            showAdThenAction {
                binding.progressBar.visibility = View.VISIBLE
                viewModel.optimizeForGame()
            }
        }

        // Refresh specs
        binding.btnRefresh.setOnClickListener {
            viewModel.loadDeviceSpecs()
        }
    }

    private fun showAdThenAction(action: () -> Unit) {
        val activity = requireActivity() as? MainActivity
        activity?.getAdManager()?.showInterstitialAd(onDismissed = action) ?: action()
    }

    private fun launchApp(packageName: String) {
        val pm = requireContext().packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            // Open Play Store
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
    }

    private fun showCustomGameDialog() {
        val games = arrayOf(
            "PUBG Mobile" to "com.tencent.ig",
            "Call of Duty Mobile" to "com.activision.callofduty.shooter",
            "Mobile Legends" to "com.mobile.legends",
            "Clash of Clans" to "com.supercell.clashofclans",
            "Clash Royale" to "com.supercell.clashroyale",
            "Roblox" to "com.roblox.client",
            "Minecraft" to "com.mojang.minecraftpe"
        )
        val names = games.map { it.first }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("اختر لعبة")
            .setItems(names) { _, which ->
                showAdThenAction { launchApp(games[which].second) }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
