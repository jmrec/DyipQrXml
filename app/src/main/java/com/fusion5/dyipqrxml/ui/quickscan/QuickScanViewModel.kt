package com.fusion5.dyipqrxml.ui.quickscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickScanUiState(
    val isScanning: Boolean = false,
    val scanResult: String? = null,
    val manualCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class QuickScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuickScanUiState())
    val uiState: StateFlow<QuickScanUiState> = _uiState.asStateFlow()

    fun startScanning() {
        _uiState.update { it.copy(isScanning = true, scanResult = null, errorMessage = null) }
    }

    fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
    }

    fun onScanResult(result: String) {
        _uiState.update { 
            it.copy(
                isScanning = false,
                scanResult = result,
                successMessage = "QR Code scanned successfully!"
            )
        }
        processScanResult(result)
    }

    fun updateManualCode(code: String) {
        _uiState.update { it.copy(manualCode = code) }
    }

    fun submitManualCode() {
        val code = _uiState.value.manualCode.trim()
        if (code.length == 4 && code.all { it.isDigit() }) {
            _uiState.update { 
                it.copy(
                    successMessage = "Code submitted successfully!",
                    errorMessage = null
                )
            }
            processScanResult(code)
        } else {
            _uiState.update { 
                it.copy(
                    errorMessage = "Please enter a valid 4-digit code",
                    successMessage = null
                )
            }
        }
    }

    private fun processScanResult(result: String) {
        viewModelScope.launch {
            // TODO: Process the scan result - could be terminal ID, route info, etc.
            // For now, we'll just store the result
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearMessages() {
        _uiState.update { 
            it.copy(
                errorMessage = null,
                successMessage = null,
                scanResult = null
            )
        }
    }

    fun clearManualCode() {
        _uiState.update { it.copy(manualCode = "") }
    }
}