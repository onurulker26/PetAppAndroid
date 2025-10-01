package com.tamer.petapp.admin

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tamer.petapp.R
import com.tamer.petapp.adapter.AdminPagerAdapter

class AdminActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var pagerAdapter: AdminPagerAdapter
    
    private var bannerManagementFragment: BannerManagementFragment? = null
    private var forumManagementFragment: ForumManagementFragment? = null
    
    private val ADMIN_UIDS = listOf("ES9Vqa1m5eODBIaa0yYYNZsBh4q1", "ADMIN_UID2") // Buraya admin UID'lerini ekleyin
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        
        // Admin kontrolü
        if (!isAdmin()) {
            Toast.makeText(this, "Bu sayfaya erişim yetkiniz yok", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // ViewPager ve TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        fabAdd = findViewById(R.id.fabAdd)
        
        setupViewPager()
        
        // FAB tıklama olayı
        fabAdd.setOnClickListener {
            handleFabClick(viewPager.currentItem)
        }
        
        // ViewPager sayfa değişim dinleyicisi
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateFabVisibility(position)
            }
        })
        
        // Başlangıçta FAB görünürlüğünü ayarla
        updateFabVisibility(0)
    }
    
    private fun updateFabVisibility(position: Int) {
        // Pozisyona göre FAB'ın görünürlüğünü değiştir
        fabAdd.visibility = when (position) {
            0 -> android.view.View.VISIBLE // Duyurular sayfasında görünür
            1 -> android.view.View.GONE    // Forum yönetimi sayfasında gizli
            else -> android.view.View.VISIBLE
        }
    }
    
    private fun setupViewPager() {
        bannerManagementFragment = BannerManagementFragment()
        forumManagementFragment = ForumManagementFragment()
        
        pagerAdapter = AdminPagerAdapter(this).apply {
            addFragment(bannerManagementFragment!!, "Duyurular")
            addFragment(forumManagementFragment!!, "Raporlanan İçerikler")
            // Ek fragment isterseniz burada ekleyebilirsiniz
        }
        
        viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getTabTitle(position)
        }.attach()
    }
    
    private fun handleFabClick(position: Int) {
        when (position) {
            0 -> bannerManagementFragment?.onFabClick() // Duyurular
            1 -> forumManagementFragment?.onFabClick() // Forum Yönetimi
            // Diğer fragmentlar için gerekirse ekleyebilirsiniz
        }
    }
    
    private fun isAdmin(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return false
        
        // Önce sabit liste kontrolü
        if (ADMIN_UIDS.contains(currentUser.uid)) return true
        
        // Firestore'dan admin kontrolü de olabilir
        // Burada asenkron bir kontrol yapmak yerine, SharedPreferences veya
        // statik bir değişkende tutabileceğiniz admin bilgisini kullanabilirsiniz
        
        return false
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 