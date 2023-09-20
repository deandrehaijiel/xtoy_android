package com.deandrehaijiel.xtoy.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class ChatViewModel : ViewModel() {
    private val _chatUser = MutableLiveData<ChatUser>()
    val chatUser: MutableLiveData<ChatUser> get() = _chatUser

    private val _chatMessages = MutableLiveData<List<ChatMessage>>()
    val chatMessages: LiveData<List<ChatMessage>> get() = _chatMessages

    private var messageListener: ListenerRegistration? = null

    init {
        fetchCurrentUser()
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
            val db = Firebase.firestore
            val userDoc = db.collection("users").document(uid)

            userDoc.get().addOnSuccessListener { document ->
                if (document != null) {
                    val data = document.data
                    if (data != null) {
                        _chatUser.value = ChatUser(
                            xId = uid,
                            xEmail = data["xEmail"] as? String ?: "",
                            xName = data["xName"] as? String ?: "",
                            xProfileImageUrl = data["xProfileImageUrl"] as? String ?: "",
                            pair = data["pair"] as? String ?: "",
                            yId = data["yId"] as? String ?: "",
                            yEmail = data["yEmail"] as? String ?: "",
                            yName = data["yName"] as? String ?: "",
                            yProfileImageUrl = data["yProfileImageUrl"] as? String ?: ""
                        )

                        if (_chatUser.value?.pair != "") {
                            fetchMessages()
                        }
                    }
                }
            }
        }
    }

    private fun fetchMessages() {
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val toId = _chatUser.value?.yId

        if (fromId != null && toId != null) {
            val db = Firebase.firestore
            messageListener = db.collection("messages")
                .document(fromId)
                .collection(toId)
                .orderBy("timestamp")
                .addSnapshotListener { value, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    val messages = mutableListOf<ChatMessage>()
                    for (doc in value!!) {
                        val message = doc.toObject(ChatMessage::class.java)
                        messages.add(message)
                    }

                    _chatMessages.value = messages
                }
        }
    }

    fun send(message: String) {
        val fromId = FirebaseAuth.getInstance().currentUser?.uid
        val toId = _chatUser.value?.yId

        if (fromId != null && toId != null) {
            val xDocument = Firebase.firestore
                .collection("messages")
                .document(fromId)
                .collection(toId)
                .document()

            val messageData = mapOf(
                "fromId" to fromId,
                "toId" to toId,
                "message" to message,
                "timestamp" to Date()
            )

            xDocument.set(messageData)

            val yDocument = Firebase.firestore
                .collection("messages")
                .document(toId)
                .collection(fromId)
                .document()

            yDocument.set(messageData)
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
