package com.example.ecosystems

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.ecosystems.DeviceInfoActivityFragments.DeviceInfoFragment
import com.example.ecosystems.ProfileActivityFragments.ChangePassFragment
import com.example.ecosystems.ProfileActivityFragments.ProfileInfoFragment
import com.example.ecosystems.ProfileActivityFragments.SettingsFragment
import java.io.Serializable

class ProfileFragmentPageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle,
                                 val personalAccountData: Map<String, Any?>):FragmentStateAdapter(fragmentManager, lifecycle) {

    lateinit var fragment: Fragment
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            fragment = ProfileInfoFragment()
            val bundle = Bundle()
            bundle.putSerializable("personalAccountData", personalAccountData as Serializable)
            fragment.arguments = bundle
            return fragment
        }
        else if (position == 1) {
            fragment = ChangePassFragment()
            val bundle = Bundle()
            bundle.putSerializable("personalAccountData", personalAccountData as Serializable)
            fragment.arguments = bundle
            return fragment
        }
        else {
            fragment = SettingsFragment()
            val bundle = Bundle()
            bundle.putSerializable("personalAccountData", personalAccountData as Serializable)
            fragment.arguments = bundle
            return fragment
        }

    }
}