package fr.vlegall.nanoorbitapplication.presentation.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.vlegall.nanoorbitapplication.NanoOrbitApplication
import fr.vlegall.nanoorbitapplication.domain.model.StatutStation
import fr.vlegall.nanoorbitapplication.domain.model.StationSol
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(
        factory = MapViewModel.provideFactory(
            LocalContext.current.applicationContext as NanoOrbitApplication
        )
    )
) {
    val stations by viewModel.stations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var myLocationOverlay by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(stations, mapView) {
        val mv = mapView ?: return@LaunchedEffect
        mv.overlays.removeAll { it is Marker }
        stations.forEach { station ->
            addStationMarker(mv, station, userLocation)
        }
        mv.invalidate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stations sol",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    myLocationOverlay?.myLocation?.let { loc ->
                        mapView?.controller?.animateTo(loc)
                        viewModel.onUserLocationUpdated(loc.latitude, loc.longitude)
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Me localiser")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        isHorizontalMapRepetitionEnabled = false
                        isVerticalMapRepetitionEnabled = false
                        minZoomLevel = 3.0
                        maxZoomLevel = 18.0
                        setScrollableAreaLimitLatitude(
                            MapView.getTileSystem().maxLatitude,
                            MapView.getTileSystem().minLatitude,
                            0
                        )
                        setScrollableAreaLimitLongitude(
                            MapView.getTileSystem().minLongitude,
                            MapView.getTileSystem().maxLongitude,
                            0
                        )
                        controller.setZoom(3.0)
                        controller.setCenter(GeoPoint(20.0, 0.0))

                        val locationOverlay = MyLocationNewOverlay(
                            GpsMyLocationProvider(ctx), this
                        ).apply { enableMyLocation() }
                        overlays.add(locationOverlay)
                        myLocationOverlay = locationOverlay
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView?.onDetach() }
    }
}

private fun addStationMarker(
    mapView: MapView,
    station: StationSol,
    userLocation: Pair<Double, Double>?
) {
    val markerColor = when (station.statut) {
        StatutStation.ACTIVE      -> android.graphics.Color.parseColor("#69F0AE")
        StatutStation.MAINTENANCE -> android.graphics.Color.parseColor("#FFB300")
        StatutStation.INACTIVE    -> android.graphics.Color.parseColor("#546E7A")
    }
    Marker(mapView).apply {
        position = GeoPoint(station.latitude, station.longitude)
        title = station.nomStation
        icon = createColoredMarkerBitmap(mapView.context, markerColor)
        subDescription = buildString {
            append("Bande : ${station.bandeFrequence ?: "N/A"}")
            station.debitMax?.let { append("\nDébit max : $it Mbps") }
            userLocation?.let { (lat, lon) ->
                val distance = calculateDistance(lat, lon, station.latitude, station.longitude)
                append("\nDistance : ${distance.toInt()} km")
            }
            append("\nStatut : ${station.statut.label}")
        }
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        mapView.overlays.add(this)
    }
}

private fun createColoredMarkerBitmap(context: android.content.Context, color: Int): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (44 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = size / 2f - 2f * density
    val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }
    canvas.drawCircle(size / 2f, size / 2f, radius, paintFill)
    canvas.drawCircle(size / 2f, size / 2f, radius, paintStroke)
    return BitmapDrawable(context.resources, bitmap)
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0] / 1000.0
}