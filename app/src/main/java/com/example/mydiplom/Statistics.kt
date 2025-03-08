package com.example.mydiplom

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class Statistics : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val userId = intent.getIntExtra("USER_ID", -1)
        Log.d("StatisticsActivity", "Получен userId: $userId")

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val adapter = StatisticsPagerAdapter(this, userId)
        viewPager.adapter = adapter

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "График"
                else -> "Календарь"
            }
        }.attach()
    }
}
