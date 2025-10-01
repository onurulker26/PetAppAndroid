package com.tamer.petapp.treatment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.tamer.petapp.databinding.DialogAddMedicationBinding
import com.tamer.petapp.model.Medication
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddMedicationDialog : DialogFragment() {
    private var _binding: DialogAddMedicationBinding? = null
    private val binding get() = _binding!!
    
    private var selectedStartDate = Date()
    private var selectedReminderTime = "08:00"
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    var onMedicationAdded: ((Medication) -> Unit)? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddMedicationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Başlangıç tarihi ve hatırlatma saatini ayarla
        binding.etStartDate.setText(dateFormat.format(selectedStartDate))
        binding.etReminderTime.setText(selectedReminderTime)
        
        // Tarih seçici
        binding.etStartDate.setOnClickListener {
            showDatePicker()
        }
        
        // Saat seçici
        binding.etReminderTime.setOnClickListener {
            showTimePicker()
        }
        
        // İptal butonu
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
        
        // Ekle butonu
        binding.btnAdd.setOnClickListener {
            if (validateInputs()) {
                val medication = Medication(
                    id = "med_${System.currentTimeMillis()}",
                    name = binding.etMedicationName.text.toString().trim(),
                    dosage = binding.etDosage.text.toString().trim(),
                    frequency = binding.etFrequency.text.toString().trim(),
                    startDate = selectedStartDate,
                    reminderTime = selectedReminderTime
                )
                
                onMedicationAdded?.invoke(medication)
                dismiss()
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { time = selectedStartDate }
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedStartDate = calendar.time
                binding.etStartDate.setText(dateFormat.format(selectedStartDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val currentTime = timeFormat.parse(selectedReminderTime) ?: return
        calendar.time = currentTime
        
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                selectedReminderTime = timeFormat.format(calendar.time)
                binding.etReminderTime.setText(selectedReminderTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    
    private fun validateInputs(): Boolean {
        var isValid = true
        
        // İlaç adı kontrolü
        if (binding.etMedicationName.text.toString().trim().isEmpty()) {
            binding.etMedicationName.error = "İlaç adı gerekli"
            isValid = false
        }
        
        // Doz kontrolü
        if (binding.etDosage.text.toString().trim().isEmpty()) {
            binding.etDosage.error = "Doz bilgisi gerekli"
            isValid = false
        }
        
        // Kullanım sıklığı kontrolü
        if (binding.etFrequency.text.toString().trim().isEmpty()) {
            binding.etFrequency.error = "Kullanım sıklığı gerekli"
            isValid = false
        }
        
        return isValid
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): AddMedicationDialog {
            return AddMedicationDialog()
        }
    }
} 