package com.cloudlevi.ping.data

import android.os.Parcelable
import com.cloudlevi.ping.ext.roundTo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.Exclude
import com.google.firebase.storage.StorageReference
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    var userID: String? = "",
    var email: String? = "",
    var displayName: String? = "",
    var username: String? = "",
    var userOnline: Boolean? = false,
    @Exclude
    @set:Exclude
    @get:Exclude
    var imageRefString: String? = null
) : Parcelable {
    @IgnoredOnParcel
    var rateList: ArrayList<RatedPost> = arrayListOf()

    companion object {
        fun createFromSnapshot(
            snapshot: DataSnapshot,
            rootStorageRef: StorageReference
        ): User? {
            val user = snapshot.getValue(User::class.java) ?: return null

            if (!user.userID.isNullOrEmpty()) {
                user.imageRefString =
                    rootStorageRef
                        .child("ProfileImages")
                        .child(user.userID.toString()).toString()
            }
            return user
        }
    }
}