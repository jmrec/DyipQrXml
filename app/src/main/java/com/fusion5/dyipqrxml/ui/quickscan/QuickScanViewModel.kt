package com.fusion5.dyipqrxml.ui.quickscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fusion5.dyipqrxml.data.repository.ScanHistoryRepository
import com.fusion5.dyipqrxml.data.repository.SessionRepository
import com.fusion5.dyipqrxml.data.repository.TerminalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuickScanUiState(
    val isScanning: Boolean = false,
    val scanResult: String? = null,
    val manualCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val validatedTerminalName: String? = null,
    val scannedTerminalId: Long? = null
)

class QuickScanViewModel(
    private val terminalRepository: TerminalRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickScanUiState())
    val uiState: StateFlow<QuickScanUiState> = _uiState.asStateFlow()

    private var lastScanContent: String? = null
    private var lastScanTimestamp: Long = 0L
    private val scanDebounceWindowMillis: Long = 1500L

    fun startScanning() {
        _uiState.update {
            it.copy(
                isScanning = true,
                scanResult = null,
                errorMessage = null,
                successMessage = null,
                validatedTerminalName = null
            )
        }
    }

    fun stopScanning() {
        _uiState.update { it.copy(isScanning = false) }
    }

    fun onScanResult(result: String) {
        val normalized = result.trim()
        if (normalized.isEmpty()) return

        val now = System.currentTimeMillis()

	    // debouncing
        if (normalized == lastScanContent && (now - lastScanTimestamp) < scanDebounceWindowMillis) {
            return
        }

        lastScanContent = normalized
        lastScanTimestamp = now

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            processScanResult(normalized)
        }
    }

    fun updateManualCode(code: String) {
        _uiState.update { it.copy(manualCode = code) }
    }

    fun submitManualCode() {
        val code = _uiState.value.manualCode.trim()
        if (code.length == 4 && code.all { it.isDigit() }) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                processScanResult(code)
            }
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "Please enter a valid 4-digit code",
                    successMessage = null,
                    validatedTerminalName = null
                )
            }
        }
    }

    private suspend fun processScanResult(result: String) {
        try {
            val terminalId = result.toLongOrNull()
            val terminal = if (terminalId != null) {
                terminalRepository.getById(terminalId)
            } else {
    null
         }
            
            val currentUserId = sessionRepository.sessionUserId.first()
            
            if (terminal != null) {
                scanHistoryRepository.saveScan("Terminal: ${terminal.name} (ID: $result)", currentUserId)
                
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResult = result,
                        validatedTerminalName = terminal.name,
                        successMessage = "Valid terminal scanned: ${terminal.name}",
                        errorMessage = null,
                        isLoading = false,
                        scannedTerminalId = terminal.id
                    )
                }
            } else {
                scanHistoryRepository.saveScan("Invalid QR: $result", currentUserId)
                
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResult = result,
                        validatedTerminalName = null,
                        successMessage = null,
                        errorMessage = "Invalid QR code",
                        isLoading = false,
                        scannedTerminalId = null
                    )
                }
            }
        } catch (e: Exception) {
            val currentUserId = sessionRepository.sessionUserId.first()
            scanHistoryRepository.saveScan("Error: $result", currentUserId)
            
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanResult = result,
                    errorMessage = "Error processing scan: ${e.message}",
                    successMessage = null,
                    isLoading = false,
                    scannedTerminalId = null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
                scanResult = null,
                validatedTerminalName = null
            )
        }
    }

    fun clearManualCode() {
        _uiState.update { it.copy(manualCode = "") }
    }
}