package com.gixtool.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PerformanceData(
    val fps: Int = 0,
    val cpuUsage: Float = 0f,
    val availableRamMb: Long = 0L,
    val temperatureCelsius: Float = 0f
)

class PerformanceMonitor(private val context: Context) {

    private val _performanceData = MutableStateFlow(PerformanceData())
    val performanceData: StateFlow<PerformanceData> = _performanceData

    private var monitorJob: Job? = null
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                val data = collectPerformanceData()
                _performanceData.value = data
                delay(1000L)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
    }

    fun onFrame() {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000L) {
            val fps = frameCount
            frameCount = 0
            lastFpsTime = now
            _performanceData.value = _performanceData.value.copy(fps = fps)
        }
    }

    private fun collectPerformanceData(): PerformanceData {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val availRamMb = memInfo.availMem / (1024 * 1024)

        val cpuUsage = readCpuUsage()
        val temp = readCpuTemperature()

        return PerformanceData(
            fps = _performanceData.value.fps,
            cpuUsage = cpuUsage,
            availableRamMb = availRamMb,
            temperatureCelsius = temp
        )
    }

    private fun readCpuUsage(): Float {
        return try {
            val reader = java.io.RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()
            val parts = load.split(" ").filter { it.isNotEmpty() }
            if (parts.size > 4) {
                val idle = parts[4].toLong()
                val total = parts.drop(1).sumOf { it.toLongOrNull() ?: 0L }
                if (total > 0) ((total - idle).toFloat() / total.toFloat()) * 100f else 0f
            } else 0f
        } catch (e: Exception) { 0f }
    }

    private fun readCpuTemperature(): Float {
        val paths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
            "/sys/class/hwmon/hwmon0/device/temp1_input"
        )
        for (path in paths) {
            try {
                val file = java.io.File(path)
                if (file.exists()) {
                    val raw = file.readText().trim().toLongOrNull() ?: continue
                    return if (raw > 1000) raw / 1000f else raw.toFloat()
                }
            } catch (e: Exception) { continue }
        }
        return 0f
    }

    fun destroy() {
        scope.cancel()
    }
}
