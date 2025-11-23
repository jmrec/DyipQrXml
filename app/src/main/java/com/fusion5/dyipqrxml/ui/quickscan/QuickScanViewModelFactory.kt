package com.fusion5.dyipqrxml.ui.quickscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fusion5.dyipqrxml.data.repository.ScanHistoryRepository
import com.fusion5.dyipqrxml.data.repository.SessionRepository
import com.fusion5.dyipqrxml.data.repository.TerminalRepository

class QuickScanViewModelFactory(
    private val terminalRepository: TerminalRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuickScanViewModel(
                terminalRepository,
                scanHistoryRepository,
                sessionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}