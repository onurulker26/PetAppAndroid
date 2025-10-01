package com.tamer.petapp.forum

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class UserProfilePagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    
    // Fragment'ları önbelleğe al
    private val fragmentsMap = mutableMapOf<Int, Fragment>()
    private val TAG = "UserProfilePagerAdapter"
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        Log.d(TAG, "createFragment çağrıldı, pozisyon: $position")
        
        // Önbellekteki fragment'ı kontrol et
        val cachedFragment = fragmentsMap[position]
        if (cachedFragment != null) {
            Log.d(TAG, "Önbellekteki fragment kullanılıyor, pozisyon: $position")
            return cachedFragment
        }
        
        // Yeni bir fragment oluştur
        val newFragment = when (position) {
            0 -> UserPostsFragment()
            1 -> LikedPostsFragment()
            else -> throw IllegalArgumentException("Geçersiz pozisyon: $position")
        }
        
        // Önbelleğe al
        fragmentsMap[position] = newFragment
        Log.d(TAG, "Yeni fragment oluşturuldu ve önbelleğe alındı, pozisyon: $position")
        return newFragment
    }
    
    // Long içeriğini daha güvenilir tutmak için
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
    
    override fun containsItem(itemId: Long): Boolean {
        return itemId in 0 until itemCount
    }
} 