package com.fusion5.dyipqrxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fusion5.dyipqrxml.databinding.FragmentSavedBinding
import com.fusion5.dyipqrxml.ui.saved.SavedAdapter
import com.fusion5.dyipqrxml.ui.saved.SavedViewModel
import com.fusion5.dyipqrxml.ui.saved.SavedViewModelFactory
import kotlinx.coroutines.launch

class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedViewModel by viewModels {
        SavedViewModelFactory(requireContext())
    }

    private lateinit var adapter: SavedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = SavedAdapter(
            onTerminalClick = { terminal ->
                // TODO: Navigate to terminal details
            },
            onRemoveFavorite = { terminal ->
                lifecycleScope.launch {
                    viewModel.removeFavorite(terminal.id)
                }
            }
        )
        binding.recyclerSaved.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSaved.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.favorites)
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.textError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
                binding.textError.text = state.errorMessage
                
                // Show login prompt if user is not logged in
                binding.layoutLoginPrompt.visibility =
                    if (!state.isUserLoggedIn && !state.isLoading) View.VISIBLE else View.GONE
                
                // Show empty state if no favorites
                binding.layoutEmptyState.visibility =
                    if (state.favorites.isEmpty() && state.isUserLoggedIn && !state.isLoading) View.VISIBLE else View.GONE
                
                // Show favorites list if user is logged in and has favorites
                binding.recyclerSaved.visibility =
                    if (state.favorites.isNotEmpty() && state.isUserLoggedIn) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}