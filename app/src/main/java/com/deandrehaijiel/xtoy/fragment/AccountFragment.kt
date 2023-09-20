package com.deandrehaijiel.xtoy.fragment

import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.deandrehaijiel.xtoy.R
import com.deandrehaijiel.xtoy.databinding.FragmentAccountBinding
import com.deandrehaijiel.xtoy.model.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var cvm: ChatViewModel

    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var imageUri: Uri

    private val db = Firebase.firestore

    private var isVisible = false

    // Registers a photo picker activity launcher in single-select mode.
    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                imageUri = uri
                loadImage(imageUri)
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textview2.setOnClickListener {
            toggleFragment()
        }

        binding.login.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            var hasError = false

            if (!isValidEmail(email)) {
                binding.email.error = "Invalid email address"
                hasError = true
            }

            if (password.isEmpty() || password.length < 6) {
                binding.password.error = "Invalid password"
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            loginUser(email, password)
        }

        binding.create.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val rePassword = binding.repassword.text.toString().trim()
            var hasError = false

            if (binding.profileImage.drawable == null) {
                Toast.makeText(
                    requireContext(),
                    "Profile picture cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
                hasError = true
            }

            if (name.isEmpty()) {
                binding.name.error = "Invalid name"
                hasError = true
            }

            if (!isValidEmail(email)) {
                binding.email.error = "Invalid email address"
                hasError = true
            }

            if (password.isEmpty() || password.length < 6) {
                binding.password.error = "Invalid password"
                hasError = true
            }

            if (rePassword.isEmpty() || rePassword.length < 6) {
                binding.repassword.error = "Invalid password"
                hasError = true
            }

            if (password != rePassword) {
                binding.repassword.error = "Passwords do not match"
                hasError = true
            }

            if (hasError) {
                return@setOnClickListener
            }

            createUser(email, password, name)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun toggleFragment() {
        if (isVisible) {
            binding.title.text = getString(R.string.account)

            val layoutParams = binding.textview1.layoutParams as ViewGroup.MarginLayoutParams
            val marginTopInPixels = resources.getDimensionPixelSize(R.dimen.login_margin_top_value)
            layoutParams.setMargins(0, marginTopInPixels, 0, 0)
            binding.textview1.layoutParams = layoutParams

            binding.textview1.text = getString(R.string.login1)
            val login2Text = getString(R.string.login2)
            val login2Underlined = SpannableString(login2Text)
            login2Underlined.setSpan(UnderlineSpan(), 0, login2Text.length, 0)
            binding.textview2.text = login2Underlined

            binding.name.text.clear()
            binding.email.text.clear()
            binding.password.text.clear()
            binding.repassword.text.clear()
        } else {
            binding.title.text = getString(R.string.create)

            val layoutParams = binding.textview1.layoutParams as ViewGroup.MarginLayoutParams
            val marginTopInPixels = resources.getDimensionPixelSize(R.dimen.account_margin_top_value)
            layoutParams.setMargins(0, marginTopInPixels, 0, 0)
            binding.textview1.layoutParams = layoutParams

            binding.textview1.text = getString(R.string.create1)
            val create2Text = getString(R.string.create2)
            val create2Underlined = SpannableString(create2Text)
            create2Underlined.setSpan(UnderlineSpan(), 0, create2Text.length, 0)
            binding.textview2.text = create2Underlined

            binding.name.text.clear()
            binding.email.text.clear()
            binding.password.text.clear()
            binding.repassword.text.clear()
        }

        isVisible = !isVisible
        binding.imagePicker.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.name.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.repassword.visibility = if (isVisible) View.VISIBLE else View.GONE
        binding.create.visibility = if (isVisible) View.VISIBLE else View.GONE

        binding.imagePicker.setOnClickListener {
            // Launch the photo picker and let the user choose only images.
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    cvm = ViewModelProvider(this)[ChatViewModel::class.java]

                    cvm.chatUser.observe(viewLifecycleOwner) { chatUser ->
                        chatUser?.let {
                            val pair = chatUser.pair
                            if (pair.isNotEmpty()) {
                                completedLogin()
                            } else {
                                createdAccount()
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Login failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun loadImage(imageUri: Uri?) {
        val profileImage = binding.profileImage
        if (imageUri != null) {
            profileImage.setImageURI(imageUri)
            profileImage.visibility = View.VISIBLE
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    private fun uploadImageAndStoreUserData(user: FirebaseUser?, name: String) {
        if (user == null || binding.profileImage.drawable == null) {
            return
        }

        val uid = user.uid
        val storageRef: StorageReference = storage.reference.child(uid)

        requireContext().contentResolver.openInputStream(imageUri)?.use { imageStream ->
            val buffer = ByteArrayOutputStream()
            val bufferData = ByteArray(1024)
            var bytesRead: Int

            while (imageStream.read(bufferData, 0, 1024).also { bytesRead = it } != -1) {
                buffer.write(bufferData, 0, bytesRead)
            }

            val imageData = buffer.toByteArray()

            val uploadTask = storageRef.putBytes(imageData)
            uploadTask.addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Failed to upload image to Storage",
                    Toast.LENGTH_SHORT
                ).show()
            }.addOnSuccessListener { _ ->
                storageRef.downloadUrl.addOnCompleteListener { urlTask ->
                    if (urlTask.isSuccessful) {
                        val downloadUrl = urlTask.result
                        if (downloadUrl != null) {
                            val userData = mapOf(
                                "xId" to uid,
                                "xName" to name,
                                "xEmail" to user.email,
                                "xProfileImageUrl" to downloadUrl.toString(),
                                "pair" to "" // Replace with the actual value for pair
                            )

                            db.collection("users")
                                .document(uid).set(userData)
                                .addOnSuccessListener {
                                    createdAccount()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Failed to store user data",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to retrieve image URL",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun createUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    uploadImageAndStoreUserData(auth.currentUser, name)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to create account",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun isValidEmail(email: CharSequence?): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email!!).matches()
    }

    private fun completedLogin() {
        findNavController().navigate(R.id.action_AccountFragment_to_ChatFragment)
    }

    private fun createdAccount() {
        findNavController().navigate(R.id.action_AccountFragment_to_SearchFragment)
    }
}