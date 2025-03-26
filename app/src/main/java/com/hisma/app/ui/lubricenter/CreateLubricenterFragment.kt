package com.hisma.app.ui.lubricenter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hisma.app.databinding.FragmentCreateLubricenterBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CreateLubricenterFragment : Fragment() {

    private var _binding: FragmentCreateLubricenterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LubricenterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateLubricenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCreate.setOnClickListener {
            val name = binding.editTextName.text.toString()
            val address = binding.editTextAddress.text.toString()
            val phone = binding.editTextPhone.text.toString()
            val email = binding.editTextEmail.text.toString()

            viewModel.createLubricenter(name, address, phone, email)
        }

        // Observar el estado de creación
        viewModel.createLubricenterState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LubricenterViewModel.CreateLubricenterState.Loading -> {
                    // Mostrar progreso
                    binding.buttonCreate.isEnabled = false
                }
                is LubricenterViewModel.CreateLubricenterState.Success -> {
                    // Mostrar éxito y volver
                    binding.buttonCreate.isEnabled = true
                    Toast.makeText(requireContext(), "Lubricentro creado con éxito", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is LubricenterViewModel.CreateLubricenterState.Error -> {
                    // Mostrar error
                    binding.buttonCreate.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                null -> { /* Estado inicial, no hacer nada */ }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}