package com.deandrehaijiel.xtoy.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.deandrehaijiel.xtoy.R
import com.deandrehaijiel.xtoy.databinding.FragmentSearchBinding
import com.deandrehaijiel.xtoy.model.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var cvm: ChatViewModel

    private val db = Firebase.firestore
    private val batch = db.batch()

    private var yId: String? = null
    private var yEmail: String? = null
    private var yName: String? = null
    private var yProfileImageUrl: String? = null
    private var formattedInfo: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()

        cvm = ViewModelProvider(this)[ChatViewModel::class.java]

        cvm.chatUser.observe(viewLifecycleOwner) { chatUser ->
            chatUser?.let {
                val profileImage = chatUser.xProfileImageUrl
                val profileName = chatUser.xName

                Glide.with(this)
                    .load(profileImage)
                    .into(binding.profileImage)

                binding.indeterminateBarProfileImage.visibility = View.INVISIBLE
                binding.profileImage.visibility = View.VISIBLE

                binding.profileName.text = profileName
            }
        }

        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.email.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                val email = binding.email.text.toString().trim()
                searchYEmail(email)
                return@setOnKeyListener true
            }
            false
        }

        binding.info.text = ""

        binding.pair.setOnClickListener {
            if (yId!!.isNotEmpty() && yEmail!!.isNotEmpty() && yName!!.isNotEmpty() && yProfileImageUrl!!.isNotEmpty()) {
                pairWithY(
                    yId = yId,
                    yEmail = yEmail,
                    yName = yName,
                    yProfileImageUrl = yProfileImageUrl
                )
            }
        }

        binding.logout.setOnClickListener {
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out") { dialog, _ ->
                    findNavController().navigate(R.id.action_ChatFragment_to_AccountFragment)
                    cvm.signOut()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()

            val positiveButton = alertDialogBuilder.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton?.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun searchYEmail(email: String) {
        binding.indeterminateBarYImage.visibility = View.VISIBLE
        binding.yImage.visibility = View.INVISIBLE

        db.collection("users")
            .whereEqualTo("xEmail", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(
                        requireContext(),
                        "User not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.indeterminateBarYImage.visibility = View.INVISIBLE
                    return@addOnSuccessListener
                }

                val document = querySnapshot.documents.firstOrNull()
                if (document != null && document.exists()) {
                    yId = document.getString("xId")
                    yEmail = document.getString("xEmail")
                    yName = document.getString("xName")
                    yProfileImageUrl = document.getString("xProfileImageUrl")
                    formattedInfo = getString(R.string.y_info, yName, yEmail)

                    Glide.with(this)
                        .load(yProfileImageUrl)
                        .into(binding.yImage)

                    binding.indeterminateBarYImage.visibility = View.INVISIBLE
                    binding.yImage.visibility = View.VISIBLE

                    binding.info.text = formattedInfo
                } else {
                    binding.indeterminateBarYImage.visibility = View.INVISIBLE
                    binding.yImage.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { _ ->
                Toast.makeText(
                    requireContext(),
                    "Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun pairWithY(
        yId: String?,
        yName: String?,
        yEmail: String?,
        yProfileImageUrl: String?
    ) {
        val currentUserUid = auth.currentUser?.uid

        if (currentUserUid.isNullOrEmpty() || yId.isNullOrEmpty()) {
            return
        }

        val xRef = Firebase.firestore.collection("users").document(currentUserUid)

        val xData = mapOf(
            "pair" to "1",
            "yId" to yId,
            "yName" to yName,
            "yEmail" to yEmail,
            "yProfileImageUrl" to yProfileImageUrl
        )

        batch.update(xRef, xData)

        val yRef = Firebase.firestore.collection("users").document(yId)

        val yData = mapOf(
            "pair" to "1",
            "yId" to cvm.chatUser.value?.xId.orEmpty(),
            "yName" to cvm.chatUser.value?.xName.orEmpty(),
            "yEmail" to cvm.chatUser.value?.xEmail.orEmpty(),
            "yProfileImageUrl" to cvm.chatUser.value?.xProfileImageUrl.orEmpty()
        )

        batch.update(yRef, yData)

        batch.commit()

        findNavController().navigate(R.id.action_SearchFragment_to_ChatFragment)
    }
}