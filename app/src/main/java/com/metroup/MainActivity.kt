package com.metroup

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
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

class MainActivity : ComponentActivity() {
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    private fun learn(station: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val gps = runCatching { getSystemService(LocationManager::class.java).getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull() ?: return
        val age = SystemClock.elapsedRealtimeNanos() - gps.elapsedRealtimeNanos
        if (age in 0..15_000_000_000L && gps.hasAccuracy() && gps.accuracy <= 40) Store(this).learn(station, gps.latitude, gps.longitude)
    }

    @Composable
    private fun MetroScreen() {
        val store = remember { Store(this) }
        var journey by remember { mutableStateOf(store.load()) }
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        var start by remember { mutableIntStateOf(8) }
        var dest by remember { mutableIntStateOf(0) }
        var before by remember { mutableIntStateOf(2) }
        var seconds by remember { mutableIntStateOf(120) }
        var error by remember { mutableStateOf<String?>(null) }
        var confirm by remember { mutableIntStateOf(8) }
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
                        Text("İSTANBUL / M1A", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                    Text(if (journey == null) "Yolun akışına\nbiraz mola ver." else if (journey!!.ringing) "Uyanma zamanı." else "Biraz dinlen.\nAlarmın kurulu.", fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
                    Text("Durağını seç. Alarmını kur. Yolculuğuna ara ver.", color = Color(0xFFABB9BF))
                    val j = journey
                    if (j == null) {
                        Panel {
                            Text("01  YOLCULUĞUN", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            StationPicker("Biniş durağı", start, M1A.stations.indices.toList()) { start = it }
                            TextButton(onClick = { val old = start; start = dest; dest = old }) { Text("↑↓ Yönü değiştir") }
                            StationPicker("Varış durağı", dest, M1A.stations.indices.toList()) { dest = it }
                        }
                        Panel {
                            Text("02  BENİ NE ZAMAN UYANDIR?", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..3).forEach { n -> FilterChip(modifier = Modifier.weight(1f), selected = before == n, onClick = { before = n }, label = { Text("$n durak\nönce") }) }
                            }
                            Text("Durak arası ortalama: ${seconds / 60} dk ${seconds % 60} sn")
                            Slider(value = seconds.toFloat(), onValueChange = { seconds = (it / 15).toInt() * 15 }, valueRange = 60f..300f, steps = 15)
                            val count = abs(dest - start)
                            Text(if (count > before) "${count} durak aralığı • Alarm yaklaşık ${(count - before) * seconds / 60} dk sonra" else "Daha uzak bir varış seç; alarm binişten sonra olmalı.", color = Color(0xFFABB9BF))
                        }
                    } else {
                        Panel {
                            Text(if (j.ringing) "VARIŞ ALARMI" else "ALARM İÇİN KALAN TAHMİNİ SÜRE", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            val remaining = ((j.alarmElapsed - now).coerceAtLeast(0) / 1000)
                            Text(if (j.ringing) "Uyan!" else "%02d:%02d".format(remaining / 60, remaining % 60), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                            Text("${M1A.stations[j.start]} → ${M1A.stations[j.destination]}")
                            Text("${j.before} durak önce: ${M1A.stations[j.alarmStation]}", color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider()
                            Text("Tahmini durak: ${M1A.stations[j.estimatedStation(now)]}")
                            Text("Son dayanak: ${j.source} • ${M1A.stations[j.anchor]}", fontSize = 12.sp)
                            Text("Konum doğrulanmazsa süreyle ilerler. Gecikmeler tahmini değiştirebilir.", color = Color(0xFFABB9BF), fontSize = 12.sp)
                        }
                        if (!j.ringing) Panel {
                            Text("DURAKTAYSAN TAHMİNİ DÜZELT", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            StationPicker("Şu anda bulunduğum durak", confirm.coerceIn(minOf(j.start, j.destination), maxOf(j.start, j.destination)), (minOf(j.start,j.destination)..maxOf(j.start,j.destination)).toList()) { confirm = it }
                            OutlinedButton(onClick = {
                                runCatching {
                                    val station = confirm.coerceIn(minOf(j.start,j.destination), maxOf(j.start,j.destination))
                                    val updated = j.reanchor(station, SystemClock.elapsedRealtime(), "Elle doğrulandı")
                                    Alarms.schedule(this@MainActivity, updated); store.save(updated); learn(station); journey = updated
                                }.onFailure { error = it.message }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Buradayım • süreyi güncelle") }
                            Text("İyi GPS sinyali varsa bu durağın konumu yalnızca telefona kaydedilir.", fontSize = 12.sp, color = Color(0xFFABB9BF))
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
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = {
                        if (j != null) { Alarms.stop(this@MainActivity); journey = null }
                        else runCatching {
                            check(exact && notify) { "Önce kesin alarm ve bildirim izinlerini aç." }
                            val created = Journey.create(start, dest, before, seconds, SystemClock.elapsedRealtime())
                            store.save(created)
                            try { Alarms.schedule(this@MainActivity, created) } catch (e: Exception) { store.clear(); throw e }
                            journey = created; confirm = start; learn(start); error = null
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                                startForegroundService(Intent(this@MainActivity, TrackingService::class.java))
                        }.onFailure { error = it.message }
                    }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(18.dp), enabled = j != null || (abs(dest-start) > before && exact && notify)) {
                        Text(if (j == null) "Yolculuğu başlat →" else if (j.ringing) "Uyandım • alarmı kapat" else "Yolculuğu bitir", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    if (j == null) TextButton(onClick = { requestPermissions() }) { Text("İsteğe bağlı GPS ve bildirim izinleri") }
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
private fun StationPicker(label: String, selected: Int, indices: List<Int>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, color = Color(0xFFABB9BF), fontSize = 12.sp)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(M1A.stations[selected], modifier = Modifier.weight(1f)); Text("⌄")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                indices.forEach { index -> DropdownMenuItem(text = { Text(M1A.stations[index]) }, onClick = { onSelect(index); expanded = false }) }
            }
        }
    }
}
