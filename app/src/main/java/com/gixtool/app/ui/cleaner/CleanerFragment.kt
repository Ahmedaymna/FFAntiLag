package com.gixtool.app.ui.cleaner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.gixtool.app.MainActivity
import com.gixtool.app.databinding.FragmentCleanerBinding
import com.gixtool.app.viewmodel.CleanerState
import com.gixtool.app.viewmodel.CleanerViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class CleanerFragment : Fragment() {

    private var _binding: FragmentCleanerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CleanerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCleanerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeStates()
    }

    private fun setupButtons() {
        // RAM Clean
        binding.btnCleanRam.setOnClickListener {
            showAdThenAction {
                viewModel.cleanRam()
            }
        }
        binding.btnRamNote.setOnClickListener {
            showNote(
                "🧠 كيف يعمل تنظيف RAM؟",
                "يستخدم التطبيق:\n\n" +
                "• ActivityManager.killBackgroundProcesses()\n" +
                "  — يوقف جميع العمليات في الخلفية\n\n" +
                "• System.gc() + Runtime.gc()\n" +
                "  — يجبر جامع القمامة على تحرير الذاكرة غير المستخدمة\n\n" +
                "⚠️ ملاحظة: القتل الكامل للتطبيقات يتطلب صلاحية KILL_BACKGROUND_PROCESSES وقد تحتاج Root للتأثير الكامل."
            )
        }

        // CPU Optimize
        binding.btnOptimizeCpu.setOnClickListener {
            showAdThenAction {
                viewModel.optimizeCpu()
            }
        }
        binding.btnCpuNote.setOnClickListener {
            showNote(
                "⚙️ كيف يعمل تحسين CPU؟",
                "يستخدم التطبيق:\n\n" +
                "• تقليل ضغط GC (Garbage Collection)\n" +
                "  — يقلل من توقفات المعالج المفاجئة\n\n" +
                "• Process.setThreadPriority()\n" +
                "  — يعيد ضبط أولوية خيوط المعالج\n\n" +
                "• تحرير الذاكرة المؤقتة\n" +
                "  — يخفف حمل المعالج\n\n" +
                "⚠️ ملاحظة: التحسين العميق (تغيير Governor) يتطلب صلاحيات Root."
            )
        }

        // Storage Clean
        binding.btnCleanStorage.setOnClickListener {
            showAdThenAction {
                viewModel.cleanStorage()
            }
        }
        binding.btnStorageNote.setOnClickListener {
            showNote(
                "💾 كيف يعمل تنظيف التخزين؟",
                "يحذف التطبيق:\n\n" +
                "• مجلد Cache الداخلي (context.cacheDir)\n" +
                "• مجلد Cache الخارجي (externalCacheDir)\n" +
                "• الملفات المؤقتة في مجلد /tmp\n" +
                "• كاش التطبيق على بطاقة SD\n\n" +
                "⚠️ ملاحظة: لحذف كاش تطبيقات أخرى تحتاج صلاحية MANAGE_EXTERNAL_STORAGE أو Root."
            )
        }

        // Clean All button
        binding.btnCleanAll.setOnClickListener {
            showAdThenAction {
                viewModel.cleanRam()
                viewModel.optimizeCpu()
                viewModel.cleanStorage()
            }
        }

        // Reward button
        binding.btnRewardBoost.setOnClickListener {
            val activity = requireActivity() as? MainActivity
            activity?.getAdManager()?.showRewardedAd(
                onRewarded = { type, amount ->
                    Snackbar.make(
                        binding.root,
                        "🎁 مكافأة مفعّلة! تسريع مزدوج لمدة 30 دقيقة",
                        Snackbar.LENGTH_LONG
                    ).show()
                },
                onDismissed = {}
            )
        }
    }

    private fun observeStates() {
        viewModel.ramState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CleanerState.Cleaning -> {
                    binding.btnCleanRam.isEnabled = false
                    binding.tvRamStatus.text = "⏳ جاري تنظيف RAM..."
                    binding.cardRamResult.visibility = View.GONE
                }
                is CleanerState.Done -> {
                    binding.btnCleanRam.isEnabled = true
                    binding.tvRamStatus.text = state.message
                    binding.cardRamResult.visibility = View.VISIBLE
                    binding.tvRamResult.text = state.message
                }
                is CleanerState.Error -> {
                    binding.btnCleanRam.isEnabled = true
                    binding.tvRamStatus.text = state.message
                }
                else -> {}
            }
        }

        viewModel.cpuState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CleanerState.Cleaning -> {
                    binding.btnOptimizeCpu.isEnabled = false
                    binding.tvCpuStatus.text = "⏳ جاري تحسين CPU..."
                    binding.cardCpuResult.visibility = View.GONE
                }
                is CleanerState.Done -> {
                    binding.btnOptimizeCpu.isEnabled = true
                    binding.tvCpuStatus.text = state.message
                    binding.cardCpuResult.visibility = View.VISIBLE
                    binding.tvCpuResult.text = state.message
                }
                is CleanerState.Error -> {
                    binding.btnOptimizeCpu.isEnabled = true
                    binding.tvCpuStatus.text = state.message
                }
                else -> {}
            }
        }

        viewModel.storageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CleanerState.Cleaning -> {
                    binding.btnCleanStorage.isEnabled = false
                    binding.tvStorageStatus.text = "⏳ جاري تنظيف التخزين..."
                    binding.cardStorageResult.visibility = View.GONE
                }
                is CleanerState.Done -> {
                    binding.btnCleanStorage.isEnabled = true
                    binding.tvStorageStatus.text = state.message
                    binding.cardStorageResult.visibility = View.VISIBLE
                    binding.tvStorageResult.text = state.message
                }
                is CleanerState.Error -> {
                    binding.btnCleanStorage.isEnabled = true
                    binding.tvStorageStatus.text = state.message
                }
                else -> {}
            }
        }
    }

    private fun showAdThenAction(action: () -> Unit) {
        val activity = requireActivity() as? MainActivity
        activity?.getAdManager()?.showInterstitialAd(onDismissed = action) ?: action()
    }

    private fun showNote(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("فهمت", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
