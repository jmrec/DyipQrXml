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
    val validatedTerminalName: String? = null
)

class QuickScanViewModel(
    private val terminalRepository: TerminalRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickScanUiState())
    val uiState: StateFlow<QuickScanUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            processScanResult(result)
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
            // Validate if the result is a terminal ID
            val terminalId = result.toLongOrNull()
            val terminal = if (terminalId != null) {
                terminalRepository.getById(terminalId)
            } else {
                null
            }
            
            // Get current user ID from session
            val currentUserId = sessionRepository.sessionUserId.first()
            
            if (terminal != null) {
                // Valid terminal ID found
                
                // Save to scan history
                scanHistoryRepository.saveScan("Terminal: ${terminal.name} (ID: $result)", currentUserId)
                
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResult = result,
                        validatedTerminalName = terminal.name,
                        successMessage = "Valid terminal scanned: ${terminal.name}",
                        errorMessage = null,
                        isLoading = false
                    )
                }
            } else {
                // Not a valid terminal ID, treat as generic scan
                scanHistoryRepository.saveScan(result, currentUserId)
                
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanResult = result,
                        validatedTerminalName = null,
                        successMessage = "QR code scanned successfully!",
                        errorMessage = null,
                        isLoading = false
                    )
                }
            }
        } catch (e: Exception) {
            // Handle any errors during processing
            val currentUserId = sessionRepository.sessionUserId.first()
            scanHistoryRepository.saveScan("Error: $result", currentUserId)
            
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanResult = result,
                    errorMessage = "Error processing scan: ${e.message}",
                    successMessage = null,
                    isLoading = false
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