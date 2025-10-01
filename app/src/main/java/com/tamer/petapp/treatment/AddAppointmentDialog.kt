package com.tamer.petapp.treatment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tamer.petapp.databinding.DialogAddAppointmentBinding
import com.tamer.petapp.model.AppointmentStatus
import com.tamer.petapp.model.VetAppointment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddAppointmentDialog : DialogFragment() {
    private var _binding: DialogAddAppointmentBinding? = null
    private val binding get() = _binding!!
    
    private var selectedDate = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    var onAppointmentAdded: ((VetAppointment) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Tarih ve saat alanlarını güncelle
        updateDateTimeFields()
        
        // Tarih seçici
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        
        // Saat seçici
        binding.etTime.setOnClickListener {
            showTimePicker()
        }
        
        // İptal butonu
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // Ekle butonu
        binding.btnAdd.setOnClickListener {
            if (validateInputs()) {
                val appointment = VetAppointment(
                    id = "app_${System.currentTimeMillis()}",
                    clinicName = binding.etClinicName.text.toString().trim(),
                    date = selectedDate.time,
                    status = AppointmentStatus.SCHEDULED,
                    notes = binding.etNotes.text.toString().trim()
                )
                
                onAppointmentAdded?.invoke(appointment)
                dismiss()
            }
        }
    }
    
    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, day)
                updateDateTimeFields()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                selectedDate.set(Calendar.MINUTE, minute)
                updateDateTimeFields()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun updateDateTimeFields() {
        binding.etDate.setText(dateFormat.format(selectedDate.time))
        binding.etTime.setText(timeFormat.format(selectedDate.time))
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // Klinik adı kontrolü
        if (binding.etClinicName.text.toString().trim().isEmpty()) {
            binding.etClinicName.error = "Klinik adı gerekli"
            isValid = false
        }
        
        return isValid
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): AddAppointmentDialog {
            return AddAppointmentDialog()
        }
    }
} 