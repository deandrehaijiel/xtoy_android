package com.deandrehaijiel.xtoy.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.deandrehaijiel.xtoy.model.ChatViewModel
import com.deandrehaijiel.xtoy.R
import com.deandrehaijiel.xtoy.databinding.FragmentChatBinding
import com.deandrehaijiel.xtoy.model.MessageAdapter

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cvm: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        cvm = ViewModelProvider(this)[ChatViewModel::class.java]

        cvm.chatUser.observe(viewLifecycleOwner) { chatUser ->
            chatUser?.let {
                val userImage = chatUser.yProfileImageUrl
                val userName = chatUser.yName

                Glide.with(this)
                    .load(userImage)
                    .into(binding.userImage)

                binding.indeterminateBarUserImage.visibility = View.INVISIBLE
                binding.userImage.visibility = View.VISIBLE

                binding.userName.text = userName
            }
        }

        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageAdapter = MessageAdapter()

        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messageAdapter
        }

        cvm.chatMessages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.updateMessages(messages)

            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        }

        binding.send.setOnClickListener {
            val message = binding.message.text.toString()
            cvm.send(message)
            binding.message.text.clear()
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
}
