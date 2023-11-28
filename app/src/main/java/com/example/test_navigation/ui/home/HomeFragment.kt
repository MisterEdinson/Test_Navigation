package com.example.test_navigation.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.test_navigation.R
import com.example.test_navigation.databinding.FragmentHomeBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var binding: FragmentHomeBinding
    private lateinit var locationManager: LocationManager
    private lateinit var userMarker: Marker
    private lateinit var customMarkerOptions: MarkerOptions

    @SuppressLint("MissingInflatedId")
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
            return
        }
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        location?.let {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.marker)
            val width = bitmap.width
            val height = bitmap.height
            val x = width / 2
            val y = height / 2
            customMarkerOptions = MarkerOptions()
                .position(LatLng(it.latitude, it.longitude))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .anchor(x.toFloat() / width.toFloat(), y.toFloat() / height.toFloat())
            userMarker = googleMap.addMarker(customMarkerOptions)!!
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userMarker.position, 15f))
        }
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1f,
            locationListener
        )
        googleMap.isMyLocationEnabled = false
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        googleMap.setOnMapClickListener { latLng ->
            addMarkerToMap(latLng)
        }
    }

    private fun addMarkerToMap(latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .position(latLng)
        googleMap.addMarker(markerOptions)
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            updateLocationUI(location)
        }

        override fun onProviderEnabled(provider: String) {
            Toast.makeText(context, "Загрузка данных...", Toast.LENGTH_SHORT).show()
        }

        override fun onProviderDisabled(provider: String) {
            showLocationSettingsDialog()
        }
    }
    private fun updateLocationUI(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        userMarker.position = latLng
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        customMarkerOptions = MarkerOptions().position(latLng)
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(userMarker.position))
        val rotation = location.bearing
        userMarker.rotation = (rotation-45)
        Toast.makeText(context, rotation.toString(), Toast.LENGTH_SHORT).show()
    }
    private fun showLocationSettingsDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Данные о местоположении отствуют")
            .setMessage("Пожалуйста разрешите доступ к Вашему местоположению для нормальной работы приложения.")
            .setPositiveButton("Настройки") { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                settingsIntent.data = uri
                startActivity(settingsIntent)
            }
            .setNegativeButton("Отменить") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
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