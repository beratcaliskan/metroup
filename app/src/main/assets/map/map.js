"use strict";
const map=L.map("map",{zoomControl:false,minZoom:9,maxZoom:18}).setView([41.04,28.96],12);
L.control.zoom({position:"topright"}).addTo(map);
L.control.scale({position:"bottomleft",imperial:false}).addTo(map);
const tiles=L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png",{
    maxZoom:18,attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    updateWhenIdle:true,keepBuffer:1
}).addTo(map);
const tileStatus=document.getElementById("tileStatus");
tiles.on("tileerror",()=>{tileStatus.hidden=false;tileStatus.textContent="Sokak haritası yüklenemedi. Hat ve duraklar kullanılabilir.";});
tiles.on("tileload",()=>{tileStatus.hidden=true;});
let state=null,routeKey=null,routeLayer=L.layerGroup().addTo(map),dynamicLayer=L.layerGroup().addTo(map);
let line=null,selectedStation=null,stationMarkers=[],following=false;
function text(value){const el=document.createElement("span");el.textContent=value;return el;}
function icon(css,label,size){return L.divIcon({className:css,html:label,iconSize:[size,size],iconAnchor:[size/2,size/2]});}
function fit(){following=false;document.getElementById("locate").textContent="Konumuma git";if(line)map.fitBounds(line.getBounds(),{padding:[30,55],maxZoom:15});}
function labels(){if(!state)return;stationMarkers.forEach((marker,i)=>{
    if(marker.getTooltip())marker.unbindTooltip();
    const important=i===0||i===state.stations.length-1||i===state.destination||i===state.alarm||i===selectedStation;
    if(map.getZoom()>=14||important)marker.bindTooltip(text(state.stations[i].name+(i===state.destination?" • Varış":i===state.alarm?" • Alarm":"")),{permanent:true,direction:map.latLngToContainerPoint(marker.getLatLng()).x>map.getSize().x*0.55?"left":"right",className:"station-label",opacity:1});
});}
map.on("zoomend moveend",labels);
map.on("dragstart",()=>{following=false;document.getElementById("locate").textContent="Konumuma git";});
document.getElementById("fit").onclick=fit;
document.getElementById("locate").onclick=()=>{
    if(!state)return;
    const point=(state.measured&&state.measured.live)?state.measured.point:state.estimated;
    if(point){following=true;document.getElementById("locate").textContent="Konum takibi açık";map.setView(point,16);}else fit();
};
window.renderMetro=function(next){
    const key=JSON.stringify([next.routeId,next.start,next.destination,next.alarm]);
    const changed=key!==routeKey;state=next;
    if(changed){
        routeKey=key;routeLayer.clearLayers();stationMarkers=[];selectedStation=null;
        const points=state.stations.map(s=>s.point);
        line=L.polyline(points,{color:"#273944",weight:10,opacity:0.9}).addTo(routeLayer);
        L.polyline(points,{color:state.color,weight:5,opacity:1}).addTo(routeLayer)
            .bindPopup(text("Durakları birleştiren yaklaşık hat çizgisi; gerçek ray güzergâhı değildir."));
        stationMarkers=state.stations.map((s,i)=>{
            const marker=L.circleMarker(s.point,{radius:6,color:"#fff",weight:2,fillColor:state.color,fillOpacity:1}).addTo(routeLayer);
            marker.bindPopup(text(state.code+" • "+s.name));marker.on("click",()=>{selectedStation=i;labels();});return marker;
        });
        if(state.start!==state.destination)L.circleMarker(points[state.start],{radius:10,color:"#fff",weight:3,fillColor:"#17272f",fillOpacity:1}).addTo(routeLayer).bindPopup(text("Biniş: "+state.stations[state.start].name));
        if(state.alarm!==null)L.marker(points[state.alarm],{icon:icon("alarm","!",22)}).addTo(routeLayer).bindPopup(text("Alarm durağı: "+state.stations[state.alarm].name));
        L.marker(points[state.destination],{icon:icon("destination","V",24)}).addTo(routeLayer).bindPopup(text("Varış: "+state.stations[state.destination].name));
        fit();labels();
    }
    dynamicLayer.clearLayers();
    if(state.estimated)L.marker(state.estimated,{icon:icon("estimated","",16)}).addTo(dynamicLayer).bindPopup(text("Hat üzerindeki tahmini konum. Uydu konumu değildir."));
    if(state.measured){const m=state.measured,color=m.live?"#258cf5":"#8d9ba3";
        L.circle(m.point,{radius:m.accuracy,color,weight:1,fillOpacity:0.13}).addTo(dynamicLayer);
        L.marker(m.point,{icon:icon("measured "+(m.live?"measurement-live":"measurement-old"),"",14),zIndexOffset:1000}).addTo(dynamicLayer)
            .bindPopup(text((m.live?"Son ölçüm":"Eski ölçüm")+" • "+m.age+" sn önce • ±"+m.accuracy+" m"));
    }
    if(following){const target=(state.measured&&state.measured.live)?state.measured.point:state.estimated;if(target)map.panTo(target,{animate:false});}
};
window.addEventListener("resize",()=>map.invalidateSize());

// AndroidView may finish loading before Compose gives WebView its final size.
window.resizeMetro=function(width,height){
    if(!(width>0&&height>0))return;
    document.documentElement.style.width=width+"px";
    document.documentElement.style.height=height+"px";
    document.body.style.width=width+"px";
    document.body.style.height=height+"px";
    document.getElementById("map").style.height=height+"px";
    map.invalidateSize({animate:false,pan:false});
    if(line&&!following)map.fitBounds(line.getBounds(),{padding:[30,55],maxZoom:15,animate:false});
};
window.metroFrame=function(data,width,height){
    if(width>0&&height>0&&(map.getSize().x!==width||map.getSize().y!==height))window.resizeMetro(width,height);
    window.renderMetro(data);
    return map.getSize().x>0&&map.getSize().y>0&&stationMarkers.length===data.stations.length;
};
