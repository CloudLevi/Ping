package com.cloudlevi.ping.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class ChatListItem(
    val users: String? = null,
    val lastMessage: String? = null,
    val lastTimeStamp: Long? = null,
    var chatID: String = "",
    var messages: List<Message> = listOf(),
    var imageDownloadURL: String = "",
    var title: String = "",
    var unreadCount: Int = 0,
    var userList: MutableList<String> = mutableListOf(),
    var userModel: User? = null
) : Parcelable {

    fun indexUsers() {
        userList.clear()

        var lastCommaIndex = 0
        users?.forEachIndexed { index, c ->
            if (c.toString() == "," || index == users.lastIndex) {
                val userIndex = users.substring(
                    lastCommaIndex,
                    if (index == users.lastIndex) index + 1 else index
                )

                if (userIndex.isNotEmpty() && userIndex.length > 1)
                    userList.add(userIndex)

                lastCommaIndex = index + 2
            }
        }
    }

    fun decreaseUnreadCount(){
        if (unreadCount > 0)
            unreadCount -= 1
    }

    fun getUserIDExcluding(excludedID: String) =
        userList.find { it != excludedID } ?: ""
}