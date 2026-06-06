package com.gixtool.app.ui.main

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gixtool.app.MainActivity
import com.gixtool.app.R
import com.gixtool.app.databinding.FragmentHomeBinding
import com.gixtool.app.renderer.GLRenderer
import com.gixtool.app.util.PerformanceMonitor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var glRenderer: GLRenderer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        performanceMonitor = PerformanceMonitor(requireContext())
        glRenderer = GLRenderer(requireContext(), performanceMonitor)

        // Setup GLSurfaceView
        binding.glSurfaceView.apply {
            setEGLContextClientVersion(3)
            setRenderer(glRenderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }

        performanceMonitor.startMonitoring()

        // Observe performance data
        viewLifecycleOwner.lifecycleScope.launch {
            performanceMonitor.performanceData.collectLatest { data ->
                binding.tvFps.text = getString(R.string.fps_format, data.fps)
                binding.tvRam.text = getString(R.string.ram_format, data.availableRamMb)
                binding.tvCpu.text = getString(R.string.cpu_format, data.cpuUsage.toInt())
                binding.tvTemp.text = if (data.temperatureCelsius > 0f)
                    getString(R.string.temp_format, data.temperatureCelsius)
                else "--°C"

                // Color coding
                binding.tvFps.setTextColor(
                    when {
                        data.fps >= 55 -> 0xFF00FF88.toInt()
                        data.fps >= 30 -> 0xFFFFAA00.toInt()
                        else -> 0xFFFF4444.toInt()
                    }
                )
                binding.tvTemp.setTextColor(
                    when {
                        data.temperatureCelsius > 60f -> 0xFFFF4444.toInt()
                        data.temperatureCelsius > 45f -> 0xFFFFAA00.toInt()
                        else -> 0xFF00FF88.toInt()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.glSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.glSurfaceView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        performanceMonitor.stopMonitoring()
        performanceMonitor.destroy() // إلغاء CoroutineScope لمنع تسرب الذاكرة
        _binding = null
    }
}
