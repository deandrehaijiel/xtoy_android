package com.deandrehaijiel.xtoy.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.deandrehaijiel.xtoy.R
import com.deandrehaijiel.xtoy.databinding.FragmentLandingBinding
import com.deandrehaijiel.xtoy.model.ChatViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LandingFragment : Fragment() {

    private var _binding: FragmentLandingBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var cvm: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()

        _binding = FragmentLandingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = auth.currentUser

        if (currentUser != null) {
            cvm = ViewModelProvider(this)[ChatViewModel::class.java]

            cvm.chatUser.observe(viewLifecycleOwner) { chatUser ->
                chatUser?.let {
                    val pair = chatUser.pair
                    if (pair.isNotEmpty()) {
                        binding.landing.setOnClickListener {
                            toChat()
                        }
                    } else {
                        binding.landing.setOnClickListener {
                            toSearch()
                        }
                    }
                }
            }
        } else {
            binding.landing.setOnClickListener {
                toAccount()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toChat() {
        findNavController().navigate(R.id.action_LandingFragment_to_ChatFragment)
    }

    private fun toSearch() {
        findNavController().navigate(R.id.action_LandingFragment_to_SearchFragment)
    }

    private fun toAccount() {
        findNavController().navigate(R.id.action_LandingFragment_to_AccountFragment)
    }
}
