package com.metroup

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.webkit.WebViewAssetLoader
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

private fun point(p: GeoPoint) = JSONArray().put(p.latitude).put(p.longitude)
private fun RouteMapState.json(now: Long): String {
    val stations = JSONArray()
    route.stations.forEachIndexed { index, name -> stations.put(JSONObject().put("name",name).put("point",point(route.points[index]))) }
    val m = measured?.let { JSONObject().put("point",point(it.point)).put("accuracy",it.accuracyMeters.toInt())
        .put("age",(now-it.elapsed).coerceAtLeast(0)/1000).put("live",live) }
    return JSONObject().put("routeId",route.id).put("code",route.code).put("stations",stations)
        .put("color",if(route.code=="M2") "#36b68d" else "#ef6269")
        .put("start",start).put("destination",destination).put("alarm",alarmStation ?: JSONObject.NULL)
        .put("measured",m ?: JSONObject.NULL).put("estimated",estimated?.let(::point) ?: JSONObject.NULL).toString()
}

@Composable
fun RouteMap(state: RouteMapState, now: Long) {
    var expanded by remember { mutableStateOf(false) }
    val json = state.json(now)
    @Composable fun MapContent(modifier: Modifier) {
        var failed by remember { mutableStateOf(false) }
        var offline by remember { mutableStateOf(false) }
        var retry by remember { mutableIntStateOf(0) }
        Column(modifier) {
            if (failed || offline) {
                Text(if(failed) "Sokak haritası başlatılamadı. Çevrimdışı hat görünümü açık." else "Çevrimdışı hat görünümü", fontSize=12.sp)
                TextButton(onClick = { failed=false; offline=false; retry++ }) { Text("Sokak haritasını aç / yeniden dene") }
                OfflineRouteMap(state, Modifier.fillMaxWidth().weight(1f))
            } else key(retry) {
                AndroidView(factory = { RouteMapView(it) { failed = true } },
                    modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)),
                    update = { it.render(json) }, onRelease = { it.releaseMap() })
                TextButton(onClick = { offline=true }) { Text("Çevrimdışı hat görünümüne geç") }
            }
        }
    }
    @Composable fun Legend() {
        Text("● Mavi: son ölçüm   ◆ Turuncu: tahmini ilerleme", fontSize = 11.sp)
        Text("V: varış   !: alarm durağı   Gri: eski ölçüm", fontSize = 11.sp)
        Text(state.status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        Text("Sokak zemini internet kullanır; hat katmanı çevrimdışıdır.", fontSize = 11.sp)
        Text("Hat çizgisi yaklaşık; gerçek ray geometrisi değildir.", fontSize = 11.sp)
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("YOLCULUK HARİTASI", modifier = Modifier.padding(top=12.dp), fontSize = 12.sp)
                TextButton(onClick = { expanded = true }) { Text("Büyüt ↗") }
            }
            if (!expanded) MapContent(Modifier.fillMaxWidth().height(340.dp))
            Legend()
        }
    }
    if (expanded) Dialog(onDismissRequest = { expanded = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { expanded = false }) { Text("← Haritayı küçült • ${state.route.label}") }
                MapContent(Modifier.fillMaxWidth().weight(1f))
                Legend()
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
private class RouteMapView(context: Context, private val failed: () -> Unit) : WebView(context) {
    private var released=false
    private var painted=false
    private val watchdog=Runnable { if(!released&&!painted) failed() }
    private var ready = false
    private var pending = ""
    private val loader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context)).build()
    init {
        // JavaScript is limited to bundled Leaflet/map assets. No native JS bridge,
        // file access, geolocation access, remote scripts or arbitrary navigation.
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.domStorageEnabled = false
        settings.setGeolocationEnabled(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.userAgentString = settings.userAgentString + " MetroUp/0.4.1 (+https://github.com/beratcaliskan/metroup)"
        setBackgroundColor(android.graphics.Color.rgb(23,39,47))
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                if(message.messageLevel()==ConsoleMessage.MessageLevel.ERROR)
                    android.util.Log.e("MetroUpMap", "Map script error at ${message.sourceId()}:${message.lineNumber()}: ${message.message()}")
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val uri = request.url
                if (uri.scheme=="https" && uri.host=="appassets.androidplatform.net" && uri.path.orEmpty().startsWith("/assets/map/"))
                    return loader.shouldInterceptRequest(uri)
                if (uri.scheme=="https" && uri.host=="tile.openstreetmap.org" && uri.port==-1 &&
                    Regex("/\\d+/\\d+/\\d+\\.png").matches(uri.path.orEmpty())) return null
                return WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream(byteArrayOf()))
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.isForMainFrame && request.url.toString()=="https://www.openstreetmap.org/copyright")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.openstreetmap.org/copyright"))) }
                return true
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if(request.isForMainFrame && !released) post { if(!released) failed() }
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (!released && url=="https://appassets.androidplatform.net/assets/map/index.html") { ready=true; render(pending) }
            }
        }
        setOnTouchListener { view, event ->
            view.parent?.requestDisallowInterceptTouchEvent(event.actionMasked!=MotionEvent.ACTION_UP && event.actionMasked!=MotionEvent.ACTION_CANCEL)
            false
        }
        postDelayed(watchdog,10_000)
        loadUrl("https://appassets.androidplatform.net/assets/map/index.html")
    }
    fun releaseMap() {
        released=true; removeCallbacks(watchdog)
        ready=false; pending=""
        webViewClient=WebViewClient()
        stopLoading(); destroy()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        post { if(!released) render(pending) }
    }
    fun render(json: String) {
        if(released) return
        pending=json
        if (ready && json.isNotEmpty() && width>0 && height>0) {
            val density=resources.displayMetrics.density
            val w=kotlin.math.round(width/density).toInt()
            val h=kotlin.math.round(height/density).toInt()
            evaluateJavascript("typeof window.metroFrame==='function' && window.metroFrame($json,$w,$h);" ) { result ->
                if(!released && result=="true") { painted=true; removeCallbacks(watchdog) }
            }
        }
    }
}
