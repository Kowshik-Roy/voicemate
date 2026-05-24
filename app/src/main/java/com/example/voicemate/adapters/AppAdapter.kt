package com.example.voicemate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.voicemate.R
import com.example.voicemate.helpers.AppHelper
import com.example.voicemate.helpers.AppPreferenceHelper

class AppAdapter(private val apps: List<AppHelper.AppInfo>) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val swPermission: SwitchCompat = view.findViewById(R.id.swAppPermission)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_permission, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.tvName.text = app.name
        holder.imgIcon.setImageDrawable(app.icon)
        
        // Reset listener to avoid triggering it while setting the initial state
        holder.swPermission.setOnCheckedChangeListener(null)
        holder.swPermission.isChecked = AppPreferenceHelper.isAppAllowed(holder.itemView.context, app.packageName)
        
        holder.swPermission.setOnCheckedChangeListener { _, isChecked ->
            AppPreferenceHelper.setAppPermission(holder.itemView.context, app.packageName, isChecked)
        }
    }

    override fun getItemCount() = apps.size
}
