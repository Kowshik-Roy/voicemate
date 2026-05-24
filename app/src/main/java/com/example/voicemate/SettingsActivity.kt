package com.example.voicemate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.voicemate.adapters.AppAdapter
import com.example.voicemate.helpers.AppHelper

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupAppPermissionsList()
    }

    private fun setupAppPermissionsList() {
        val rvAppPermissions = findViewById<RecyclerView>(R.id.rvAppPermissions)
        val installedApps = AppHelper.getAllInstalledApps(this)
        
        val adapter = AppAdapter(installedApps)
        rvAppPermissions.layoutManager = LinearLayoutManager(this)
        rvAppPermissions.adapter = adapter
    }
}
