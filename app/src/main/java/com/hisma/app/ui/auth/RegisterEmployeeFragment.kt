package com.hisma.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hisma.app.databinding.FragmentRegisterEmployeeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterEmployeeFragment : Fragment() {

    private var _binding: FragmentRegisterEmployeeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón de registro
        binding.buttonRegister.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val lastName = binding.editTextLastName.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val lubricenterCuit = binding.editTextLubricenterCuit.text.toString()

            viewModel.registerEmployee(name, lastName, email, password, lubricenterCuit)
        }

        // Configurar texto para ir a login
        binding.textLogin.setOnClickListener {
            viewModel.navigateToLogin()
        }

        // Observar estado de registro
        viewModel.employeeRegisterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.EmployeeRegisterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonRegister.isEnabled = false
                }
                is AuthViewModel.EmployeeRegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Registro exitoso. ¡Bienvenido a HISMA!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthViewModel.EmployeeRegisterState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                null -> { /* No hacer nada */ }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}