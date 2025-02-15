package com.example.ecosystems

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileActivityLayoutFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileActivityLayoutFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var fragmentAdapter: FragmentPageAdapter
    private lateinit var saveChangesButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_activity_layout, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager2 = view.findViewById(R.id.viewPager2)
        saveChangesButton = view.findViewById(R.id.saveChangesButton)
        saveChangesButton.setOnClickListener{
            if(tabLayout.selectedTabPosition == 0)
            {
                saveChangesButton.setText("Изменить")

                val setName = view.findViewById<EditText>(R.id.editNameText)
                val setSurname = view.findViewById<EditText>(R.id.editSurnameText)
                val setEmail = view.findViewById<EditText>(R.id.editTextEmailAddress)
                val setPhone = view.findViewById<EditText>(R.id.editTextPhone)
                val setOrganization = view.findViewById<EditText>(R.id.editOrganisationText)

                setName.isEnabled = if(setName.isEnabled) false else true
                setSurname.isEnabled = if(setSurname.isEnabled) false else true
                setEmail.isEnabled = if(setEmail.isEnabled) false else true
                setPhone.isEnabled = if(setPhone.isEnabled) false else true
                setOrganization.isEnabled = if(setOrganization.isEnabled) false else true
            }
            if(tabLayout.selectedTabPosition == 1)
            {
                saveChangesButton.setText("Изменить")
            }
            if(tabLayout.selectedTabPosition == 2)
            {
                saveChangesButton.setText("Сохранить")
            }
        }

        val fragmentAdapter = FragmentPageAdapter(parentFragmentManager,lifecycle)
        viewPager2.adapter = fragmentAdapter

        tabLayout.addOnTabSelectedListener(object  : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    viewPager2.currentItem = tab.position
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileActivityLayoutFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileActivityLayoutFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}