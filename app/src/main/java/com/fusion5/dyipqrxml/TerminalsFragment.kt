package com.fusion5.dyipqrxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.fusion5.dyipqrxml.databinding.FragmentTerminalsBinding
import com.fusion5.dyipqrxml.ui.terminals.TerminalListItem
import com.fusion5.dyipqrxml.ui.terminals.TerminalsAdapter
import com.fusion5.dyipqrxml.ui.terminals.TerminalsViewModel
import com.fusion5.dyipqrxml.ui.terminals.TerminalsViewModelFactory
import kotlinx.coroutines.launch

class TerminalsFragment : Fragment() {

    private var _binding: FragmentTerminalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TerminalsViewModel by viewModels {
        TerminalsViewModelFactory(requireContext())
    }

    private lateinit var adapter: TerminalsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        adapter = TerminalsAdapter(
            onTerminalClick = { item ->
                // TODO navigate using item.terminal
            },
            onToggleFavorite = { item ->
                viewModel.toggleFavorite(item.terminal.id)
            }
        )
        binding.recyclerTerminals.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTerminals.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                adapter.submitList(state.items)
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                binding.textError.visibility = if (state.errorMessage != null) View.VISIBLE else View.GONE
                binding.textError.text = state.errorMessage
                binding.layoutEmptyState.visibility =
                    if (state.items.isEmpty() && !state.isLoading) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonRefresh.setOnClickListener {
            // Refresh terminal list
            binding.recyclerTerminals.smoothScrollToPosition(0)
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.search(newText ?: "")
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}