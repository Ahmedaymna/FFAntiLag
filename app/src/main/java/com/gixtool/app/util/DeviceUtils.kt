package com.gixtool.app.util

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.RandomAccessFile

data class DeviceSpecs(
    val totalRam: Long,
    val availableRam: Long,
    val cpuModel: String,
    val cpuCores: Int,
    val cpuFreqMhz: Int,
    val gpuRenderer: String,
    val gpuVendor: String,
    val totalStorage: Long,
    val freeStorage: Long,
    val androidVersion: String,
    val deviceModel: String,
    val manufacturer: String
)

object DeviceUtils {

    fun getDeviceSpecs(context: Context): DeviceSpecs {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        return DeviceSpecs(
            totalRam = memInfo.totalMem,
            availableRam = memInfo.availMem,
            cpuModel = getCpuModel(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuFreqMhz = getCpuMaxFreq(),
            gpuRenderer = getGpuRenderer(),
            gpuVendor = getGpuVendor(),
            totalStorage = getTotalStorage(),
            freeStorage = getFreeStorage(),
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            manufacturer = Build.MANUFACTURER
        )
    }

    private fun getCpuModel(): String {
        return try {
            val reader = BufferedReader(FileReader("/proc/cpuinfo"))
            var line: String?
            var hardware = "Unknown"
            var processor = "Unknown"
            while (reader.readLine().also { line = it } != null) {
                when {
                    line!!.startsWith("Hardware") -> hardware = line!!.split(":")[1].trim()
                    line!!.startsWith("model name") -> processor = line!!.split(":")[1].trim()
                    line!!.startsWith("Processor") -> if (processor == "Unknown") processor = line!!.split(":")[1].trim()
                }
            }
            reader.close()
            if (processor != "Unknown") processor else hardware
        } catch (e: Exception) {
            "${Build.HARDWARE} / ${Build.BOARD}"
        }
    }

    private fun getCpuMaxFreq(): Int {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (file.exists()) {
                val freq = file.readText().trim().toLong()
                (freq / 1000).toInt()
            } else 0
        } catch (e: Exception) { 0 }
    }

    private fun getGpuRenderer(): String {
        return try {
            // This must be called from GL thread in production; here we return a fallback
            "Adreno / Mali / PowerVR (see GLSurfaceView)"
        } catch (e: Exception) { "Unknown GPU" }
    }

    private fun getGpuVendor(): String = Build.HARDWARE

    private fun getTotalStorage(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) { 0L }
    }

    private fun getFreeStorage(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) { 0L }
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format("%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format("%.1f MB", bytes / 1_048_576.0)
            bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun getRecommendedFFSettings(specs: DeviceSpecs): String {
        val ramGb = specs.totalRam / 1_073_741_824.0
        return when {
            ramGb >= 6 -> buildString {
                appendLine("✅ جهازك قوي جداً! الإعدادات المثلى:")
                appendLine("• الجودة: Max (HD)")
                appendLine("• معدل الإطارات: Extreme (90 FPS)")
                appendLine("• الظلال: عالي")
                appendLine("• التأثيرات: عالي")
                appendLine("• إزالة التشويش: مفعّل")
            }
            ramGb >= 4 -> buildString {
                appendLine("⚡ جهازك جيد! الإعدادات المقترحة:")
                appendLine("• الجودة: High")
                appendLine("• معدل الإطارات: High (60 FPS)")
                appendLine("• الظلال: متوسط")
                appendLine("• التأثيرات: متوسط")
                appendLine("• إزالة التشويش: مفعّل")
            }
            ramGb >= 2 -> buildString {
                appendLine("⚠️ جهازك متوسط. الإعدادات المثلى للأداء:")
                appendLine("• الجودة: Smooth")
                appendLine("• معدل الإطارات: High (60 FPS)")
                appendLine("• الظلال: منخفض")
                appendLine("• التأثيرات: منخفض")
                appendLine("• إزالة التشويش: معطل")
            }
            else -> buildString {
                appendLine("🔴 جهازك يحتاج للتحسين. الإعدادات الموصى بها:")
                appendLine("• الجودة: Fastest")
                appendLine("• معدل الإطارات: Medium (30 FPS)")
                appendLine("• الظلال: معطل")
                appendLine("• التأثيرات: معطل")
                appendLine("• طور حفظ البطارية: مفعّل")
            }
        }
    }

    fun deleteTempFiles(context: Context): Long {
        var deletedBytes = 0L
        val dirs = listOf(
            context.cacheDir,
            context.externalCacheDir,
            File(context.filesDir, "tmp"),
            File(Environment.getExternalStorageDirectory(), "Android/data/${context.packageName}/cache")
        )
        dirs.filterNotNull().forEach { dir ->
            deletedBytes += deleteRecursive(dir)
        }
        return deletedBytes
    }

    private fun deleteRecursive(file: File): Long {
        var size = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { size += deleteRecursive(it) }
        } else {
            size = file.length()
            file.delete()
        }
        return size
    }
}
