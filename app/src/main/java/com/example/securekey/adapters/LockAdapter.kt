package com.example.securekey.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.securekey.R

class LockAdapter(private val lockStates: List<Boolean>) :
    RecyclerView.Adapter<LockAdapter.LockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_lock, parent, false)
        return LockViewHolder(view)
    }

    override fun onBindViewHolder(holder: LockViewHolder, position: Int) {
        val isOpen = lockStates[position]
        if (isOpen) {
            holder.lockClosedIcon.visibility = View.INVISIBLE
            holder.lockOpenIcon.visibility = View.VISIBLE
        } else {
            holder.lockClosedIcon.visibility = View.VISIBLE
            holder.lockOpenIcon.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = lockStates.size

    inner class LockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lockClosedIcon: ImageView = itemView.findViewById(R.id.lock_closed_icon)
        val lockOpenIcon: ImageView = itemView.findViewById(R.id.lock_open_icon)
    }
}