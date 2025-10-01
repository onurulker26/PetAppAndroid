package com.tamer.petapp.forum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tamer.petapp.databinding.FragmentLikedPostsBinding
import com.tamer.petapp.model.ForumPost

class LikedPostsFragment : Fragment() {
    private var _binding: FragmentLikedPostsBinding? = null
    private val binding get() = _binding!!
    
    private var initialized = false
    private var pendingPosts: List<ForumPost>? = null
    private lateinit var postsAdapter: UserPostsAdapter
    private val TAG = "LikedPostsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate çağrıldı")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView çağrıldı")
        _binding = FragmentLikedPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated çağrıldı")
        
        setupRecyclerView()
        
        // Başlangıçta progress bar göster
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.tvNoContent.visibility = View.GONE
        
        // Fragment initialize olduğunda bekleyen verileri varsa göster
        initialized = true
        pendingPosts?.let {
            Log.d(TAG, "Bekleyen ${it.size} gönderi var, şimdi gösteriliyor")
            updatePosts(it)
            pendingPosts = null
        }
    }
    
    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView çağrıldı")
        postsAdapter = UserPostsAdapter(
            onItemClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java)
                intent.putExtra("postId", post.id)
                startActivity(intent)
            }
        )
        
        binding.recyclerView.apply {
            adapter = postsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    fun updatePosts(posts: List<ForumPost>) {
        Log.d(TAG, "updatePosts çağrıldı, ${posts.size} post alındı")
        
        // Fragment henüz tamamen oluşturulmamışsa verileri beklet
        if (!initialized || !isAdded || _binding == null) {
            Log.d(TAG, "Fragment henüz initialize olmamış, veriler beklemeye alındı")
            pendingPosts = posts
            return
        }
        
        try {
            // Her durumda progress bar'ı gizle
            binding.progressBar.visibility = View.GONE
            
            if (posts.isEmpty()) {
                binding.tvNoContent.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                Log.d(TAG, "Gösterilecek post bulunamadı")
            } else {
                binding.tvNoContent.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                postsAdapter.submitList(ArrayList(posts)) // Yeni bir liste oluşturarak zorla güncelleme yap
                Log.d(TAG, "Post listesi güncellendi: ${posts.size} post gösteriliyor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updatePosts sırasında hata: ${e.message}", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume çağrıldı")
        
        // Fragment aktif olduğunda, aktiviteye veri yükleme sinyali gönderelim
        (activity as? UserProfileActivity)?.let {
            Log.d(TAG, "TabLayout pozisyonu kontrol ediliyor")
            if (it.binding.viewPager.currentItem == 1) {
                Log.d(TAG, "Beğenilen gönderiler sekmesindeyiz, verileri yeniliyoruz")
                it.viewModel.loadLikedPosts()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView çağrıldı")
        initialized = false
        _binding = null
    }
} 