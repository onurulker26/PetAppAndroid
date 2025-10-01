package com.tamer.petapp.forum

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ActivityUserProfileBinding
import com.tamer.petapp.model.ForumPost
import com.tamer.petapp.repository.ForumRepository
import com.tamer.petapp.viewmodel.ForumViewModel
import com.tamer.petapp.viewmodel.ForumViewModelFactory
import com.tamer.petapp.viewmodel.UserProfileViewModel
import com.tamer.petapp.viewmodel.UserProfileViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserProfileActivity : AppCompatActivity() {
    // Public olarak erişilebilecek binding ve viewModel
    val binding: ActivityUserProfileBinding by lazy { ActivityUserProfileBinding.inflate(layoutInflater) }
    val viewModel: UserProfileViewModel by viewModels {
        UserProfileViewModelFactory(
            ForumRepository()
        )
    }
    
    private lateinit var adapter: UserPostsAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var pagerAdapter: UserProfilePagerAdapter
    
    private val TAG = "UserProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Firebase başlat
        auth = FirebaseAuth.getInstance()
        
        // Toolbar ayarla
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profilim"
        
        setupViewPager()
        observeViewModel()
        
        // Verileri yüklemek için kullanıcı ID'sini al
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "Kullanıcı girişi yapılmış: ${currentUser.uid}")
            
            // ViewPager tamamen kurulduktan sonra verileri yükle
            lifecycleScope.launch {
                delay(300) // ViewPager'ın fragment'ları oluşturması için kısa bir gecikme
                viewModel.loadUserPosts()
                viewModel.loadLikedPosts()
            }
        } else {
            Log.e(TAG, "Kullanıcı girişi yapılmamış!")
            Toast.makeText(this, "Kullanıcı girişi yapmalısınız", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupViewPager() {
        pagerAdapter = UserProfilePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // TabLayout ve ViewPager'ı bağla
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Gönderilerim"
                1 -> tab.text = "Beğendiklerim"
            }
        }.attach()
        
        // Tab değiştiğinde gerekirse verileri yeniden yükle
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                Log.d(TAG, "Tab seçildi: ${tab?.position}")
                when (tab?.position) {
                    0 -> viewModel.loadUserPosts()
                    1 -> viewModel.loadLikedPosts()
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Tab tekrar seçildiğinde de verileri yenile
                when (tab?.position) {
                    0 -> viewModel.loadUserPosts()
                    1 -> viewModel.loadLikedPosts()
                }
            }
        })
    }
    
    fun getLikedPostsFragment(): LikedPostsFragment? {
        // ViewPager2'nin mevcut fragmentlarını bul
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is LikedPostsFragment) {
                return fragment
            }
            
            // Fragment içerisindeki childFragmentManager'da da ara
            for (childFragment in fragment.childFragmentManager.fragments) {
                if (childFragment is LikedPostsFragment) {
                    return childFragment
                }
            }
        }
        return null
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userPostsState.collect { state ->
                    when (state) {
                        is UserProfileViewModel.UiState.Loading -> {
                            // Fragment içinde yükleme gösterilecek
                        }
                        is UserProfileViewModel.UiState.Success<*> -> {
                            val posts = (state.data as? List<*>)?.filterIsInstance<ForumPost>() ?: emptyList()
                            Log.d(TAG, "Kullanıcı gönderileri alındı: ${posts.size} adet")
                            
                            // Tüm fragment'ları kontrol ederek UserPostsFragment'ı bul
                            var foundFragment = false
                            for (fragment in supportFragmentManager.fragments) {
                                if (fragment is UserPostsFragment) {
                                    fragment.updatePosts(posts)
                                    foundFragment = true
                                    break
                                }
                                
                                // Fragment içerisindeki childFragmentManager'da da ara
                                for (childFragment in fragment.childFragmentManager.fragments) {
                                    if (childFragment is UserPostsFragment) {
                                        childFragment.updatePosts(posts)
                                        foundFragment = true
                                        break
                                    }
                                }
                                if (foundFragment) break
                            }
                            
                            if (!foundFragment) {
                                Log.w(TAG, "UserPostsFragment bulunamadı")
                            }
                        }
                        is UserProfileViewModel.UiState.Error -> {
                            Toast.makeText(this@UserProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.likedPostsState.collect { state ->
                    when (state) {
                        is UserProfileViewModel.UiState.Loading -> {
                            // Fragment içinde yükleme gösterilecek
                        }
                        is UserProfileViewModel.UiState.Success<*> -> {
                            val posts = (state.data as? List<*>)?.filterIsInstance<ForumPost>() ?: emptyList()
                            Log.d(TAG, "Beğenilen gönderiler alındı: ${posts.size} adet")
                            
                            // Özel helper metod ile fragment'ı bul
                            val likedPostsFragment = getLikedPostsFragment()
                            if (likedPostsFragment != null) {
                                Log.d(TAG, "LikedPostsFragment bulundu, veriler güncelleniyor")
                                likedPostsFragment.updatePosts(posts)
                            } else {
                                Log.w(TAG, "LikedPostsFragment bulunamadı, 500ms sonra tekrar denenecek")
                                // Fragment henüz oluşturulmamış olabilir, kısa bir gecikme ile tekrar dene
                                lifecycleScope.launch {
                                    delay(500)
                                    val retryFragment = getLikedPostsFragment()
                                    if (retryFragment != null) {
                                        retryFragment.updatePosts(posts)
                                        Log.d(TAG, "LikedPostsFragment gecikme sonrası bulundu ve güncellendi")
                                    } else {
                                        Log.e(TAG, "LikedPostsFragment gecikme sonrası da bulunamadı")
                                        
                                        // Fragment kesinlikle bulunamadı, ViewPager'a doğrudan erişmeyi dene
                                        if (binding.viewPager.currentItem == 1) {
                                            // Şu anda "Beğendiklerim" sekmesindeyiz, sayfayı yenile
                                            binding.viewPager.adapter = pagerAdapter
                                            delay(200)
                                            viewModel.loadLikedPosts()
                                        }
                                    }
                                }
                            }
                        }
                        is UserProfileViewModel.UiState.Error -> {
                            Toast.makeText(this@UserProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
    
    @Deprecated("Replaced by onBackPressedDispatcher.onBackPressed()", level = DeprecationLevel.WARNING)
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 