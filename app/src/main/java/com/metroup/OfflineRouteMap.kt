package com.metroup

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min

data class MapXY(val x: Float, val y: Float)
class RouteMapProjection(points: List<GeoPoint>, private val width: Float, private val height: Float, padding: Float) {
    private val factor=cos(Math.toRadians(points.map { it.latitude }.average()))
    private val minX=points.minOf { it.longitude*factor }
    private val maxX=points.maxOf { it.longitude*factor }
    private val minY=points.minOf { -it.latitude }
    private val maxY=points.maxOf { -it.latitude }
    private val spanX=(maxX-minX).coerceAtLeast(0.000001)
    private val spanY=(maxY-minY).coerceAtLeast(0.000001)
    private val scale=min((width-2*padding).coerceAtLeast(1f)/spanX,(height-2*padding).coerceAtLeast(1f)/spanY)
    fun project(point: GeoPoint)=MapXY(
        ((point.longitude*factor-(minX+maxX)/2)*scale+width/2).toFloat(),
        ((-point.latitude-(minY+maxY)/2)*scale+height/2).toFloat())
    fun radius(meters: Double)=(meters/111195*scale).toFloat()
}

@Composable
fun OfflineRouteMap(state: RouteMapState, modifier: Modifier=Modifier) {
    Canvas(modifier.background(Color(0xFF17272F)).clipToBounds()) {
        val projection=RouteMapProjection(state.route.points,size.width,size.height,32.dp.toPx())
        fun at(point: GeoPoint)=projection.project(point).let { Offset(it.x,it.y) }
        val tint=if(state.route.code=="M2")Color(0xFF36B68D) else Color(0xFFEF6269)
        val points=state.route.points.map(::at)
        points.zipWithNext().forEach { (a,b) -> drawLine(tint,a,b,4.dp.toPx()) }
        points.forEach { drawCircle(Color.White,5.dp.toPx(),it); drawCircle(tint,3.dp.toPx(),it) }
        state.alarmStation?.let { drawCircle(Color(0xFFED99DC),9.dp.toPx(),points[it]) }
        drawCircle(Color(0xFFB6F36A),10.dp.toPx(),points[state.destination])
        state.estimated?.let {
            val p=at(it);val r=8.dp.toPx()
            val diamond=Path().apply { moveTo(p.x,p.y-r);lineTo(p.x+r,p.y);lineTo(p.x,p.y+r);lineTo(p.x-r,p.y);close() }
            drawPath(diamond,Color(0xFFFFAC4C))
        }
        state.measured?.let {
            val p=at(it.point);val color=if(state.live)Color(0xFF258CF5) else Color(0xFF8D9BA3)
            drawCircle(color.copy(alpha=0.15f),projection.radius(it.accuracyMeters).coerceIn(2f,120.dp.toPx()),p)
            drawCircle(Color.White,7.dp.toPx(),p);drawCircle(color,5.dp.toPx(),p)
        }
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.WHITE;textSize=11.dp.toPx() }
        val indices=setOf(0,state.route.stations.lastIndex,state.destination,state.alarmStation).filterNotNull()
        indices.forEach { index ->
            val p=points[index]
            val name=state.route.stations[index]+when(index){state.destination->" • Varış";state.alarmStation->" • Alarm";else->""}
            paint.textAlign=if(p.x>size.width/2)Paint.Align.RIGHT else Paint.Align.LEFT
            val x=if(p.x>size.width/2)p.x-12.dp.toPx() else p.x+12.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(name,x,p.y.coerceIn(15.dp.toPx(),size.height-8.dp.toPx()),paint)
        }
        paint.textAlign=Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText("K ↑ • Çevrimdışı hat",10.dp.toPx(),18.dp.toPx(),paint)
    }
}
