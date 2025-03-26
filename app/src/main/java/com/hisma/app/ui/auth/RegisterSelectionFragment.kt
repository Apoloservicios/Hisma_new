package com.hisma.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hisma.app.databinding.FragmentRegisterSelectionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterSelectionFragment : Fragment() {

    private var _binding: FragmentRegisterSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

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

        // Configurar clic para registro de lubricentro
        binding.cardLubricenter.setOnClickListener {
            viewModel.setRegistrationType(AuthViewModel.RegistrationType.LUBRICENTER)
            viewModel.navigateToRegisterLubricenter()
        }

        // Configurar clic para registro de empleado
        binding.cardEmployee.setOnClickListener {
            viewModel.setRegistrationType(AuthViewModel.RegistrationType.EMPLOYEE)
            viewModel.navigateToRegisterEmployee()
        }

        // Configurar texto para ir a login
        binding.textLogin.setOnClickListener {
            viewModel.navigateToLogin()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}