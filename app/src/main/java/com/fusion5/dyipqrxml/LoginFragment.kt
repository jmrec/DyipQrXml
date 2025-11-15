package com.fusion5.dyipqrxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fusion5.dyipqrxml.databinding.FragmentLoginBinding
import com.fusion5.dyipqrxml.ui.auth.AuthViewModel
import com.fusion5.dyipqrxml.ui.auth.AuthViewModelFactory
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        binding.textRegister.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                state.errorMessage?.let {
                    binding.textInputEmail.error = it
                    binding.textInputPassword.error = it
                }
                if (state.success) {
                    findNavController().navigate(R.id.userProfileFragment)
                    viewModel.resetState()
                }
            }
        }
    }

    private fun performLogin() {
        val email = binding.editEmail.text.toString()
        val password = binding.editPassword.text.toString()
        viewModel.login(email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}