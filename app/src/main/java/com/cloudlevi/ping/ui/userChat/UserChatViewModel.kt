package com.cloudlevi.ping.ui.userChat

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudlevi.ping.MediaAdapterVM
import com.cloudlevi.ping.data.*
import com.cloudlevi.ping.ext.SimpleEventListener
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserChatViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val state: SavedStateHandle
) : ViewModel(), MediaAdapterVM {

    private val allChatsRef = FirebaseDatabase.getInstance().getReference("chats")
    private val chatsIndexRef = FirebaseDatabase.getInstance().getReference("chatIndex")
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    private val storageRef = FirebaseStorage.getInstance().getReference("ChatImages")

    var receiverID: String = ""

    private val userChatEventChannel = Channel<UserChatEvent>()
    var userChatEvent = userChatEventChannel.receiveAsFlow()

    val receiverModel = MutableLiveData<User?>()

    var userID: String = ""
    var userName: String = ""

    var chatID: String = ""

    var messages: MutableList<Message> = mutableListOf()

    val attachmentsAdapter = AttachmentAdapter(this)
    var userChatAdapter: UserChatAdapter? = null

    val attachmentsList = arrayListOf<MediaAttachment>()
    private var replacingImageAtIndex = 0

    fun fragmentCreate(userModel: User?, mChatItem: ChatListItem?) = viewModelScope.launch {

        userID = preferencesManager.getUserID()
        userName = preferencesManager.getUserDisplayName()

        messages = mChatItem?.messages?.toMutableList() ?: mutableListOf()
        updateMessages()

        receiverModel.value = userModel
        userChatAdapter = UserChatAdapter(this@UserChatViewModel)
        userChatAdapter?.update()

        chatID = mChatItem?.chatID ?: ""
        receiverID = userModel?.userID ?: ""

        observeUserChanges()

        if (!chatExists())
            findCurrentChat()
        else observeChatChanges(chatID)
    }

    private fun findCurrentChat() {
        val currentUserChatsQuery: Query = chatsIndexRef
            .orderByChild("users")
            .startAt("%${userID}%")
            //.endAt("$userID\uf8ff")

        currentUserChatsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                snapshot.children.forEach { childSnapshot ->
                    val chatItem = childSnapshot.getValue(ChatListItem::class.java)
                    chatItem?.indexUsers()

                    if (chatItem?.userList?.size == 2 && chatItem.userList.containsAll(
                            listOf(
                                userID,
                                receiverID
                            )
                        )
                    ) {
                        chatID = childSnapshot.key ?: ""
                        observeChatChanges(chatID)
                        return@forEach
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "onCancelled: ${error.message}")
            }

        })
    }

    fun sendMessage(message: String) {
        val messageTimeStamp = System.currentTimeMillis()
        val mediaCount = attachmentsList.size

        if (chatExists()) {
            val messagesRef = allChatsRef.child(chatID).child("messages")
            val chatIndexRef = chatsIndexRef.child(chatID)
            val messageKey = messagesRef.push().key ?: ""

            val messageObject = Message(
                messageKey,
                message,
                userID,
                messageTimeStamp,
                false,
                mediaCount
            )
            val mapOfIndex = mapOf(
                Pair("lastMessage", message),
                Pair("lastTimeStamp", messageTimeStamp),
                Pair("mediaCount", mediaCount)
            )

            uploadImagesIfAny(messageKey) {
                messagesRef.child(messageKey).setValue(messageObject)
                chatIndexRef.updateChildren(mapOfIndex)
                imageUploadFinished()
            }

        } else {
            createNewChatWithMessage(message, messageTimeStamp, mediaCount)
        }
    }

    private fun createNewChatWithMessage(message: String, messageTimeStamp: Long, mediaCount: Int) {
        val newChatKey = allChatsRef.push().key ?: ""
        val messageID = "0"

        val messages = listOf(
            Message(messageID, message, userID, messageTimeStamp, false, mediaCount)
        )

        val indexMap = mapOf(
            Pair("users", "$userID, $receiverID"),
            Pair("lastMessage", message),
            Pair("lastTimeStamp", messageTimeStamp),
            Pair("mediaCount", mediaCount)
        )

        uploadImagesIfAny(messageID) {
            allChatsRef
                .child(newChatKey)
                .setValue(Chat(newChatKey, messages))
                .addOnSuccessListener {
                    chatID = newChatKey
                    observeChatChanges(chatID)

                    chatsIndexRef.child(chatID).setValue(indexMap)
                    imageUploadFinished()
                }
                .addOnFailureListener {
                    chatID = ""
                    imageUploadFinished()
                }
        }
    }

    private fun observeUserChanges() {
        if (receiverID.isNotEmpty()) {
            usersRef.child(receiverID).addValueEventListener(userEventListener)
        }
    }

    fun imageAttachmentReceived(uri: Uri, byteArrayCompressed: ByteArray) {
        attachmentsList.add(MediaAttachment(uri, byteArrayCompressed))
        attachmentsAdapter.update()
        //attachmentsAdapter.itemAdded()
        changeAttachmentVisibility(true)
    }

    fun removeImageAt(pos: Int) {
        attachmentsList.removeAt(pos)
        attachmentsAdapter.update()
        //attachmentsAdapter.itemRemovedAt(pos)
        if (attachmentsList.isEmpty()) {
            changeAttachmentVisibility(false)
        }
    }

    private fun uploadImagesIfAny(messageKey: String, finished: () -> Unit) {
        var imageCount = 0
        val hasImages = attachmentsList.isNotEmpty()

        if (!hasImages) {
            finished()
            return
        }

        attachmentsList.forEachIndexed { index, mediaAttachment ->
            storageRef
                .child(messageKey)
                .child(index.toString())
                .putBytes(mediaAttachment.byteArray!!)
                .addOnCompleteListener {
                    imageCount++
                    if (imageCount == attachmentsList.size) finished()
                }
        }
    }

    fun replaceImageAt(pos: Int) {
        replacingImageAtIndex = pos
        singleImageChooser()
    }

    fun imageReplacementReceived(uri: Uri?, byteArrayCompressed: ByteArray) {
        uri ?: return
        attachmentsList[replacingImageAtIndex] = MediaAttachment(uri, byteArrayCompressed)
        attachmentsAdapter.update()
        changeAttachmentVisibility(true)
    }

    fun addImageClick() {
        addClick()
    }

    private val userEventListener = object : SimpleEventListener() {
        override fun onDataChange(snapshot: DataSnapshot) {
            receiverModel.value = snapshot.getValue(User::class.java)
        }
    }

    fun observeChatChanges(chatID: String?) {
        if (chatID == null) return
        allChatsRef.child(chatID).addValueEventListener(chatEventListener)
    }

    private fun indexOfMessage(messageID: String) =
        messages.indexOfFirst { it.messageID == messageID }

    private fun getMediaForMessage(
        message: Message,
        position: Int
    ) {
        val mediaCount = message.mediaCount ?: 0

        (0 until mediaCount).forEach { index ->
            storageRef.child(message.messageID ?: "")
                .child(index.toString())
                .downloadUrl.addOnSuccessListener { uri ->
                    messages[position].imagesList[index] = uri
                    userChatAdapter?.updateImageForMessageAtPos(position, index, uri)
                    notifyItemChanged(position)
                }
        }
    }

    private val chatEventListener = object : SimpleEventListener() {
        override fun onDataChange(snapshot: DataSnapshot) {
            val messagesSnapshot = snapshot.child("messages").children.map { messageSnapShot ->
                val value = messageSnapShot.getValue(Message::class.java) ?: return

                if (value.senderID != userID && value.read == false) {
                    snapshot.child("messages").child(messageSnapShot.key ?: "")
                        .ref.updateChildren(
                            mapOf(Pair("read", true))
                        )
                }
                value
            }.sortedBy { it.timeStamp }

            messages = messagesSnapshot.toMutableList()
            userChatAdapter?.update()
            updateMessages()
        }
    }

    private fun changeAttachmentVisibility(isVisible: Boolean) = viewModelScope.launch {
        userChatEventChannel.send(UserChatEvent.AttachmentVisibility(isVisible))
    }

    private fun addClick() = viewModelScope.launch {
        userChatEventChannel.send(UserChatEvent.AddClick)
    }

    private fun singleImageChooser() = viewModelScope.launch {
        userChatEventChannel.send(UserChatEvent.ChooseSingleImage)
    }

    private fun toggleLoading(isLoading: Boolean) = viewModelScope.launch {
        userChatEventChannel.send(UserChatEvent.ToggleLoading(isLoading))
    }

    private fun imageUploadFinished() = viewModelScope.launch {
        userChatEventChannel.send(UserChatEvent.ImageUploadFinished)
    }

    private fun updateMessages() {
        messages.forEachIndexed { index, msg ->
            if (msg.hasMedia() && msg.imagesList.isEmpty()) {
                val list = (0 until (msg.mediaCount ?: 0)).map { Uri.EMPTY }

                msg.imagesList = list.toMutableList()
                userChatAdapter?.updateImagesForPos(index, list.toMutableList())

                getMediaForMessage(msg, index)
            }
        }

        viewModelScope.launch {
            userChatEventChannel.send(UserChatEvent.UpdateMessages)
        }
    }

    private fun openImageAt(messagePos: Int, startPos: Int, imagesList: List<Uri>) =
        viewModelScope.launch {
            userChatEventChannel.send(UserChatEvent.OpenImageAt(messagePos, startPos, imagesList))
        }

    private fun notifyItemChanged(adapterPos: Int) = viewModelScope.launch {
        userChatEventChannel.send(UserChatEvent.NotifyImageChanged(adapterPos))
    }

    private fun chatExists(): Boolean = chatID.trim().isNotEmpty()

    fun onDestroy() {
        allChatsRef.child(chatID).removeEventListener(chatEventListener)
        usersRef.child(receiverID).removeEventListener(userEventListener)
    }

    sealed class UserChatEvent {
        data class AttachmentVisibility(val isVisible: Boolean) : UserChatEvent()
        data class ToggleLoading(val isLoading: Boolean) : UserChatEvent()
        data class OpenImageAt(val messagePos: Int, val startPos: Int, val imagesList: List<Uri>) :
            UserChatEvent()

        data class NotifyImageChanged(val adapterPos: Int) : UserChatEvent()
        object ImageUploadFinished : UserChatEvent()
        object AddClick : UserChatEvent()
        object UpdateMessages : UserChatEvent()
        object ChooseSingleImage : UserChatEvent()
    }

    override fun getCurrentListByID(id: String?): MutableList<Uri> {
        val message = messages.find { it.messageID == id }
        val list = message?.imagesList ?: mutableListOf()
        return list
    }

    override fun getCurrentListByPos(pos: Int): MutableList<Uri> {
        val list = messages[pos].imagesList
        return list
    }

    override fun onMediaImageClick(id: String?, position: Int?) {
        val index = indexOfMessage(id ?: "")
        openImageAt(index, position ?: 0, messages[index].imagesList)
    }
}