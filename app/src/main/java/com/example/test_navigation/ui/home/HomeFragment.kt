package com.example.test_navigation.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.test_navigation.R
import com.example.test_navigation.databinding.FragmentHomeBinding
import com.example.test_navigation.domain.utils.Constants.Companion.DEG_ROTATE
import com.example.test_navigation.domain.utils.Constants.Companion.DISTANCE_UPDATE
import com.example.test_navigation.domain.utils.Constants.Companion.HALF
import com.example.test_navigation.domain.utils.Constants.Companion.TIME_UPDATE
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var binding: FragmentHomeBinding
    private lateinit var locationManager: LocationManager
    private lateinit var userMarker: Marker
    private lateinit var customMarkerOptions: MarkerOptions
    private var lastLocation: Location? = null
    private var speed: Double = 0.0
    private var dialog: AlertDialog? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getLocation()
            }

            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getLocation()
            }

            else -> {
                showLocationSettingsDialog()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        requestPermissions()
    }

    private fun requestPermissions() {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            && PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            showLocationSettingsDialog()
            return
        }
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        location?.let {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.marker)
            val width = bitmap.width
            val height = bitmap.height
            val x = width / HALF
            val y = height / HALF
            customMarkerOptions = MarkerOptions()
                .position(LatLng(it.latitude, it.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchor(x.toFloat() / width.toFloat(), y.toFloat() / height.toFloat())
            googleMap?.addMarker(customMarkerOptions)?.let { marker -> userMarker = marker }
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(userMarker.position, 15f))
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            TIME_UPDATE,
            DISTANCE_UPDATE,
            locationListener
        )
        googleMap?.isMyLocationEnabled = false
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        googleMap?.setOnMapClickListener { latLng ->
            addMarkerToMap(latLng)
        }
    }

    private fun addMarkerToMap(latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .position(latLng)
        googleMap?.addMarker(markerOptions)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocationUI(location)
            speedDefinition(location)
        }

        override fun onProviderEnabled(provider: String) {
            Toast.makeText(context, resources.getText(R.string.txt_loading), Toast.LENGTH_SHORT).show()
            requestPermissions()
        }

        override fun onProviderDisabled(provider: String) {
            showLocationSettingsDialog()
        }
    }

    private fun updateLocationUI(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        userMarker.position = latLng
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        customMarkerOptions = MarkerOptions().position(latLng)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLng(userMarker.position))
    }

    private fun showLocationSettingsDialog() {
        dialog?.takeIf { it.isShowing }?.apply { dismiss() }
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getText(R.string.dialog_header_err))
            .setMessage(resources.getText(R.string.dialog_message_err))
            .setPositiveButton(resources.getText(R.string.dialog_btn_positive_err)) { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                settingsIntent.data = uri
                startActivity(settingsIntent)
            }
            .setNegativeButton(resources.getText(R.string.dialog_btn_negative_err)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun speedDefinition(location: Location) {
        lastLocation?.let {
            val timeDiff = (location.time - it.time) / 1000.0
            val distance = it.distanceTo(location)
            speed = distance / timeDiff
            lastLocation = location
            if (speed > 3) {
                googleMap?.apply {
                    moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(cameraPosition.target)
                                .zoom(cameraPosition.zoom)
                                .bearing(it.bearing)
                                .tilt(cameraPosition.tilt)
                                .build()
                        )
                    )
                }
            } else {
                val rotation = location.bearing
                userMarker.rotation = (rotation - DEG_ROTATE)
            }

        } ?: kotlin.run { lastLocation = location }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        googleMap?.let { getLocation() }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}