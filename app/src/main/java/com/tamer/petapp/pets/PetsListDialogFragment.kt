package com.tamer.petapp.pets

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.tamer.petapp.R
import com.tamer.petapp.adapter.PetAdapter
import com.tamer.petapp.databinding.FragmentPetsListDialogBinding
import com.tamer.petapp.model.Pet

/**
 * Evcil hayvanları listeleyen dialog fragment
 */
class PetsListDialogFragment : DialogFragment() {
    
    private var _binding: FragmentPetsListDialogBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var petsAdapter: PetAdapter
    
    private val TAG = "PetsListDialog"
    
    var onPetSelected: ((Pet) -> Unit)? = null
    var onEditPet: ((Pet) -> Unit)? = null
    var onDeletePet: ((Pet) -> Unit)? = null
    var onAddPetClick: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        
        // Firebase başlatma
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetsListDialogBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        
        // Evcil hayvanları yükle
        loadPets()
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.your_pets)
            setNavigationOnClickListener {
                dismiss()
            }
        }
    }
    
    private fun setupRecyclerView() {
        petsAdapter = PetAdapter(
            onItemClick = { pet ->
                onPetSelected?.invoke(pet)
                dismiss()
            },
            onEditClick = { pet ->
                onEditPet?.invoke(pet)
                dismiss()
            },
            onDeleteClick = { pet ->
                onDeletePet?.invoke(pet)
                dismiss()
            }
        )
        
        binding.recyclerViewPets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = petsAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupAddButton() {
        binding.btnAddPet.setOnClickListener {
            onAddPetClick?.invoke()
            dismiss()
        }
    }
    
    private fun loadPets() {
        val currentUser = auth.currentUser ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewPets.visibility = View.GONE
        binding.tvNoPets.visibility = View.GONE
        
        // Firestore ayarlarını optimize et
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
        
        // Önce önbellekten yükle, sonra sunucudan senkronize et
        firestore.collection("users")
            .document(currentUser.uid)
            .collection("pets")
            .get(Source.CACHE)
            .addOnSuccessListener { documents ->
                // Önbellekten gelen veriler varsa göster
                if (!documents.isEmpty) {
                    processPetsData(documents.toObjects(Pet::class.java).apply {
                        forEachIndexed { index, pet -> 
                            pet.id = documents.documents[index].id
                        }
                    })
                }
                
                // Sunucudan güncel verileri al
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("pets")
                    .get(Source.SERVER)
                    .addOnSuccessListener { serverDocuments ->
                        processPetsData(serverDocuments.toObjects(Pet::class.java).apply {
                            forEachIndexed { index, pet -> 
                                pet.id = serverDocuments.documents[index].id
                            }
                        })
                    }
                    .addOnFailureListener { e ->
                        handleLoadError(e)
                    }
            }
            .addOnFailureListener { e ->
                // Önbellek boşsa sunucudan yükle
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("pets")
                    .get(Source.SERVER)
                    .addOnSuccessListener { documents ->
                        processPetsData(documents.toObjects(Pet::class.java).apply {
                            forEachIndexed { index, pet -> 
                                pet.id = documents.documents[index].id
                            }
                        })
                    }
                    .addOnFailureListener { e2 ->
                        handleLoadError(e2)
                    }
            }
    }
    
    private fun processPetsData(petsList: List<Pet>) {
        binding.progressBar.visibility = View.GONE
        
        if (petsList.isEmpty()) {
            binding.tvNoPets.visibility = View.VISIBLE
            binding.recyclerViewPets.visibility = View.GONE
        } else {
            binding.tvNoPets.visibility = View.GONE
            binding.recyclerViewPets.visibility = View.VISIBLE
            petsAdapter.submitList(petsList)
        }
        
        Log.d(TAG, "${petsList.size} evcil hayvan yüklendi")
    }
    
    private fun handleLoadError(e: Exception) {
        binding.progressBar.visibility = View.GONE
        binding.tvNoPets.visibility = View.VISIBLE
        binding.recyclerViewPets.visibility = View.GONE
        binding.tvNoPets.text = getString(R.string.error_loading_pets)
        
        Log.e(TAG, "Evcil hayvanlar yüklenirken hata: ${e.message}", e)
        Toast.makeText(context, "Yükleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        const val TAG = "PetsListDialogFragment"
        
        fun newInstance(): PetsListDialogFragment {
            return PetsListDialogFragment()
        }
    }
} 