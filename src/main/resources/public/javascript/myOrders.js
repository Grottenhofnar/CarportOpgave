
const box = document.getElementById("orders");

document.getElementById("logout").addEventListener("click", async (e) => {
    e.preventDefault();
    await fetch("/api/logout", { method: "POST" });
    sessionStorage.clear();
    location.href = "/Order.html";
});

function money(v) { return (Number(v) || 0).toLocaleString("da-DK", { minimumFractionDigits: 2 }) + " kr."; }
function show(id) { document.getElementById(id).classList.remove("hidden"); }

async function loadProfile() {
    const set = (id, val) => { const el = document.getElementById(id); if (el) el.value = val || ""; };
    const fill = (p) => {
        set("p-email", p.email); set("p-firstname", p.firstName); set("p-lastname", p.lastName);
        set("p-address", p.address); set("p-postalcode", p.postalCode); set("p-city", p.city);
        set("p-phone", p.phone);
    };
    const cached = sessionStorage.getItem("profile");
    if (cached) { try { fill(JSON.parse(cached)); } catch (e) {} }
    try {
        const res = await fetch("/api/profile", { credentials: "same-origin" });
        if (res.ok) fill(await res.json());
    } catch (e) {}
}

const saveBtn = document.getElementById("save-profile");
if (saveBtn) saveBtn.addEventListener("click", async () => {
    const status = document.getElementById("profile-status");
    const v = (id) => document.getElementById(id).value.trim();
    const payload = {
        email: v("p-email"), firstName: v("p-firstname"), lastName: v("p-lastname"),
        address: v("p-address"), postalCode: v("p-postalcode"), city: v("p-city"), phone: v("p-phone")
    };
    if (!payload.email) { status.style.color = "#b4231f"; status.textContent = "Email skal udfyldes."; return; }
    const vr = window.ContactValidation.validateContact(payload, ["email", "firstName", "phone", "postalCode", "city"]);
    if (!vr.ok) { status.style.color = "#b4231f"; status.textContent = Object.values(vr.errors)[0]; return; }
    try {
        const res = await fetch("/api/profile", { method: "POST", headers: { "Content-Type": "application/json" },
            credentials: "same-origin", body: JSON.stringify(payload) });
        if (!res.ok) { status.style.color = "#b4231f"; status.textContent = await res.text(); return; }
        const updated = await res.json();
        sessionStorage.setItem("profile", JSON.stringify(updated));
        sessionStorage.setItem("email", updated.email);
        status.style.color = "#4a5d3a";
        status.textContent = "Oplysninger gemt.";
    } catch (e) { status.style.color = "#b4231f"; status.textContent = e.message; }
});

const pwModal = document.getElementById("pw-modal");
function openPwModal() {
    document.getElementById("pw-current").value = "";
    document.getElementById("pw-new").value = "";
    document.getElementById("pw-repeat").value = "";
    document.getElementById("pw-status").textContent = "";
    pwModal.classList.remove("hidden");
}
function closePwModal() { pwModal.classList.add("hidden"); }

const changePwBtn = document.getElementById("change-pw");
if (changePwBtn) changePwBtn.addEventListener("click", openPwModal);
const pwCancel = document.getElementById("pw-cancel");
if (pwCancel) pwCancel.addEventListener("click", closePwModal);

if (pwModal) pwModal.addEventListener("click", (e) => { if (e.target === pwModal) closePwModal(); });

const pwSave = document.getElementById("pw-save");
if (pwSave) pwSave.addEventListener("click", async () => {
    const st = document.getElementById("pw-status");
    const current = document.getElementById("pw-current").value;
    const next = document.getElementById("pw-new").value;
    const repeat = document.getElementById("pw-repeat").value;
    st.style.color = "#b4231f";
    if (!current || !next) { st.textContent = "Udfyld alle felter."; return; }
    if (next.length < 4) { st.textContent = "Ny adgangskode skal være mindst 4 tegn."; return; }
    if (next !== repeat) { st.textContent = "De to nye adgangskoder er ikke ens."; return; }
    try {
        const res = await fetch("/api/profile/password", {
            method: "POST", headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: JSON.stringify({ currentPassword: current, newPassword: next, repeatPassword: repeat })
        });
        if (res.ok) {
            st.style.color = "#4a5d3a";
            st.textContent = "Adgangskode skiftet.";
            setTimeout(closePwModal, 900);
        } else {
            st.textContent = await res.text();
        }
    } catch (e) { st.textContent = e.message; }
});

async function load() {
    try {
        const res = await fetch("/api/orders");
        if (res.status === 401) { show("orders-login"); return; }
        const orders = await res.json();
        if (!orders.length) { show("orders-empty"); return; }
        orders.forEach(addCard);
    } catch (e) {
        const el = document.getElementById("orders-error");
        el.textContent = e.message;
        el.classList.remove("hidden");
    }
}

