package com.example.ecosystems

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.ecosystems.DeviceInfoActivityFragments.DeviceInfoFragment
import com.example.ecosystems.DeviceInfoActivityFragments.NotificationTypeFragment
import com.example.ecosystems.DeviceInfoActivityFragments.NotificationsFragment
import com.example.ecosystems.ProfileActivityFragments.ChangePassFragment
import com.example.ecosystems.ProfileActivityFragments.ProfileInfoFragment
import com.example.ecosystems.ProfileActivityFragments.SettingsFragment
import java.io.Serializable


class DeviceInfoFragmentPageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle,
                                    val currentDevice: Map<String, Any?>):
    FragmentStateAdapter(fragmentManager, lifecycle) {

    lateinit var fragment: Fragment

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0){
            fragment = DeviceInfoFragment()
            val bundle = Bundle()
            bundle.putSerializable("currentDevice", currentDevice as Serializable)
            fragment.arguments = bundle
            return fragment
        }
        else if (position == 1) {
            fragment = NotificationTypeFragment()
            val bundle = Bundle()
            bundle.putSerializable("currentDevice", currentDevice as Serializable)
            fragment.arguments = bundle
            return fragment
        }
        else {
            fragment = NotificationsFragment()
            val bundle = Bundle()
            bundle.putSerializable("currentDevice", currentDevice as Serializable)
            fragment.arguments = bundle
            return fragment
        }
    }
}