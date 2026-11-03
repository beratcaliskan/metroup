const fs=require('fs'),http=require('http'),path=require('path');
const projectRoot=path.resolve(__dirname,'..');process.chdir(projectRoot);
const {chromium}=require(process.env.PLAYWRIGHT_CORE_MODULE||path.join(projectRoot,'.tools/playwright-core/package'));
const root=path.resolve('app/src/main/assets/map');
const base=JSON.parse(fs.readFileSync('docs/m2-stations.json','utf8')).filter(s=>s.id!==31);
const m1a=JSON.parse(fs.readFileSync('docs/m1a-stations.json','utf8'));
const toStations=rows=>rows.map(s=>({name:s.name,point:[s.latitude,s.longitude]}));
(async()=>{
 const server=http.createServer((req,res)=>{const name=path.basename(req.url.split('?')[0])||'index.html';const file=path.join(root,name);if(!fs.existsSync(file)){res.writeHead(404).end();return;}res.setHeader('Content-Type',name.endsWith('.js')?'text/javascript':name.endsWith('.css')?'text/css':'text/html');res.end(fs.readFileSync(file));});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.CHROME_PATH||'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
 try {
 const page=await browser.newPage({viewport:{width:390,height:650},deviceScaleFactor:1});let errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:'+server.address().port+'/index.html');await page.waitForFunction(()=>typeof window.renderMetro==='function');
 const state={routeId:'M2',code:'M2',color:'#36b68d',stations:toStations(base),start:0,destination:14,alarm:12,estimated:[41.045,28.9865],measured:{point:[41.0453,28.9866],accuracy:18,age:1,live:true}};
 await page.evaluate(s=>window.renderMetro(s),state);await page.waitForTimeout(2000);
 if(await page.evaluate(()=>stationMarkers.length)!==15)throw Error('M2 station count');
 await page.screenshot({path:'.tools/map-m2-online.png'});
 await page.locator('#locate').click();await page.waitForTimeout(1000);if(await page.evaluate(()=>map.getZoom())!==16)throw Error('Locate zoom');
 const before=await page.evaluate(()=>map.getCenter().toString());await page.evaluate(s=>window.renderMetro(s),{...state,measured:{...state.measured,age:2}});
 if(await page.evaluate(()=>map.getCenter().toString())!==before)throw Error('Camera reset on tick');
 await page.screenshot({path:'.tools/map-position.png'});
 const moved={...state,measured:{...state.measured,point:[41.0473,28.9866]}};
 await page.evaluate(s=>window.renderMetro(s),moved);
 if(Math.abs(await page.evaluate(()=>map.getCenter().lat)-41.0473)>0.00001)throw Error('Follow did not move camera');
 await page.mouse.move(180,300);await page.mouse.down();await page.mouse.move(130,350,{steps:6});await page.mouse.up();await page.waitForTimeout(500);
 if(await page.evaluate(()=>following))throw Error('Drag did not stop following');
 const dragged=await page.evaluate(()=>map.getCenter().toString());await page.evaluate(s=>window.renderMetro(s),state);
 if(await page.evaluate(()=>map.getCenter().toString())!==dragged)throw Error('User camera overwritten');

 await page.route('https://tile.openstreetmap.org/**',r=>r.abort());
 const shuttle=JSON.parse(fs.readFileSync('docs/m2-stations.json','utf8')).filter(s=>[30,31].includes(s.id));
 await page.evaluate(s=>window.renderMetro(s),{...state,routeId:'M2_SHUTTLE',stations:toStations(shuttle),start:0,destination:1,alarm:0,measured:null,estimated:shuttle.map(s=>[s.latitude,s.longitude])[0]});
 await page.waitForTimeout(1000);
 if(await page.evaluate(()=>stationMarkers.length)!==2)throw Error('Old route leaked');
 if(!await page.locator('#tileStatus').isVisible())throw Error('No fallback notice');
 await page.screenshot({path:'.tools/map-offline.png'});
 await page.evaluate(s=>window.renderMetro(s),{...state,routeId:'M1A',code:'M1A',color:'#ef6269',stations:toStations(m1a),start:8,destination:0,alarm:2,measured:{...state.measured,age:45,live:false},estimated:m1a[6]&&[m1a[6].latitude,m1a[6].longitude]});
 if(await page.evaluate(()=>stationMarkers.length)!==18)throw Error('M1A station count');
 const stale=await page.locator('.measurement-old').count();
 if(!stale)throw Error('Stale measurement not grey');
 if(errors.length)throw Error(errors.join('; '));
 console.log('PASS: M2/M1A/shuttle, locate, stable camera, stale marker, offline route; no JS errors');
 }finally{await browser.close();server.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
