package com.example.securekey

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.securekey.fragments.LockFragment

class MainActivity : AppCompatActivity(), LockFragment.OnAllLocksOpenedListener {

    private var permissionToRecordAccepted = false
    private var permissionToCameraAccepted = false
    private val permissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )
    private val REQUEST_PERMISSIONS = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS)

        if (savedInstanceState == null) {
            val lockFragment = LockFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, lockFragment)
                .commit()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            permissionToRecordAccepted = grantResults.size > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionToCameraAccepted = grantResults.size > 1 &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED

            if (!permissionToRecordAccepted) {
                Toast.makeText(this, "Microphone permission is required to identify the song.", Toast.LENGTH_LONG).show()
            }

            if (!permissionToCameraAccepted) {
                Toast.makeText(this, "Camera permission is required for one of the locks.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onAllLocksOpened() {
        Toast.makeText(this, "You did it! Well done!", Toast.LENGTH_LONG).show()
    }
}