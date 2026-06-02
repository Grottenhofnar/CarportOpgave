
const listEl = document.getElementById("order-list");
let currentId = null;
let costTotal = 0;
let currentOrder = null;

document.getElementById("logout").addEventListener("click", async (e) => {
    e.preventDefault();
    await fetch("/api/logout", { method: "POST" });
    sessionStorage.clear();
    location.href = "/Login.html";
});

function money(v) { return (Number(v) || 0).toLocaleString("da-DK", { minimumFractionDigits: 2 }) + " kr."; }

function fillSelect(el, min, max, selected) {
    el.innerHTML = "";
    for (let v = min; v <= max; v += 30) {
        const o = document.createElement("option");
        o.value = v; o.textContent = v + " cm";
        if (v === selected) o.selected = true;
        el.appendChild(o);
    }
}

async function loadList() {
    listEl.innerHTML = "";
    const res = await fetch("/api/admin/orders");
    if (res.status === 403) { document.getElementById("list-login").classList.remove("hidden"); return; }
    const orders = await res.json();
    if (!orders.length) { document.getElementById("list-empty").classList.remove("hidden"); return; }
    const tpl = document.getElementById("list-item-template");
    orders.forEach(o => {
        const item = tpl.content.firstElementChild.cloneNode(true);
        item.querySelector(".li-label").textContent = `#${o.orderId} · ${o.contactName || "Gæst"}`;
        const badge = item.querySelector(".li-status");
        badge.textContent = o.statusLabel;
        badge.classList.add(o.status);
        item.addEventListener("click", () => openDetail(o.orderId));
        listEl.appendChild(item);
    });
}

async function openDetail(id) {
    currentId = id;
    const res = await fetch(`/api/admin/orders/${id}`);
    if (!res.ok) { document.getElementById("a-status").textContent = await res.text(); return; }
    const { order, bom } = await res.json();

    document.getElementById("detail-placeholder").classList.add("hidden");
    document.getElementById("detail-content").classList.remove("hidden");

    document.getElementById("d-title").textContent =
        `Ordre #${order.orderId} — ${order.contactName || "Gæst"}`;

    document.getElementById("d-contact-name").textContent = order.contactName || "(intet navn)";
    document.getElementById("d-contact-phone").textContent = order.contactPhone ? "Telefon: " + order.contactPhone : "";
    setLine("d-contact-email", order.contactEmail ? "Email: " + order.contactEmail : "");
    setLine("d-contact-address", "");
    setLine("d-contact-message", order.message ? "“" + order.message + "”" : "");
    document.getElementById("d-account").textContent = "";

    fillSelect(document.getElementById("f-width"), 240, 600, order.widthCm);
    fillSelect(document.getElementById("f-length"), 240, 780, order.lengthCm);
    fillSelect(document.getElementById("f-sw"), 120, 720, order.shedWidthCm || 210);
    fillSelect(document.getElementById("f-sl"), 120, 690, order.shedLengthCm || 270);
    document.getElementById("f-shed").checked = order.withShed;
    document.getElementById("f-note").value = order.adminNote || "";
    document.getElementById("p-sale").value = (order.totalPrice && order.totalPrice !== "") ? order.totalPrice : "";
    document.getElementById("a-status").textContent = "";

    renderBom(bom);
    drawAdmin();
    currentOrder = order;
    showSection("maal");
    updateReceipt();
}

function setLine(id, text) {
    const el = document.getElementById(id);
    el.innerHTML = "";
    if (text) { el.appendChild(document.createElement("br")); el.appendChild(document.createTextNode(text)); }
}

function readFields() {
    return {
        roofType: "plastic trapezoidal plates",
        widthCm: parseInt(document.getElementById("f-width").value),
        lengthCm: parseInt(document.getElementById("f-length").value),
        withShed: document.getElementById("f-shed").checked,
        shedWidthCm: parseInt(document.getElementById("f-sw").value),
        shedLengthCm: parseInt(document.getElementById("f-sl").value)
    };
}

function renderBom(bom) {
    const body = document.getElementById("bom-body");
    body.innerHTML = "";
    const tpl = document.getElementById("bom-row-template");
    bom.lines.forEach(l => {
        const row = tpl.content.firstElementChild.cloneNode(true);
        row.querySelector(".bl-desc").textContent = l.description;
        row.querySelector(".bl-length").textContent = l.lengthCm > 0 ? l.lengthCm + " cm" : "—";
        row.querySelector(".bl-qty").textContent = l.quantity;
        row.querySelector(".bl-price").textContent = money(l.lineTotal);
        body.appendChild(row);
    });
    costTotal = Number(bom.total) || 0;
    document.getElementById("total").textContent = money(costTotal);
    document.getElementById("p-cost").value = money(costTotal);

    const suggested = costTotal > 0 ? Math.round(costTotal / 0.70) : 0;
    document.getElementById("p-sale").placeholder = "";
    document.getElementById("p-sale").dataset.suggested = suggested;
    updateProfit();
}

function effectiveSale() {
    const el = document.getElementById("p-sale");
    const typed = parseFloat(el.value);
    if (!isNaN(typed) && typed > 0) return typed;
    return parseFloat(el.dataset.suggested) || 0;
}

