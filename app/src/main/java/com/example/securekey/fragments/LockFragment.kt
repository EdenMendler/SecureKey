package com.example.securekey.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.securekey.R
import com.example.securekey.adapters.LockAdapter
import com.example.securekey.utilities.FaceDetectionManager
import com.example.securekey.utilities.SensorManager
import com.example.securekey.utilities.SpeechRecognitionManager

class LockFragment : Fragment() {

    private lateinit var locksRecyclerView: RecyclerView
    private lateinit var adapter: LockAdapter
    private lateinit var listener: OnAllLocksOpenedListener
    private val lockStates: MutableList<Boolean> = MutableList(6) { false }

    private lateinit var sensorManager: SensorManager
    private lateinit var speechRecognitionManager: SpeechRecognitionManager
    private lateinit var faceDetectionManager: FaceDetectionManager

    private var previewView: PreviewView? = null
    private var detectionStatusText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var lockCheckRunnable: Runnable
    private val CHECK_INTERVAL = 2000L
    private val TAG = "LockFragment"

    interface OnAllLocksOpenedListener {
        fun onAllLocksOpened()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnAllLocksOpenedListener) {
            listener = context
        } else {
            throw RuntimeException("$context חייב ליישם OnAllLocksOpenedListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lock, container, false)

        locksRecyclerView = view.findViewById(R.id.locks_recycler_view)
        locksRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = LockAdapter(lockStates)
        locksRecyclerView.adapter = adapter
        previewView = view.findViewById(R.id.viewFinder)
        detectionStatusText = view.findViewById(R.id.detectionStatus)
        sensorManager = SensorManager(requireContext()) { position, isOpen ->
            updateLockStatus(position, isOpen)
        }

        speechRecognitionManager = SpeechRecognitionManager(requireContext()) { position, isOpen ->
            updateLockStatus(position, isOpen)
        }

        faceDetectionManager = FaceDetectionManager(
            requireContext(),
            previewView
        ) { position, isOpen ->
            updateLockStatus(position, isOpen)
        }
        checkInitialLocks()

        return view
    }

    override fun onResume() {
        super.onResume()
        sensorManager.startSensing()
        speechRecognitionManager.startListening()
        faceDetectionManager.startFaceDetection(lockStates)
        lockCheckRunnable = Runnable {
            checkLocks()
            if (!lockStates.all { it }) {
                handler.postDelayed(lockCheckRunnable, CHECK_INTERVAL)
            }
        }
        handler.postDelayed(lockCheckRunnable, CHECK_INTERVAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.stopSensing()
        speechRecognitionManager.stopListening()
        faceDetectionManager.stopFaceDetection()
        handler.removeCallbacks(lockCheckRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognitionManager.destroy()
        faceDetectionManager.shutdown()
    }

    private fun updateLockStatus(position: Int, isOpen: Boolean) {
        Log.d(TAG, "updateLockStatus called: position=$position, isOpen=$isOpen")
        if (position >= 0 && position < lockStates.size && !lockStates[position]) {
            lockStates[position] = isOpen
            requireActivity().runOnUiThread {
                adapter.notifyItemChanged(position)

                checkAllLocksOpened()
            }
        }
    }

    private fun checkAllLocksOpened() {
        val allOpened = lockStates.all { it }
        Log.d(TAG, "checkAllLocksOpened: allOpened=$allOpened, lockStates=$lockStates")

        if (allOpened) {
            listener.onAllLocksOpened()
        }
    }

    private fun checkInitialLocks() {
        checkBatteryLevel()
        checkWifiConnection()
        checkTimeOfDay()

        if (!sensorManager.hasAccelerometer()) {
            updateLockStatus(2, true)
        }
    }

    private fun checkLocks() {
        checkBatteryLevel()
        checkWifiConnection()
        checkTimeOfDay()
        checkAllLocksOpened()
    }

    private fun checkBatteryLevel() {
        val batteryPct = sensorManager.getBatteryLevel(requireActivity())
        if (batteryPct <= 75) {
            updateLockStatus(0, true)
        }
    }

    private fun checkWifiConnection() {
        if (sensorManager.isWifiConnected(requireContext())) {
            updateLockStatus(3, true)
        }
    }

    private fun checkTimeOfDay() {
        if (sensorManager.isTimeInRange(8, 18)) {
            updateLockStatus(4, true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        speechRecognitionManager.handlePermissionResult(requestCode, permissions, grantResults)
        faceDetectionManager.handlePermissionResult(requestCode, permissions, grantResults)
    }
}