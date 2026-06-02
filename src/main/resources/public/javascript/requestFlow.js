
(function () {
    function isLoggedIn() { return !!sessionStorage.getItem("email"); }

    async function setupContactStep() {
        const prompt = document.getElementById("contact-login-prompt");
        const note = document.getElementById("contact-loggedin-note");
        if (isLoggedIn()) {
            if (prompt) prompt.style.display = "none";
            const fill = (p) => {
                const set = (id, val) => { const el = document.getElementById(id); if (el && val) el.value = val; };
                set("fname", p.firstName);
                set("lname", p.lastName);
                set("address", p.address);
                set("postalcode", p.postalCode);
                set("city", p.city);
                set("email", p.email);
                set("phone-number", p.phone);
                if (note) note.style.display = "block";
            };

            const cached = sessionStorage.getItem("profile");
            if (cached) { try { fill(JSON.parse(cached)); } catch (e) {} }

            try {
                const res = await fetch("/api/profile", { credentials: "same-origin" });
                if (res.ok) fill(await res.json());
            } catch (e) {  }
        } else {
            if (prompt) prompt.style.display = "block";
            if (note) note.style.display = "none";
        }
    }

    function readSpec() {
        const choice = (localStorage.getItem("shedChoice") || "").replace(/"/g, "");
        return {
            roofType: (localStorage.getItem("carportRoofType") || "").replace(/"/g, ""),
            widthCm: parseInt(localStorage.getItem("carportWidth")) || 0,
            lengthCm: parseInt(localStorage.getItem("carportLength")) || 0,
            withShed: choice === "yes",
            shedWidthCm: parseInt(localStorage.getItem("shedWidth")) || 0,
            shedLengthCm: parseInt(localStorage.getItem("shedLength")) || 0
        };
    }

    function readContact() {
        const v = (id) => { const el = document.getElementById(id); return el ? el.value.trim() : ""; };
        return {
            firstName: v("fname"), lastName: v("lname"), address: v("address"),
            postalCode: v("postalcode"), city: v("city"),
            email: v("email"), phone: v("phone-number"), message: v("message")
        };
    }

    function roofTypeDa(v) {
        const map = {
            "without roof tiles": "Uden tagplader",
            "plastic trapezoidal plates": "Plasttrapezplader"
        };
        return map[v] || v || "—";
    }

    function renderSummary() {
        const box = document.getElementById("done-summary");
        if (!box) return;
        const sp = readSpec();
        if (!sp.widthCm || !sp.lengthCm) {
            box.innerHTML = "<p style='color:#b4231f;'>Vælg bredde og længde på Carport-trinnet.</p>";
            return;
        }
        let c = readContact();

        if (isLoggedIn() && (!c.firstName && !c.email && !c.phone)) {
            const cached = sessionStorage.getItem("profile");
            if (cached) { try {
                const p = JSON.parse(cached);
                c = { firstName: p.firstName, lastName: p.lastName, address: p.address,
                      postalCode: p.postalCode, city: p.city, email: p.email, phone: p.phone };
            } catch (e) {} }
        }
        const shed = sp.withShed ? `${sp.shedWidthCm} × ${sp.shedLengthCm} cm` : "Nej";
        const navn = (c.firstName || "") + " " + (c.lastName || "");
        const adr = [c.address, [c.postalCode, c.city].filter(Boolean).join(" ")].filter(Boolean).join(", ");
        const kontaktRows = `
            <hr class="receipt-line">
            <div class="receipt-row"><span>Navn</span><span>${navn.trim() || "—"}</span></div>
            ${adr ? `<div class="receipt-row"><span>Adresse</span><span>${adr}</span></div>` : ""}
            ${c.email ? `<div class="receipt-row"><span>Email</span><span>${c.email}</span></div>` : ""}
            ${c.phone ? `<div class="receipt-row"><span>Telefon</span><span>${c.phone}</span></div>` : ""}`;
        box.innerHTML = `
            <div class="receipt">
                <div class="receipt-head">
                    <strong style="font-size:18px;">Fog · Carport</strong>
                    <span style="color:#777;">Forespørgsel</span>
                </div>
                <hr class="receipt-line">
                <div class="receipt-row"><span>Tagtype</span><span>${roofTypeDa(sp.roofType)}</span></div>
                <div class="receipt-row"><span>Carport</span><span>${sp.widthCm} × ${sp.lengthCm} cm</span></div>
                <div class="receipt-row"><span>Redskabsrum</span><span>${shed}</span></div>
                ${kontaktRows}
            </div>`;
        const hint = document.getElementById("login-hint");
        if (hint) hint.textContent = isLoggedIn()
            ? "Du er logget ind — forespørgslen knyttes til din konto. En medarbejder ser på den og kontakter dig."
            : "Udfyld kontaktoplysninger (mindst fornavn + telefon), eller log ind, for at sende. En medarbejder ser på din forespørgsel og kontakter dig.";
    }

    const sendBtn = document.getElementById("send-request");
    if (sendBtn) sendBtn.addEventListener("click", async () => {
        const status = document.getElementById("request-status");
        const spec = readSpec();
        const contact = readContact();
        const payload = { ...spec, ...contact };
        if (!spec.widthCm || !spec.lengthCm) {
            status.innerHTML = "<span style='color:#b4231f;'>Vælg bredde og længde først.</span>"; return;
        }
        if (!isLoggedIn()) {

            const required = ["firstName", "phone"];
            if (contact.email) required.push("email");
            const result = window.ContactValidation.validateContact(contact, required);
            if (!result.ok) {
                const first = Object.values(result.errors)[0];
                status.innerHTML = `<span style='color:#b4231f;'>${first}</span>`;
                return;
            }
        }
        try {
            const res = await fetch("/api/orders", {
                method: "POST", headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify(payload)
            });
            if (res.status === 409) {

                const data = await res.json().catch(() => ({}));
                status.innerHTML = `<span style="color:#b4231f;">${data.message || "Denne email er allerede registreret."}</span>
                    <a href="/Login.html" class="picker-btn" style="margin-left:10px;text-decoration:none;"><span>Log ind her</span></a>`;
                return;
            }
            if (!res.ok) throw new Error(await res.text());
            const r = await res.json();
            if (r.accountCreated) {

                status.innerHTML = `
                    <div class="receipt" style="margin-top:6px;">
                        <strong>Tak! Forespørgsel sendt (nr. ${r.orderId}).</strong>
                        <p style="margin-top:8px;">Vi har oprettet en konto til dig, så du kan følge din ordre:</p>
                        <div class="receipt-row"><span>Email</span><span>${r.email}</span></div>
                        <div class="receipt-row"><span>Adgangskode</span><span><strong>${r.tempPassword}</strong></span></div>
                        <p style="margin-top:8px;font-size:13px;color:#777;">Log ind og skift adgangskoden under Mine oplysninger.</p>
                        <a href="/Login.html" class="picker-btn" style="margin-top:8px;text-decoration:none;"><span>Log ind</span></a>
                    </div>`;
            } else {
                const link = isLoggedIn() ? ` Følg den under <a href="/MyOrders.html">Mine ordrer</a>.` : " Vi kontakter dig.";
                status.innerHTML = `Tak! Forespørgsel sendt (nr. ${r.orderId}).${link}`;
            }
        } catch (e) {
            status.innerHTML = `<span style='color:#b4231f;'>${e.message}</span>`;
        }
    });

    const prev = document.getElementById("done-previous");
    if (prev) prev.addEventListener("click", () => { if (typeof personalPage === "function") personalPage(); });

    const doneBtn = document.getElementById("done-btn");
    if (doneBtn) doneBtn.addEventListener("click", () => setTimeout(renderSummary, 0));
    const personalNext = document.getElementById("personal-next");
    if (personalNext) personalNext.addEventListener("click", () => setTimeout(renderSummary, 0));

    function updateNav() {
        const nav = document.getElementById("nav-links");
        if (!nav) return;
        if (isLoggedIn()) {
            nav.innerHTML =
                '<a href="/MyOrders.html">Mine ordrer</a>' +
                '<a href="#" id="nav-logout">Log ud</a>';
            const out = document.getElementById("nav-logout");
            if (out) out.addEventListener("click", async (e) => {
                e.preventDefault();
                try { await fetch("/api/logout", { method: "POST" }); } catch (err) {}
                sessionStorage.clear();

                ["fname", "lname", "address", "postalcode", "city", "email", "phone-number"]
                    .forEach(id => { const el = document.getElementById(id); if (el) el.value = ""; });
                updateNav();
                setupContactStep();
                renderSummary();
            });
        } else {
            nav.innerHTML = '<a href="/Login.html" id="nav-login">Log ind</a>';
        }
    }
    updateNav();
    setupContactStep();

    if (window.ContactValidation) {
        window.ContactValidation.digitsOnly(document.getElementById("postalcode"));
        window.ContactValidation.digitsOnly(document.getElementById("phone-number"));
    }
})();
