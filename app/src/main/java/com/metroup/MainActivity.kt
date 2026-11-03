package com.metroup

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.abs

class MainActivity : ComponentActivity(), LocationListener {
    private var latestFix by mutableStateOf<GpsFix?>(null)
    private var nearby by mutableStateOf<StationMatch?>(null)
    private var selectedRouteId by mutableStateOf("M1A")
    private val selectedRoute get() = Routes.get(selectedRouteId)
    private var nearbyMatcher = StationMatcher()
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startGps() }
    override fun onResume() { super.onResume(); startGps() }
    override fun onPause() {
        getSystemService(LocationManager::class.java).removeUpdates(this)
        super.onPause()
    }
    private fun startGps() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        runCatching { getSystemService(LocationManager::class.java).requestLocationUpdates(LocationManager.GPS_PROVIDER, 5_000L, 0f, this) }
        val j = Store(this).load()
        if (j != null && !j.ringing) runCatching { startForegroundService(Intent(this, TrackingService::class.java)) }
    }
    override fun onLocationChanged(location: Location) {
        if (!location.hasAccuracy()) { nearby = null; latestFix = null; return }
        val fix = GpsFix(GeoPoint(location.latitude, location.longitude), location.accuracy.toDouble(), location.elapsedRealtimeNanos / 1_000_000)
        latestFix = fix
        val now = SystemClock.elapsedRealtime()
        val match = nearbyMatcher.accept(fix, now, selectedRoute.stations.indices.toList())
        if (nearbyMatcher.nearest(fix, now) == null) nearby = null
        else if (match != null) nearby = match
    }
    @Deprecated("Legacy location callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) { nearby = null; latestFix = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store(this).load()?.let { selectedRouteId = it.routeId; nearbyMatcher = StationMatcher(it.route) }
        setContent { MetroScreen() }
    }
    private fun notificationsAllowed(): Boolean = getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    private fun requestPermissions() {
        permissions.launch(buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray())
    }
    @Composable
    private fun MetroScreen() {
        val store = remember { Store(this) }
        var journey by remember { mutableStateOf(store.load()) }
        val route = journey?.route ?: selectedRoute
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        var start by remember { mutableIntStateOf(if (selectedRoute.id == "M1A") 8 else 0) }
        var dest by remember { mutableIntStateOf(if (selectedRoute.id == "M1A") 0 else selectedRoute.stations.lastIndex) }
        var before by remember { mutableIntStateOf(if (selectedRoute.id == "M2_SHUTTLE") 1 else 2) }
        var seconds by remember { mutableIntStateOf(120) }
        var error by remember { mutableStateOf<String?>(null) }
        var confirm by remember { mutableIntStateOf(if (selectedRoute.id == "M1A") 8 else 0) }
        var exact by remember { mutableStateOf(Alarms.allowed(this)) }
        var notify by remember { mutableStateOf(notificationsAllowed()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = SystemClock.elapsedRealtime(); journey = store.load()
                exact = Alarms.allowed(this@MainActivity); notify = notificationsAllowed()
                delay(1000)
            }
        }
        MaterialTheme(colorScheme = darkColorScheme(
            primary = Color(0xFFB6F36A), onPrimary = Color(0xFF182709),
            background = Color(0xFF10171B), surface = Color(0xFF1C272D),
            onSurface = Color(0xFFF0F4F1), onBackground = Color(0xFFF0F4F1)
        )) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("metroUP", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("İSTANBUL / ${route.code}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                    Text(if (journey == null) "Yolun akışına\nbiraz mola ver." else if (journey!!.ringing) "Uyanma zamanı." else "Biraz dinlen.\nAlarmın kurulu.", fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
                    Text("Durağını seç. Alarmını kur. Yolculuğuna ara ver.", color = Color(0xFFABB9BF))
                    val j = journey
                    if (j == null) {
                        Panel {
                            Text("01  YOLCULUĞUN", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            RoutePicker(route) { selected ->
                                selectedRouteId = selected.id
                                nearby = null; nearbyMatcher = StationMatcher(selected)
                                start = 0; dest = selected.stations.lastIndex
                                before = if (selected.stations.size == 2) 1 else 2
                                confirm = 0; error = null
                            }
                            if (route.id == "M2_SHUTTLE") Text("Seyrantepe kolu: aktarmayı Sanayi Mahallesi'nde yap. Tek aralıkta 1 durak önce alarm başlangıçta çalar.", fontSize = 12.sp)
                            val detected = nearby?.takeIf { now - it.elapsed in 0..30_000L }
                            if (detected != null) {
                                Text("GPS yakını: ${route.stations[detected.station]} • ${detected.distanceMeters.toInt()} m", color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { start = detected.station }) { Text("Biniş durağı olarak seç") }
                            } else Text("GPS: ${route.code} durak yakınlığı doğrulanmadan alarm başlatılamaz.", fontSize = 12.sp)
                            StationPicker(route, "Biniş durağı", start, route.stations.indices.toList()) { start = it }
                            TextButton(onClick = { val old = start; start = dest; dest = old }) { Text("↑↓ Yönü değiştir") }
                            StationPicker(route, "Varış durağı", dest, route.stations.indices.toList()) { dest = it }
                        }
                        Panel {
                            Text("02  BENİ NE ZAMAN UYANDIR?", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..3).forEach { n -> FilterChip(modifier = Modifier.weight(1f), selected = before == n, onClick = { before = n }, label = { Text("$n durak\nönce") }) }
                            }
                            Text("Durak arası ortalama: ${seconds / 60} dk ${seconds % 60} sn")
                            Slider(value = seconds.toFloat(), onValueChange = { seconds = (it / 15).toInt() * 15 }, valueRange = 60f..300f, steps = 15)
                            val count = abs(dest - start)
                            Text(if (count == before && count > 0) "Alarm binişte hemen çalacak." else if (count > before) "${count} durak aralığı • Alarm yaklaşık ${(count - before) * seconds / 60} dk sonra" else "Daha uzak bir varış veya daha az durak önce uyarı seç.", color = Color(0xFFABB9BF))
                        }
                    } else {
                        Panel {
                            Text(if (j.ringing) "VARIŞ ALARMI" else "ALARM İÇİN KALAN TAHMİNİ SÜRE", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            val remaining = ((j.alarmElapsed - now).coerceAtLeast(0) / 1000)
                            Text(if (j.ringing) "Uyan!" else "%02d:%02d".format(remaining / 60, remaining % 60), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                            Text("${route.stations[j.start]} → ${route.stations[j.destination]}")
                            Text("${j.before} durak önce: ${route.stations[j.alarmStation]}", color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider()
                            val gpsFresh = j.source.startsWith("GPS") && now - j.anchorElapsed in 0..30_000L
                            Text(if (gpsFresh) "GPS yakını: ${route.stations[j.anchor]}" else "Süre tahmini: ${route.stations[j.estimatedStation(now)]}")
                            Text("Son dayanak: ${j.source} • ${route.stations[j.anchor]}", fontSize = 12.sp)
                            Text("Konum doğrulanmazsa süreyle ilerler. Gecikmeler tahmini değiştirebilir.", color = Color(0xFFABB9BF), fontSize = 12.sp)
                        }
                        if (!j.ringing) Panel {
                            Text("DURAKTAYSAN TAHMİNİ DÜZELT", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            StationPicker(route, "Şu anda bulunduğum durak", confirm.coerceIn(minOf(j.start, j.destination), maxOf(j.start, j.destination)), (minOf(j.start,j.destination)..maxOf(j.start,j.destination)).toList()) { confirm = it }
                            OutlinedButton(onClick = {
                                runCatching {
                                    val station = confirm.coerceIn(minOf(j.start,j.destination), maxOf(j.start,j.destination))
                                    val updated = j.reanchor(station, SystemClock.elapsedRealtime(), "Elle doğrulandı")
                                    Alarms.schedule(this@MainActivity, updated); store.save(updated); journey = updated
                                }.onFailure { error = it.message }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Buradayım • süreyi güncelle") }
                            Text("GPS gelirse resmi durak koordinatlarına göre alarm otomatik güncellenir.", fontSize = 12.sp, color = Color(0xFFABB9BF))
                        }
                    }
                    if (!exact || !notify) Panel {
                        Text("Alarmı hazırla", fontWeight = FontWeight.Bold)
                        if (!notify) TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33) requestPermissions()
                            else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                        }) { Text("Bildirim iznini aç") }
                        if (!exact && Build.VERSION.SDK_INT >= 31) TextButton(onClick = { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }) { Text("Kesin alarm iznini aç") }
                    }
                    if (j == null) {
                        val problem = BoardingGuard.problem(latestFix, nearby, start, now,
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
                            getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER), route)
                        problem?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = {
                        if (j != null) { Alarms.stop(this@MainActivity); journey = null }
                        else runCatching {
                            check(exact && notify) { "Önce kesin alarm ve bildirim izinlerini aç." }
                            val locationProblem = BoardingGuard.problem(latestFix, nearby, start, SystemClock.elapsedRealtime(),
                                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
                                getSystemService(LocationManager::class.java).isProviderEnabled(LocationManager.GPS_PROVIDER), route)
                            check(locationProblem == null) { locationProblem ?: "Konum doğrulanamadı." }
                            val created = Journey.create(start, dest, before, seconds, SystemClock.elapsedRealtime(), route).copy(source = "GPS biniş doğrulaması")
                            store.save(created)
                            try { Alarms.schedule(this@MainActivity, created) } catch (e: Exception) { store.clear(); throw e }
                            journey = created; confirm = start; error = null
                            try { startForegroundService(Intent(this@MainActivity, TrackingService::class.java)) }
                            catch (e: Exception) { Alarms.stop(this@MainActivity); journey = null; throw e }
                        }.onFailure { error = it.message }
                    }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(18.dp), enabled = j != null || (abs(dest-start) >= before && start != dest && exact && notify)) {
                        Text(if (j == null) "Yolculuğu başlat →" else if (j.ringing) "Uyandım • alarmı kapat" else "Yolculuğu bitir", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    if (j == null) TextButton(onClick = { requestPermissions() }) { Text("Konum ve bildirim izinlerini aç") }
                    Text("ÇEVRİMDIŞI HAZIR / HESAP GEREKTİRMEZ", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    Text("Süreler tahminidir. Alarm sesini açık tut. Telefon kapanırsa, yeniden başlarsa veya uygulamayı zorla durdurursan yolculuğu yeniden başlat. İlk kullanımda ekran kilitliyken alarmı dene.", color = Color(0xFFABB9BF), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun StationPicker(route: MetroRoute, label: String, selected: Int, indices: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember(route.id) { mutableStateOf(false) }
    Column {
        Text(label, color = Color(0xFFABB9BF), fontSize = 12.sp)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(route.stations[selected], modifier = Modifier.weight(1f)); Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                indices.forEach { index -> DropdownMenuItem(text = { Text(route.stations[index]) }, onClick = { onSelect(index); expanded = false }) }
            }
        }
    }
}

@Composable
private fun RoutePicker(route: MetroRoute, onSelect: (MetroRoute) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(route.label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Routes.all.forEach { item -> DropdownMenuItem(text = { Text(item.label) }, onClick = { expanded = false; if (item.id != route.id) onSelect(item) }) }
        }
    }
}
