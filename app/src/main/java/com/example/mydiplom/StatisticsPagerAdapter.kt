package com.example.mydiplom

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class StatisticsPagerAdapter(fragmentActivity: FragmentActivity, private val userId: Int) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GraphFragment.newInstance(userId)  // Передаем userId
            else -> CalendarFragment.newInstance(userId)
        }
    }
}