function addCard(o) {
    const tpl = document.getElementById("order-card-template");
    const card = tpl.content.firstElementChild.cloneNode(true);

    card.querySelector(".oc-id").textContent = "Ordre #" + o.orderId;

    const badge = card.querySelector(".oc-status");
    badge.textContent = o.statusLabel;
    badge.classList.add(o.status);

    card.querySelector(".oc-dims").textContent =
        `${(o.widthCm/100).toFixed(2)} × ${(o.lengthCm/100).toFixed(2)} m` + (o.withShed ? " · med redskabsrum" : "");

    if (o.adminNote) {
        const note = card.querySelector(".oc-note");
        note.textContent = "Besked fra Fog: " + o.adminNote;
        note.classList.remove("hidden");
    }

    const price = o.totalPrice ? money(o.totalPrice) : "—";
    card.querySelector(".oc-price").textContent = "Pris: " + price;

    const sketch = card.querySelector(".oc-sketch");
    if (o.sketchReleased) {
        sketch.classList.remove("hidden");
        drawSketch(sketch, o);
    } else {
        card.querySelector(".oc-sketch-pending").classList.remove("hidden");
    }

    const payBtn = card.querySelector(".oc-pay");
    const bomBtn = card.querySelector(".oc-bom");
    const pdfBtn = card.querySelector(".oc-pdf");
    payBtn.classList.add("hidden");
    bomBtn.classList.add("hidden");
    pdfBtn.classList.add("hidden");
    if (o.paid) {

        bomBtn.classList.remove("hidden");
        bomBtn.addEventListener("click", () => showBom(o.orderId, card));
        pdfBtn.classList.remove("hidden");
        pdfBtn.addEventListener("click", () => downloadPdf(o.orderId));
    } else if (o.status === "QUOTED") {

        payBtn.textContent = "Betal " + price;
        payBtn.classList.remove("hidden");
        payBtn.addEventListener("click", () => doPay(o.orderId));
    }

    box.appendChild(card);
}

async function doPay(id) {
    const r = await fetch(`/api/orders/${id}/pay`, { method: "POST", credentials: "same-origin" });
    if (r.ok) { box.innerHTML = ""; load(); } else alert(await r.text());
}

function downloadPdf(id) {

    window.open(`/api/orders/${id}/pdf`, "_blank");
}

async function showBom(id, card) {
    const target = card.querySelector(".bom-target");
    target.innerHTML = "";
    const res = await fetch(`/api/orders/${id}/bom`);
    if (res.status === 402) { target.textContent = "Styklisten frigives efter betaling."; return; }
    if (!res.ok) { target.textContent = await res.text(); return; }
    const data = await res.json();

    const table = document.getElementById("bom-table-template").content.firstElementChild.cloneNode(true);
    const body = table.querySelector(".bom-body");
    data.lines.forEach(l => {
        const row = document.getElementById("bom-row-template").content.firstElementChild.cloneNode(true);
        row.querySelector(".bl-desc").textContent = l.description;
        row.querySelector(".bl-qty").textContent = l.quantity;
        row.querySelector(".bl-price").textContent = money(l.lineTotal);
        body.appendChild(row);
    });
    table.querySelector(".bom-total").textContent = money(data.total);
    target.appendChild(table);
}

function drawSketch(cv, o) {
    const ctx = cv.getContext("2d");
    const cssW = 420, cssH = 300;
    const dpr = window.devicePixelRatio || 1;
    cv.style.width = cssW + "px";
    cv.style.height = cssH + "px";
    cv.width = Math.round(cssW * dpr);
    cv.height = Math.round(cssH * dpr);
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, cssW, cssH);
    const pad = 45, scale = Math.min((cssW-2*pad)/o.lengthCm, (cssH-2*pad)/o.widthCm);
    const L = o.lengthCm*scale, W = o.widthCm*scale, x = pad, y = pad, ps = 8;
    ctx.lineWidth = 3; ctx.strokeStyle = "#000"; ctx.strokeRect(x, y, L, W);
    let gaps = Math.ceil(o.lengthCm/60); if (o.lengthCm/gaps >= 59.999) gaps++;
    const rafters = gaps+1; ctx.lineWidth = 1; ctx.strokeStyle = "#777";
    for (let i=0;i<rafters;i++){ const sx=x+L*i/(rafters-1); ctx.beginPath(); ctx.moveTo(sx,y); ctx.lineTo(sx,y+W); ctx.stroke(); }
    ctx.fillStyle = "#444";
    ctx.fillRect(x,y,ps,ps); ctx.fillRect(x+L-ps,y,ps,ps); ctx.fillRect(x,y+W-ps,ps,ps); ctx.fillRect(x+L-ps,y+W-ps,ps,ps);
    if (o.withShed && o.shedWidthCm && o.shedLengthCm) {
        const sl=o.shedLengthCm*scale, sw=o.shedWidthCm*scale;
        ctx.fillStyle="rgba(100,100,100,.18)"; ctx.fillRect(x+L-sl,y+W-sw,sl,sw);
        ctx.lineWidth=2; ctx.strokeStyle="#444"; ctx.strokeRect(x+L-sl,y+W-sw,sl,sw);
        ctx.fillStyle="#444"; ctx.font="14px Arial"; ctx.fillText("Redskabsrum", x+L-sl+6, y+W-sw+18);
    }
    ctx.fillStyle="#000"; ctx.font="16px Arial";
    ctx.fillText("Carport Plan", x, y-12);
    ctx.fillText(o.widthCm+"cm", x-40, y+W/2);
    ctx.fillText(o.lengthCm+"cm", x+L/2-25, y+W+30);
}

loadProfile();
load();
if (window.ContactValidation) {
    window.ContactValidation.digitsOnly(document.getElementById("p-postalcode"));
    window.ContactValidation.digitsOnly(document.getElementById("p-phone"));
}
