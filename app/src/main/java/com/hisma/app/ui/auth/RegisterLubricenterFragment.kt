package com.hisma.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hisma.app.databinding.FragmentRegisterLubricenterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterLubricenterFragment : Fragment() {

    private var _binding: FragmentRegisterLubricenterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterLubricenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar botón de registro
        binding.buttonRegister.setOnClickListener {
            val ownerName = binding.editTextOwnerName.text.toString()
            val ownerLastName = binding.editTextOwnerLastName.text.toString()
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            val lubricenterName = binding.editTextLubricenterName.text.toString()
            val cuit = binding.editTextCuit.text.toString()
            val address = binding.editTextAddress.text.toString()
            val phone = binding.editTextPhone.text.toString()

            viewModel.registerLubricenter(
                ownerName,
                ownerLastName,
                email,
                password,
                lubricenterName,
                cuit,
                address,
                phone
            )
        }

        // Configurar texto para ir a login
        binding.textLogin.setOnClickListener {
            viewModel.navigateToLogin()
        }

        // Observar estado de registro
        viewModel.lubricenterRegisterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthViewModel.LubricenterRegisterState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonRegister.isEnabled = false
                }
                is AuthViewModel.LubricenterRegisterState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Registro exitoso. ¡Bienvenido a HISMA!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is AuthViewModel.LubricenterRegisterState.Error -> {
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