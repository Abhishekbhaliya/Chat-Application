package com.bhaliya.mr_chat.Adpter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color.green
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bhaliya.mr_chat.MessageActivity
import com.bhaliya.mr_chat.R
import com.bhaliya.mr_chat.databinding.ItemProfileBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class UserAdpter(var context: Context,var userList : ArrayList<com.bhaliya.mr_chat.Model.User>) :
    RecyclerView.Adapter<UserAdpter.UserViewHolder>() {


    var firebaseFirestore: FirebaseFirestore? = null
    var auth: FirebaseAuth? = null
    var database: FirebaseDatabase? = null
    var receiverUid: String? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {

        var view = LayoutInflater.from(context).inflate(R.layout.item_profile,parent,false)
        return UserViewHolder(view)
    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {

        firebaseFirestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()




        val currentuser = userList[position]

        receiverUid = currentuser.uid
        holder.binding.nameTextView.text = currentuser.name
        holder.binding.statusTextView.setTextColor(R.color.green)

        database!!.reference.child("presence").child(receiverUid!!)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("ResourceAsColor")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.getValue(String::class.java)
                        if (!status!!.isEmpty()) {
                            if (status == "Offline") {
                                holder.binding.statusTextView.visibility = View.VISIBLE
                            } else {
                                holder.binding.statusTextView.text= status
                                holder.binding.statusTextView.setTextColor(R.color.green)
                                holder.binding.statusTextView.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })


        Glide.with(context).
        load(currentuser.profileImage)
            .placeholder(R.drawable.profile_image)
            .into(holder.binding.profile)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MessageActivity::class.java)
            intent.putExtra("name",currentuser.name)
            intent.putExtra("image",currentuser.profileImage)
            intent.putExtra("uid",currentuser.uid)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(itemView: View) :RecyclerView.ViewHolder(itemView){
        val binding : ItemProfileBinding = ItemProfileBinding.bind(itemView)
    }
}