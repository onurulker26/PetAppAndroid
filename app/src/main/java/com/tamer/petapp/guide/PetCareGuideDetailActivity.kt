package com.tamer.petapp.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ActivityPetCareGuideDetailBinding
import com.tamer.petapp.model.PetCareGuide

class PetCareGuideDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetCareGuideDetailBinding
    private lateinit var petType: String
    private val TAG = "PetCareGuideDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetCareGuideDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        petType = intent.getStringExtra(EXTRA_PET_TYPE) ?: return finish()

        // Toolbar ayarları
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.pet_care_guide_for, petType)

        // Pet türüne göre bakım bilgilerini göster
        showCareGuideForPetType(petType)
    }

    private fun showCareGuideForPetType(petType: String) {
        Log.d(TAG, "Bakım rehberi gösteriliyor: $petType")
        
        val guide = when (petType.lowercase()) {
            getString(R.string.dog).lowercase() -> PetCareGuide.getDogGuide()
            getString(R.string.cat).lowercase() -> PetCareGuide.getCatGuide()
            getString(R.string.bird).lowercase() -> PetCareGuide.getBirdGuide()
            getString(R.string.fish).lowercase() -> PetCareGuide.getFishGuide()
            getString(R.string.rabbit).lowercase() -> PetCareGuide.getRabbitGuide()
            getString(R.string.hamster).lowercase() -> PetCareGuide.getHamsterGuide()
            else -> PetCareGuide.getGenericGuide()
        }

        // TextView yerine RecyclerView kullanarak rehberi göster
        binding.tvCareGuide.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PetCareGuideDetailActivity)
            adapter = PetCareGuideContentAdapter(guide)
        }
        
        // Pet tipine göre üst bilgi ekle
        val petInfo = when (petType.lowercase()) {
            getString(R.string.dog).lowercase() -> getPetTypeInfo("Köpek", R.drawable.ic_dog)
            getString(R.string.cat).lowercase() -> getPetTypeInfo("Kedi", R.drawable.ic_cat)
            getString(R.string.bird).lowercase() -> getPetTypeInfo("Kuş", R.drawable.ic_bird)
            getString(R.string.fish).lowercase() -> getPetTypeInfo("Balık", R.drawable.ic_fish)
            getString(R.string.rabbit).lowercase() -> getPetTypeInfo("Tavşan", R.drawable.ic_rabbit)
            getString(R.string.hamster).lowercase() -> getPetTypeInfo("Hamster", R.drawable.ic_hamster)
            else -> null
        }

        if (petInfo != null) {
            binding.petTypeImageView.visibility = View.VISIBLE
            binding.petTypeInfoTextView.visibility = View.VISIBLE
            
            binding.petTypeInfoTextView.text = petInfo.first
            try {
                binding.petTypeImageView.setImageResource(petInfo.second)
            } catch (e: Exception) {
                Log.e(TAG, "Resim yüklenirken hata: ${e.message}")
                binding.petTypeImageView.visibility = View.GONE
            }
        } else {
            binding.petTypeImageView.visibility = View.GONE
            binding.petTypeInfoTextView.visibility = View.GONE
        }
    }
    
    private fun getPetTypeInfo(petTypeName: String, imageResId: Int): Pair<String, Int> {
        val infoText = when (petTypeName) {
            "Köpek" -> "Köpekler, sadık ve sevgi dolu yoldaşlardır. Düzenli egzersiz, dengeli beslenme ve sağlık kontrollerine ihtiyaç duyarlar. Her ırkın kendine özgü özellikleri ve bakım gereksinimleri vardır."
            "Kedi" -> "Kediler, bağımsız ve zeki evcil hayvanlardır. Temiz bir yaşam alanı, dengeli beslenme ve düzenli veteriner kontrolleri sağlıklı bir kedi için esastır. Hem tüy bakımı hem de günlük oyun aktiviteleri önemlidir."
            "Kuş" -> "Kuşlar, neşeli ve zeki evcil hayvanlardır. Kafes temizliği, dengeli beslenme ve sosyal etkileşim sağlıklı bir kuş için çok önemlidir. Farklı kuş türleri farklı bakım gerektirir."
            "Balık" -> "Balıklar, rahatlatıcı ve bakması kolay evcil hayvanlardır. Su kalitesi, filtrasyon ve düzenli beslenme akvaryum balıkları için esastır. Türe bağlı olarak su sıcaklığı ve pH değerleri farklılık gösterebilir."
            "Tavşan" -> "Tavşanlar, sessiz ve sevecen evcil hayvanlardır. Bol miktarda kuru ot, taze sebzeler ve güvenli bir yaşam alanı ihtiyaç duyarlar. Diş sağlığı ve düzenli tüy bakımı önemlidir."
            "Hamster" -> "Hamsterlar, sevimli ve gece aktif olan kemirgenlerdir. Temiz bir kafes, dengeli beslenme ve aktivite oyuncakları sağlıklı bir hamster için gereklidir. Gece aktif olduklarını unutmayın."
            else -> "Bu hayvan türü için genel bakım bilgileri."
        }
        return Pair(infoText, imageResId)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val EXTRA_PET_TYPE = "extra_pet_type"

        fun newIntent(context: Context, petType: String): Intent {
            return Intent(context, PetCareGuideDetailActivity::class.java).apply {
                putExtra(EXTRA_PET_TYPE, petType)


            }
        }
    }
} 