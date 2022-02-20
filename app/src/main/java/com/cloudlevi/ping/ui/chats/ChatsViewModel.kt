package com.cloudlevi.ping.ui.chats

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.data.ChatListItem
import com.cloudlevi.ping.data.Message
import com.cloudlevi.ping.data.PreferencesManager
import com.cloudlevi.ping.data.User
import com.cloudlevi.ping.ext.SimpleEventListener
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ChatsViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel() {

    private val allChatsRef = FirebaseDatabase.getInstance().getReference("chats")
    private val chatsIndexRef = FirebaseDatabase.getInstance().getReference("chatIndex")
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    private val storageInstance = FirebaseStorage.getInstance()

    private val eventChannel = Channel<UserChatsEvent>()
    val userChatsEvent = eventChannel.receiveAsFlow()

    val chatsAdapter = ChatsAdapter(this)

    val isLoading = MutableLiveData(false)

    var mListener: ValueEventListener? = null

    var currentUserChatsQuery: Query? = null

    var listenerInProgress = false
    var snapShotPending: DataSnapshot? = null

    var userID = ""

    var allChats: ArrayList<ChatListItem?> = arrayListOf()

    var observedUserIDList = mutableListOf<String>()

    init {
        viewModelScope.launch {
            userID = preferencesManager.getUserID()

            currentUserChatsQuery = chatsIndexRef
                .orderByChild("users")
                .startAt("%$userID%")
            //.endAt("$userID\uf8ff")
        }
    }

    fun fragmentCreate() {
        if (allChats.isEmpty())
            isLoading.value = true

        mListener = object : SimpleEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (listenerInProgress) {
                    snapShotPending = snapshot
                    return
                }
                processSnapshot(snapshot)
            }
        }

        currentUserChatsQuery?.addValueEventListener(mListener!!)

    }

    private fun processSnapshot(snapshot: DataSnapshot?) {
        if (snapshot == null) return

        listenerInProgress = true

        allChats.clear()

        var count = 0L
        val childrenCount = snapshot.childrenCount

        if (childrenCount == 0L) {
            isLoading.value = false
            sendMessage("No chats found!")
        }

        snapshot.children.forEach { childSnapshot ->
            val chatItem = childSnapshot.getValue(ChatListItem::class.java)

            chatItem?.indexUsers()
            chatItem?.chatID = childSnapshot.key ?: ""

            if (chatItem?.userList?.contains(userID) == true) {
                usersRef.child(chatItem.getUserIDExcluding(userID) ?: "")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {

                            val otherUserModel = User.createFromSnapshot(snapshot, storageInstance.reference)
                            chatItem.userModel = otherUserModel
                            val uID = otherUserModel?.userID ?: ""

                            if (!observedUserIDList.contains(uID)) {
                                observedUserIDList.add(uID)
                                usersRef.child(uID).addValueEventListener(userListener)
                            }

                            allChatsRef
                                .child(chatItem.chatID)
                                .child("messages")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val unreadCount: Int
                                        val messages = snapshot.children.map {
                                            it.getValue(Message::class.java) ?: return
                                        }.sortedBy { it.timeStamp }

                                        unreadCount = messages.count { message ->
                                            message.read == false && message.senderID != userID
                                        }

                                        chatItem.unreadCount = unreadCount
                                        chatItem.messages = messages
                                        allChats.add(chatItem)

                                        count++
                                        if (count == childrenCount) chatsReceived()
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "onCancelled: ${error.message}")
                                        count++
                                        if (count == childrenCount) chatsReceived()
                                    }

                                })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "onCancelled: ${error.message}")
                            count++
                            if (count == childrenCount) chatsReceived()
                        }

                    })
            } else {
                count++
                if (count == childrenCount) chatsReceived()
            }
        }
    }

    private val userListener = object : SimpleEventListener() {
        override fun onDataChange(snapshot: DataSnapshot) {
            val chatIndex = allChats.indexOfFirst {
                it?.userModel?.userID == snapshot.key
            }
            allChats.getOrNull(chatIndex)?.userModel =
                User.createFromSnapshot(snapshot, storageInstance.reference)

            allChats.getOrNull(chatIndex) ?: return
            chatsAdapter.updateSoft(chatIndex)
        }
    }

    fun onDestroy() {
        if (mListener != null)
            currentUserChatsQuery?.removeEventListener(mListener!!)
    }

    fun chatClicked(chatListItem: ChatListItem?) {

        if (chatListItem == null)
            sendMessage("Chat not found. Please try later.")
        else openChat(chatListItem)
    }

    private fun chatsReceived() {
        allChats.sortByDescending {
            it?.lastTimeStamp
        }

        chatsAdapter.update()
        listenerInProgress = false

        if (snapShotPending != null) {
            processSnapshot(snapShotPending)
            snapShotPending = null
        }
    }

    private fun openChat(chatListItem: ChatListItem?) = viewModelScope.launch {
        eventChannel.send(UserChatsEvent.OpenChat(chatListItem))
    }

    private fun sendMessage(message: String) = viewModelScope.launch {
        eventChannel.send(UserChatsEvent.SendMessage(message))
    }

    fun storageRefFromString(imgRefString: String?): StorageReference? {
        imgRefString ?: return null
        return storageInstance.getReferenceFromUrl(imgRefString)
    }

    sealed class UserChatsEvent {
        data class OpenChat(val chatListItem: ChatListItem?) : UserChatsEvent()
        data class SendMessage(val message: String) : UserChatsEvent()
    }
}