function updateProfit() {
    const sale = effectiveSale();
    const profit = sale - costTotal;
    const profitEl = document.getElementById("p-profit");
    const marginEl = document.getElementById("p-margin");
    if (sale > 0) {
        const margin = (profit / sale) * 100;
        profitEl.textContent = money(sale);
        marginEl.textContent = margin.toFixed(1) + " % avance";
        profitEl.style.color = profit >= 0 ? "#4a5d3a" : "#b4231f";
    } else {
        profitEl.textContent = "—";
        marginEl.textContent = "sæt en salgspris";
        profitEl.style.color = "#16140f";
    }
}

async function recalcBom() {
    drawAdmin();
    const status = document.getElementById("a-status");
    try {
        const res = await fetch("/api/preview", {
            method: "POST", headers: { "Content-Type": "application/json" },
            credentials: "same-origin", body: JSON.stringify(readFields())
        });
        if (res.ok) {
            renderBom(await res.json());
            if (status) status.textContent = "";
        } else if (status) {
            status.style.color = "#b4231f";
            status.textContent = await res.text();
        }
    } catch (e) {  }
    updateReceipt();
}

function drawAdmin() {
    const cv = document.getElementById("ac");
    const ctx = cv.getContext("2d");
    const f = readFields();

    const cssW = 460, cssH = 300;
    const dpr = window.devicePixelRatio || 1;
    cv.style.width = cssW + "px";
    cv.style.height = cssH + "px";
    cv.width = Math.round(cssW * dpr);
    cv.height = Math.round(cssH * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, cssW, cssH);

    const pad = 40, scale = Math.min((cssW-2*pad)/f.lengthCm, (cssH-2*pad)/f.widthCm);
    const L = f.lengthCm*scale, W = f.widthCm*scale, x = pad, y = pad;
    ctx.lineWidth = 2; ctx.strokeStyle = "#000"; ctx.strokeRect(x, y, L, W);
    let gaps = Math.ceil(f.lengthCm/60); if (f.lengthCm/gaps >= 59.999) gaps++;
    const rafters = gaps+1; ctx.lineWidth = 1; ctx.strokeStyle = "#888";
    for (let i=0;i<rafters;i++){ const sx=x+L*i/(rafters-1); ctx.beginPath(); ctx.moveTo(sx,y); ctx.lineTo(sx,y+W); ctx.stroke(); }
    if (f.withShed && f.shedWidthCm && f.shedLengthCm) {

        const sl=f.shedLengthCm*scale, sw=f.shedWidthCm*scale;
        ctx.fillStyle="rgba(100,100,100,.18)"; ctx.fillRect(x+L-sl,y+W-sw,sl,sw);
        ctx.strokeStyle="#444"; ctx.strokeRect(x+L-sl,y+W-sw,sl,sw);
    }
    ctx.fillStyle="#000"; ctx.font="13px Arial";
    ctx.fillText(rafters + " spær · " + f.widthCm + "×" + f.lengthCm + " cm", x, y-10);
}

async function sendQuote() {
    const sale = effectiveSale();
    const status = document.getElementById("a-status");
    if (!sale || sale <= 0) { status.style.color = "#b4231f"; status.textContent = "Sæt en salgspris først."; return; }
    const note = encodeURIComponent(document.getElementById("f-note").value);
    const body = { ...readFields(), salePrice: sale };
    const res = await fetch(`/api/admin/orders/${currentId}/quote?note=${note}`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        credentials: "same-origin", body: JSON.stringify(body)
    });
    if (res.ok) { status.style.color = "#4a5d3a"; status.textContent = "Tilbud og tegning sendt til kunden."; loadList(); }
    else { status.style.color = "#b4231f"; status.textContent = await res.text(); }
}

function showSection(sec) {
    document.querySelectorAll(".admin-sec").forEach(s => s.classList.add("hidden"));
    const target = document.getElementById("sec-" + sec);
    if (target) target.classList.remove("hidden");
    document.querySelectorAll("#admin-nav button").forEach(b => {
        b.classList.toggle("active-page", b.dataset.sec === sec);
    });
    if (sec === "tegning") drawAdmin();
    if (sec === "kvittering") updateReceipt();
}

function updateReceipt() {
    if (!currentOrder) return;
    const f = readFields();
    const sale = effectiveSale();
    document.getElementById("r-orderno").textContent = "Ordre #" + currentOrder.orderId;
    document.getElementById("r-customer").textContent =
        (currentOrder.contactName || "") + (currentOrder.contactEmail ? " · " + currentOrder.contactEmail : "");
    const shed = f.withShed ? `, redskabsrum ${f.shedWidthCm}×${f.shedLengthCm} cm` : "";
    document.getElementById("r-spec").textContent = `Carport ${f.widthCm}×${f.lengthCm} cm${shed}`;
    document.getElementById("r-base").textContent = money(sale);
    document.getElementById("r-total").textContent = money(sale);
}

["f-width","f-length","f-shed","f-sw","f-sl"].forEach(id =>
    document.getElementById(id).addEventListener("change", () => { recalcBom(); updateReceipt(); }));
document.getElementById("p-sale").addEventListener("input", (e) => {

    const cleaned = e.target.value.replace(/[^0-9]/g, "");
    if (e.target.value !== cleaned) e.target.value = cleaned;
    updateProfit(); updateReceipt();
});
document.getElementById("send").addEventListener("click", sendQuote);
document.querySelectorAll("#admin-nav button").forEach(b =>
    b.addEventListener("click", () => showSection(b.dataset.sec)));

loadList();
