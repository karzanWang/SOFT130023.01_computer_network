package com.fdu.ftp_server.ui.aboutinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.fdu.ftp_server.databinding.FragmentAboutinfoBinding

class AboutinfoFragment : Fragment() {
    private lateinit var aboutinfoViewModel: AboutinfoViewModel
    private var _binding: FragmentAboutinfoBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        aboutinfoViewModel =
            ViewModelProvider(this)[AboutinfoViewModel::class.java]

        _binding = FragmentAboutinfoBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}