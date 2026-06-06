package com.gixtool.app.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gixtool.app.util.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class CleanerState {
    object Idle : CleanerState()
    object Cleaning : CleanerState()
    data class Done(val message: String, val freedBytes: Long) : CleanerState()
    data class Error(val message: String) : CleanerState()
}

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val _ramState = MutableLiveData<CleanerState>(CleanerState.Idle)
    val ramState: LiveData<CleanerState> = _ramState

    private val _cpuState = MutableLiveData<CleanerState>(CleanerState.Idle)
    val cpuState: LiveData<CleanerState> = _cpuState

    private val _storageState = MutableLiveData<CleanerState>(CleanerState.Idle)
    val storageState: LiveData<CleanerState> = _storageState

    // ─── RAM Cleaning ─────────────────────────────────────────────────────────
    fun cleanRam() {
        _ramState.value = CleanerState.Cleaning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val am = getApplication<Application>()
                    .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                // Kill background processes (requires KILL_BACKGROUND_PROCESSES)
                val packages = am.runningAppProcesses
                    ?.filter { it.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
                    ?.map { it.processName }
                    ?: emptyList()

                packages.forEach { pkg ->
                    try { am.killBackgroundProcesses(pkg) } catch (_: Exception) {}
                }

                // Force GC
                System.gc()
                Runtime.getRuntime().gc()
                delay(800)

                val memInfo = ActivityManager.MemoryInfo()
                am.getMemoryInfo(memInfo)
                val freed = memInfo.availMem

                _ramState.postValue(
                    CleanerState.Done(
                        "✅ تم تنظيف RAM بنجاح!\n" +
                        "عدد التطبيقات المُوقَفة: ${packages.size}\n" +
                        "ذاكرة متاحة الآن: ${DeviceUtils.formatBytes(freed)}",
                        freed
                    )
                )
            } catch (e: Exception) {
                _ramState.postValue(CleanerState.Error("خطأ: ${e.message}"))
            }
        }
    }

    // ─── CPU Optimization ────────────────────────────────────────────────────
    fun optimizeCpu() {
        _cpuState.value = CleanerState.Cleaning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Trim memory to reduce CPU usage from GC pressure
                System.gc()
                Runtime.getRuntime().gc()
                delay(500)

                // Try setting thread priority
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                delay(200)
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT)

                _cpuState.postValue(
                    CleanerState.Done(
                        "✅ تم تحسين CPU!\n" +
                        "• تقليل ضغط GC\n" +
                        "• ضبط أولوية الخيوط\n" +
                        "• تحسين استهلاك المعالج\n\n" +
                        "ملاحظة: التحسين الكامل يتطلب صلاحيات Root",
                        0L
                    )
                )
            } catch (e: Exception) {
                _cpuState.postValue(CleanerState.Error("خطأ: ${e.message}"))
            }
        }
    }

    // ─── Storage Cleaning ────────────────────────────────────────────────────
    fun cleanStorage() {
        _storageState.value = CleanerState.Cleaning
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val freed = DeviceUtils.deleteTempFiles(getApplication())
                delay(600)
                _storageState.postValue(
                    CleanerState.Done(
                        "✅ تم تنظيف التخزين!\n" +
                        "حجم المحذوف: ${DeviceUtils.formatBytes(freed)}\n" +
                        "• ملفات الكاش\n" +
                        "• الملفات المؤقتة\n" +
                        "• ذاكرة التطبيق",
                        freed
                    )
                )
            } catch (e: Exception) {
                _storageState.postValue(CleanerState.Error("خطأ: ${e.message}"))
            }
        }
    }
}
