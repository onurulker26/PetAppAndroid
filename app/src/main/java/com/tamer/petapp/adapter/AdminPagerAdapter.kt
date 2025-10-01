package com.tamer.petapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    private val fragmentList = mutableListOf<Fragment>()
    private val fragmentTitleList = mutableListOf<String>()
    
    fun addFragment(fragment: Fragment, title: String) {
        fragmentList.add(fragment)
        fragmentTitleList.add(title)
    }
    
    fun getTabTitle(position: Int): String {
        return fragmentTitleList[position]
    }
    
    override fun getItemCount(): Int = fragmentList.size
    
    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
} 