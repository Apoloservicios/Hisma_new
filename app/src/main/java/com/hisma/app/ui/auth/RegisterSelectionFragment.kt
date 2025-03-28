package com.hisma.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hisma.app.R
import com.hisma.app.databinding.FragmentRegisterSelectionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterSelectionFragment : Fragment() {

    private var _binding: FragmentRegisterSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Opción de registrar lubricentro
        binding.cardLubricenter.setOnClickListener {
            viewModel.setRegistrationType(AuthViewModel.RegistrationType.LUBRICENTER)
            findNavController().navigate(R.id.action_registerSelectionFragment_to_registerLubricenterFragment)
        }

        // Opción de registrar empleado
        binding.cardEmployee.setOnClickListener {
            viewModel.setRegistrationType(AuthViewModel.RegistrationType.EMPLOYEE)
            findNavController().navigate(R.id.action_registerSelectionFragment_to_registerEmployeeFragment)
        }

        // Texto "Iniciar sesión"
        binding.textLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerSelectionFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}