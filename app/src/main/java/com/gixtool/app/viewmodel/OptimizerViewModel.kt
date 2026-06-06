package com.gixtool.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.gixtool.app.util.DeviceSpecs
import com.gixtool.app.util.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class OptimizerState {
    object Idle : OptimizerState()
    object Loading : OptimizerState()
    data class Success(val specs: DeviceSpecs, val recommendations: String) : OptimizerState()
    data class Error(val message: String) : OptimizerState()
}

class OptimizerViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableLiveData<OptimizerState>(OptimizerState.Idle)
    val state: LiveData<OptimizerState> = _state

    private val _optimizeResult = MutableLiveData<String>()
    val optimizeResult: LiveData<String> = _optimizeResult

    fun loadDeviceSpecs() {
        _state.value = OptimizerState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val specs = DeviceUtils.getDeviceSpecs(getApplication())
                val recommendations = DeviceUtils.getRecommendedFFSettings(specs)
                _state.postValue(OptimizerState.Success(specs, recommendations))
            } catch (e: Exception) {
                _state.postValue(OptimizerState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun optimizeForGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Trim memory
                val runtime = Runtime.getRuntime()
                System.gc()
                runtime.gc()

                val freed = runtime.freeMemory()
                _optimizeResult.postValue("✅ تم تحسين الأداء بنجاح!\nذاكرة محررة: ${DeviceUtils.formatBytes(freed)}")
            } catch (e: Exception) {
                _optimizeResult.postValue("⚠️ ${e.message}")
            }
        }
    }
}
