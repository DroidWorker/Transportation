package com.app.transportation.ui

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color.BLUE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.app.transportation.core.collectWithLifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.databinding.FragmentMapBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import io.ktor.utils.io.*


class MapFragment : Fragment() {

    private var binding: FragmentMapBinding? = null
    private val b get() = binding!!
    private lateinit var mapview : MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        mapview = b.mapView as MapView
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        mapview.map.move(
            CameraPosition(Point(55.751574, 37.573856), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0f),
            null
        )
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "Экран"
            b.toolbars.isVisible = true
            window.navigationBarColor = requireContext().getColor(R.color.bottom_nav_color)
        }

        super.onViewCreated(view, savedInstanceState)
        b.search2.clearFocus()

        if (arguments?.getBoolean("needToUpdate") == true) {
            arguments?.putBoolean("needToUpdate", false)
            viewModel.updateMainFragmentData()
        }

        viewModel.getPlcs()
        applyObservers()

        applyListeners()
    }

    @Override
    override fun onStop() {
        b.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        b.mapView.onStart()
    }

    fun getLastKnownLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = PRIORITY_HIGH_ACCURACY

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(requireContext(), "Нет доступа к геолокации!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            b.mapView.map.move(
                                CameraPosition(
                                    Point(location.latitude, location.longitude),
                                    14f,
                                    0f,
                                    0f
                                )
                            )
                            // Создаем метку с координатами местоположения и добавляем ее на картумф
                            val mapObjects = b.mapView.map.mapObjects
                            val placemark = mapObjects.addPlacemark(
                                Point(location.latitude, location.longitude),
                                ImageProvider.fromResource(requireContext(), R.drawable.aim),
                                IconStyle().setScale(0.1f)
                            )
                            placemark.isDraggable = true
                            placemark.isVisible = true
                        }
                    }
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        // Показываем пользователю диалог для включения геолокации
                        exception.startResolutionForResult(requireActivity(), 1)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Toast.makeText(
                            requireContext(),
                            "Не удалось включить геолокацию!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Геолокация отключена на устройстве!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun applyObservers() {
        viewModel.cachedPlaces.collectWithLifecycle(viewLifecycleOwner){
            for ((coords, advert) in it) {
                val point = Point(coords.first.toDouble(), coords.second.toDouble())
                val placemark = b.mapView.map.mapObjects.addPlacemark(point)

                val mapObjectTapListener = MapObjectTapListener { mapObject, point ->
                    /*if (mapObject is Placemark) {
                        val title = mapObject.captionText
                        val description = mapObject.description
                        Toast.makeText(requireContext(), "$title\n$description", Toast.LENGTH_LONG).show()
                    }*/
                    false
                }

                placemark.addTapListener(mapObjectTapListener)
            }
        }
    }

    private fun applyListeners() {

        b.zoomIn.setOnClickListener{
            b.mapView.map.move(CameraPosition(
                b.mapView.map.cameraPosition.target,
                b.mapView.map.cameraPosition.zoom + 1,
                b.mapView.map.cameraPosition.azimuth,
                b.mapView.map.cameraPosition.tilt
            ), Animation(Animation.Type.SMOOTH, 1f), null)
        }

        b.zoomOut.setOnClickListener{
            b.mapView.map.move(CameraPosition(
                b.mapView.map.cameraPosition.target,
                b.mapView.map.cameraPosition.zoom - 1,
                b.mapView.map.cameraPosition.azimuth,
                b.mapView.map.cameraPosition.tilt
            ), Animation(Animation.Type.SMOOTH, 1f), null)
        }

        b.mapFilter.setOnClickListener {

        }

        b.myLocation.setOnClickListener {
            getLastKnownLocation()
        }

        b.search2.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_DONE){
                if (b.search2.text.length<2){
                    Snackbar.make(b.search2, "минимальная длина запроса 2 символа!", Snackbar.LENGTH_LONG).show()
                    return@OnEditorActionListener true
                }
                //viewModel.getSearchResult("ер")
                /*findNavController().navigate(R.id.advertisementsFragment,
                    bundleOf("categoryId" to lastCheckedCategoryId, "type" to 3, "searchText" to b.search.text.toString()))
                b.search.setText("")*/
                true
            } else {
                false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}