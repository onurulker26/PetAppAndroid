package com.tamer.petapp.treatment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tamer.petapp.R
import com.tamer.petapp.databinding.DialogAddDocumentBinding
import com.tamer.petapp.model.TreatmentDocument
import java.util.Date
import java.util.UUID

class AddDocumentDialog : DialogFragment() {
    
    private var _binding: DialogAddDocumentBinding? = null
    private val binding get() = _binding!!
    
    private var selectedDocumentUri: Uri? = null
    var onDocumentAdded: ((TreatmentDocument, Uri) -> Unit)? = null
    
    private val TAG = "AddDocumentDialog"
    
    companion object {
        private const val REQUEST_DOCUMENT_PICK = 101
        
        fun newInstance(): AddDocumentDialog {
            return AddDocumentDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_PetApp_Dialog)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDocumentTypeSpinner()
        
        // Belge seçme butonuna tıklandığında
        binding.btnSelectDocument.setOnClickListener {
            // Dosya boyutu uyarısını göster
            AlertDialog.Builder(requireContext())
                .setTitle("Dosya Boyutu Uyarısı")
                .setMessage("Yükleyeceğiniz belgenin boyutu en fazla 1MB olmalıdır. Daha büyük belgeler yüklenemeyecektir.")
                .setPositiveButton("Anladım") { dialog, _ ->
                    dialog.dismiss()
                    // Belge seçme işlemini başlat
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/pdf"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    startActivityForResult(intent, REQUEST_DOCUMENT_PICK)
                }
                .setNegativeButton("İptal", null)
                .show()
        }
        
        // Kaydetme butonu
        binding.btnSave.setOnClickListener {
            validateAndSaveDocument()
        }
        
        // İptal butonu
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupDocumentTypeSpinner() {
        val documentTypes = arrayOf(
            getString(R.string.prescription),
            getString(R.string.test_result),
            getString(R.string.other)
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            documentTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.spinnerDocumentType.adapter = adapter
    }
    
    private fun selectDocument() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf" // Sadece PDF dosyalarını kabul et
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            
            // Dosya seçme aktivitesini başlat
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.upload_document)),
                REQUEST_DOCUMENT_PICK
            )
        } catch (e: Exception) {
            Log.e(TAG, "Dosya seçme hatası: ${e.message}", e)
            Toast.makeText(requireContext(), "Dosya seçme hatası: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_DOCUMENT_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // Seçilen dosya URI'sini kaydet
                    selectedDocumentUri = uri
                    
                    // Dosya adını göster
                    val fileName = getFileNameFromUri(uri)
                    binding.tvSelectedDocument.text = fileName
                    binding.tvSelectedDocument.visibility = View.VISIBLE
                    
                    // Dosya tipi PDF mi kontrol et
                    val fileType = requireContext().contentResolver.getType(uri)
                    Log.d(TAG, "Seçilen dosya tipi: $fileType")
                    
                    if (fileType != "application/pdf") {
                        Toast.makeText(requireContext(), "Lütfen PDF formatında bir dosya seçin", Toast.LENGTH_SHORT).show()
                        selectedDocumentUri = null
                        binding.tvSelectedDocument.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Dosya işleme hatası: ${e.message}", e)
                    Toast.makeText(requireContext(), "Dosya işlenemedi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    selectedDocumentUri = null
                    binding.tvSelectedDocument.visibility = View.GONE
                }
            }
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "Bilinmeyen dosya"
        
        try {
            // ContentResolver kullanarak dosya adını al
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = cursor.getString(displayNameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dosya adı alınamadı: ${e.message}", e)
            fileName = "belge_${UUID.randomUUID().toString().substring(0, 8)}.pdf"
        }
        
        return fileName
    }
    
    private fun validateAndSaveDocument() {
        try {
            // Girişleri kontrol et
            val documentName = binding.etDocumentName.text.toString().trim()
            
            if (documentName.isEmpty()) {
                binding.etDocumentName.error = getString(R.string.field_required)
                return
            }
            
            if (selectedDocumentUri == null) {
                Toast.makeText(requireContext(), "Lütfen bir belge seçin", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Belge türünü al
            val documentTypePosition = binding.spinnerDocumentType.selectedItemPosition
            val documentType = binding.spinnerDocumentType.getItemAtPosition(documentTypePosition).toString()
            
            // TreatmentDocument nesnesi oluştur
            val document = TreatmentDocument(
                id = "temp_" + UUID.randomUUID().toString(),
                name = documentName,
                type = documentType,
                uploadDate = Date(),
                fileUrl = "",
                localUri = selectedDocumentUri.toString()
            )
            
            Log.d(TAG, "Yeni belge oluşturuldu: $document")
            
            // Callback'i çağır
            onDocumentAdded?.invoke(document, selectedDocumentUri!!)
            
            // Dialog'u kapat
            dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Belge kaydetme hatası: ${e.message}", e)
            Toast.makeText(requireContext(), "Beklenmeyen bir hata oluştu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 