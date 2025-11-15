package com.fusion5.dyipqrxml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fusion5.dyipqrxml.databinding.FragmentSignupBinding
import com.fusion5.dyipqrxml.ui.auth.AuthViewModel
import com.fusion5.dyipqrxml.ui.auth.AuthViewModelFactory
import kotlinx.coroutines.launch

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { AuthViewModelFactory(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.buttonSignUp.setOnClickListener { performSignup() }
        binding.textLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                state.errorMessage?.let {
                    binding.textInputEmail.error = it
                    binding.textInputPassword.error = it
                    binding.textInputConfirmPassword.error = it
                }
                if (state.success) {
                    findNavController().navigate(R.id.userProfileFragment)
                    viewModel.resetState()
                }
            }
        }
    }

    private fun performSignup() {
        val name = binding.editFullName.text.toString()
        val email = binding.editEmail.text.toString()
        val password = binding.editPassword.text.toString()
        val confirmPassword = binding.editConfirmPassword.text.toString()
        if (password != confirmPassword) {
            binding.textInputConfirmPassword.error = "Passwords do not match"
            return
        }
        viewModel.signup(name, email, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
