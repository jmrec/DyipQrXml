package com.fusion5.dyipqrxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fusion5.dyipqrxml.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardQuickScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_quickScan)
        }

        binding.cardAccount.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_userProfile)
        }

        binding.cardTerminals.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_terminals)
        }

        binding.cardSaved.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_saved)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}