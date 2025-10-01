package com.tamer.petapp.forum

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tamer.petapp.databinding.FragmentUserPostsBinding
import com.tamer.petapp.model.ForumPost

class UserPostsFragment : Fragment() {
    private var _binding: FragmentUserPostsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postsAdapter: UserPostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
    }
    
    private fun setupRecyclerView() {
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
        binding.progressBar.visibility = View.GONE
        
        if (posts.isEmpty()) {
            binding.tvNoContent.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvNoContent.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            postsAdapter.submitList(posts)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 