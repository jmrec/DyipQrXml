package com.fusion5.dyipqrxml.ui.quickscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class QuickScanViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickScanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuickScanViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}