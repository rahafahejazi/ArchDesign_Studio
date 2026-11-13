import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ArchDesignStudio {

    private static final String PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>ArchDesign Studio</title>
<style>
* { box-sizing: border-box; }
body {
    margin: 0;
    font-family: Arial, sans-serif;
    background: #eef1f5;
    color: #253247;
}
header {
    background: #192b43;
    color: white;
    padding: 22px 30px;
}
header h1 { margin: 0; font-size: 25px; }
header p { margin: 7px 0 0; color: #cbd5e1; }
main {
    display: grid;
    grid-template-columns: 260px 1fr;
    gap: 20px;
    padding: 22px;
}
.panel, .workspace {
    background: white;
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 3px 12px #172b4310;
}
h2 { font-size: 19px; margin-top: 0; }
label {
    display: block;
    margin: 17px 0 7px;
    font-weight: bold;
    font-size: 13px;
}
input, select {
    width: 100%;
    padding: 11px;
    border: 1px solid #cbd5e1;
    border-radius: 7px;
    font-size: 15px;
}
button {
    background: #315d87;
    color: white;
    border: 0;
    border-radius: 7px;
    padding: 12px;
    cursor: pointer;
    font-weight: bold;
}
button:hover { background: #244768; }
#generate { width: 100%; margin-top: 22px; }
.workspace { min-width: 0; }
.tabs {
    display: flex;
    gap: 10px;
    margin-bottom: 17px;
}
.tabs button { background: #dce5ef; color: #253247; }
.tabs button.active { background: #315d87; color: white; }
.view {
    background: #f7f9fc;
    border: 1px solid #e1e7ef;
    border-radius: 9px;
    padding: 12px;
    min-height: 390px;
}
svg { width: 100%; height: 360px; }
canvas { width: 100%; height: 360px; display: block; }
.controls {
    display: flex;
    gap: 18px;
    flex-wrap: wrap;
    margin-top: 14px;
}
.controls label { flex: 1; min-width: 130px; }
.controls input { padding: 0; }
.stats {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    margin-top: 18px;
}
.stat {
    background: #edf3f9;
    padding: 13px;
    border-radius: 8px;
    flex: 1;
    min-width: 100px;
}
.stat strong { display: block; font-size: 19px; }
.stat small { color: #607086; }
#message { color: #b42318; font-size: 13px; margin-top: 12px; }
@media(max-width: 750px) {
    main { grid-template-columns: 1fr; padding: 12px; }
}
</style>
</head>
<body>
<header>
    <h1>ArchDesign Studio</h1>
    <p>Design your space. Visualize your ideas.</p>
</header>

<main>
<section class="panel">
    <h2>Project Settings</h2>

    <label for="area">Total House Area (sq ft)</label>
    <input id="area" type="number" min="100" max="20000" value="2000">

    <label for="floors">Number of Floors</label>
    <select id="floors">
        <option value="1">1 Floor</option>
        <option value="2" selected>2 Floors</option>
        <option value="3">3 Floors</option>
    </select>

    <label for="rooms">Total Number of Rooms</label>
    <input id="rooms" type="number" min="1" max="30" value="6">

    <button id="generate">Generate Design</button>
    <div id="message"></div>

    <div class="stats">
        <div class="stat">
            <strong id="statArea">2,000</strong>
            <small>Square Feet</small>
        </div>
        <div class="stat">
            <strong id="statFloors">2</strong>
            <small>Floors</small>
        </div>
        <div class="stat">
            <strong id="statRooms">6</strong>
            <small>Rooms</small>
        </div>
    </div>
</section>

<section class="workspace">
    <h2>Design Workspace</h2>

    <div class="tabs">
        <button id="tab2d" class="active">2D Floor Plan</button>
        <button id="tab3d">3D Model</button>
    </div>

    <div class="view" id="view2d">
        <label for="floorSelect">Select Floor</label>
        <select id="floorSelect"></select>
        <svg id="floorPlan" viewBox="0 0 700 360"
             xmlns="http://www.w3.org/2000/svg">
        </svg>
    </div>

    <div class="view" id="view3d" style="display:none">
        <canvas id="model"></canvas>
        <div class="controls">
            <label>
                Rotation
                <input id="rotation" type="range"
                       min="0" max="360" value="35">
            </label>
            <label>
                Zoom
                <input id="zoom" type="range"
                       min="40" max="130" value="80">
            </label>
        </div>
    </div>
</section>
</main>

<script>
let area = 2000;
let floors = 2;
let rooms = 6;
let rotation = 35;
let zoom = 80;

const $ = id => document.getElementById(id);

function generate() {
    const a = Number($("area").value);
    const f = Number($("floors").value);
    const r = Number($("rooms").value);

    if (!Number.isFinite(a) || a < 100 || a > 20000 ||
        !Number.isInteger(r) || r < 1 || r > 30) {
        $("message").textContent =
            "Enter an area from 100 to 20,000 and 1 to 30 rooms.";
        return;
    }

    area = a;
    floors = f;
    rooms = r;

    $("message").textContent = "";
    $("statArea").textContent = area.toLocaleString();
    $("statFloors").textContent = floors;
    $("statRooms").textContent = rooms;

    const selector = $("floorSelect");
    selector.innerHTML = "";

    for (let i = 1; i <= floors; i++) {
        const option = document.createElement("option");
        option.value = i;
        option.textContent = "Floor " + i;
        selector.appendChild(option);
    }

    drawPlan();
    drawModel();
}

function roomsOnFloor(floor) {
    const base = Math.floor(rooms / floors);
    const extra = rooms % floors;
    return base + (floor <= extra ? 1 : 0);
}

function drawPlan() {
    const floor = Number($("floorSelect").value || 1);
    const count = roomsOnFloor(floor);
    const svg = $("floorPlan");

    const width = Math.sqrt(area * 1.4);
    const height = area / width;

    const x = 115, y = 65, w = 470, h = 225;
    const columns = Math.ceil(Math.sqrt(count));
    const rows = Math.ceil(count / columns);
    const cellW = w / columns;
    const cellH = h / rows;

    let output = `
    <rect x="0" y="0" width="700" height="360"
          fill="#f7f9fc"/>
    <text x="350" y="25" text-anchor="middle"
          font-size="16" font-weight="bold" fill="#253247">
          FLOOR ${floor} - FLOOR PLAN
    </text>
    <rect x="${x}" y="${y}" width="${w}" height="${h}"
          fill="white" stroke="#263b52" stroke-width="5"/>`;

    for (let i = 0; i < count; i++) {
        const col = i % columns;
        const row = Math.floor(i / columns);
        const rx = x + col * cellW;
        const ry = y + row * cellH;

        output += `
        <rect x="${rx}" y="${ry}" width="${cellW}"
              height="${cellH}" fill="#e6eef6"
              stroke="#496985" stroke-width="2"/>
        <text x="${rx + cellW/2}" y="${ry + cellH/2}"
              text-anchor="middle" font-size="13"
              fill="#253247">Room ${i + 1}</text>`;
    }

    output += `
    <text x="350" y="320" text-anchor="middle"
          font-size="13" fill="#526579">
          Approx. dimensions: ${width.toFixed(1)} ft ×
          ${height.toFixed(1)} ft
    </text>
    <text x="350" y="343" text-anchor="middle"
          font-size="12" fill="#526579">
          ${area.toLocaleString()} sq ft total house area
    </text>`;

    svg.innerHTML = output;
}

function drawModel() {
    const canvas = $("model");
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;

    canvas.width = rect.width * dpr;
    canvas.height = 360 * dpr;

    const ctx = canvas.getContext("2d");
    ctx.scale(dpr, dpr);

    const W = rect.width;
    const H = 360;

    ctx.fillStyle = "#f7f9fc";
    ctx.fillRect(0, 0, W, H);

    const angle = rotation * Math.PI / 180;
    const scale = zoom / 100;
    const cx = W / 2;
    const baseY = H * 0.75;
    const size = Math.min(W * 0.28, 125) * scale;
    const depth = size * 0.65;
    const floorHeight = 65 * scale;

    function point(x, y, z) {
        const rx = x * Math.cos(angle) -
                   y * Math.sin(angle);
        const ry = x * Math.sin(angle) +
                   y * Math.cos(angle);

        return [
            cx + rx,
            baseY - z - ry * 0.48
        ];
    }

    function polygon(points, fill, stroke) {
        ctx.beginPath();
        points.forEach((p, i) => {
            if (i === 0) ctx.moveTo(p[0], p[1]);
            else ctx.lineTo(p[0], p[1]);
        });
        ctx.closePath();
        ctx.fillStyle = fill;
        ctx.fill();
        ctx.strokeStyle = stroke;
        ctx.lineWidth = 2;
        ctx.stroke();
    }

    for (let floor = 0; floor < floors; floor++) {
        const z = floor * floorHeight;
        const a = point(-size/2, -depth/2, z);
        const b = point(size/2, -depth/2, z);
        const c = point(size/2, depth/2, z);
        const d = point(-size/2, depth/2, z);

        const A = point(-size/2, -depth/2, z + floorHeight);
        const B = point(size/2, -depth/2, z + floorHeight);
        const C = point(size/2, depth/2, z + floorHeight);
        const D = point(-size/2, depth/2, z + floorHeight);

        polygon([a, b, B, A], "#b8cddd", "#35516b");
        polygon([b, c, C, B], "#8daac2", "#35516b");
        polygon([d, a, A, D], "#d8e4ed", "#35516b");

        const center = point(0, -depth/2 - 1,
                             z + floorHeight * 0.55);
        ctx.fillStyle = "#315d87";
        ctx.font = "bold 11px Arial";
        ctx.textAlign = "center";
        ctx.fillText("Floor " + (floor + 1),
                     center[0], center[1]);
    }

    const topZ = floors * floorHeight;
    const top = [
        point(-size/2, -depth/2, topZ),
        point(size/2, -depth/2, topZ),
        point(size/2, depth/2, topZ),
        point(-size/2, depth/2, topZ)
    ];

    polygon(top, "#dce8f1", "#35516b");

    ctx.fillStyle = "#253247";
    ctx.font = "bold 15px Arial";
    ctx.textAlign = "center";
    ctx.fillText("3D House Model", cx, 27);

    ctx.font = "12px Arial";
    ctx.fillStyle = "#607086";
    ctx.fillText("Use the controls to rotate and zoom",
                 cx, H - 12);
}

$("generate").addEventListener("click", generate);

$("floorSelect").addEventListener("change", drawPlan);

$("tab2d").addEventListener("click", () => {
    $("view2d").style.display = "block";
    $("view3d").style.display = "none";
    $("tab2d").classList.add("active");
    $("tab3d").classList.remove("active");
});

$("tab3d").addEventListener("click", () => {
    $("view2d").style.display = "none";
    $("view3d").style.display = "block";
    $("tab3d").classList.add("active");
    $("tab2d").classList.remove("active");
    drawModel();
});

$("rotation").addEventListener("input", event => {
    rotation = Number(event.target.value);
    drawModel();
});

$("zoom").addEventListener("input", event => {
    zoom = Number(event.target.value);
    drawModel();
});

window.addEventListener("resize", drawModel);

generate();
</script>
</body>
</html>
""";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/", exchange -> {
            byte[] response = PAGE.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });

        server.start();

        System.out.println("ArchDesign Studio is running!");
System.out.println("Open port 8080 in your browser.");    }
}