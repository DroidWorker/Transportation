package com.app.transportation.ui

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.transportation.MainActivity
import com.app.transportation.R
import com.app.transportation.core.collectWithLifecycle
import com.app.transportation.data.database.entities.Advert
import com.app.transportation.databinding.FragmentMapBinding
import com.app.transportation.ui.adapters.CreateOrderCategorySelectorAdapter
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
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider


class MapFragment : Fragment() {

    private var binding: FragmentMapBinding? = null
    private val b get() = binding!!
    private lateinit var mapview : MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var advs : List<Advert> = emptyList()

    private val adapter by lazy { CreateOrderCategorySelectorAdapter() }

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
        adapter.mode = 1
        b.categoriesView.adapter = adapter
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.apply {
            b.title.text = "КАРТА"
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
        val listener : MapObjectTapListener = MapObjectTapListener(){ mapObject, point ->
            if(mapObject is PlacemarkMapObject) {
                val advert = advs.find {
                    it.id==mapObject.userData
                }
                val dialogFragment = MapDialogFragment()
                if(advert!=null) {
                    val bundle = Bundle()
                    bundle.putInt("id", advert.categoryId)
                    bundle.putString("title", advert.title)
                    bundle.putString("price", advert.price)
                    if (advert.profile.isNotEmpty()) bundle.putString(
                        "namephone",
                        advert.profile.first().firstName + "\n " + advert.profile.first().phone
                    )
                    bundle.putString("photo", advert.photo.firstOrNull())
                    dialogFragment.arguments = bundle
                }
                dialogFragment.show(requireFragmentManager(), "mapDialog")
            }
            true
        }
        viewModel.cachedPlaces.collectWithLifecycle(viewLifecycleOwner){
            advs = it.values.toList()
            for ((coords, advert) in it) {
                val point = Point(coords.first.toDouble(), coords.second.toDouble())
                val placemark = b.mapView.map.mapObjects.addPlacemark(point)
                placemark.userData = advert.id
                placemark.setIconStyle(IconStyle().setScale(1.7f))
                placemark.addTapListener(listener)
            }
        }
        viewModel.addAdvertScreenCategoriesFlowAll().collectWithLifecycle(viewLifecycleOwner) {
            adapter.submitList(it)
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
            b.categoriesView.visibility = View.VISIBLE
        }

        adapter.onClick={p1, p2->

            b.categoriesView.visibility = View.GONE
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
                //clear points
                val mapObjects = b.mapView.map.mapObjects
                mapObjects.clear()
                val filteredPlaces = viewModel.cachedPlaces.value.filterValues { advert -> advert.title.contains(b.search2.text.toString()) }
                viewModel.cachedPlaces.tryEmit(filteredPlaces)
                true
            } else {
                false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        b.search2.setText("")
        binding = null
    }

}