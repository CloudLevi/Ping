package com.cloudlevi.ping.ext

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

abstract class SimpleEventListener: ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) { }

    override fun onCancelled(error: DatabaseError) {
        Log.e("firebase_error", "error: ${error.message}")
    }
}