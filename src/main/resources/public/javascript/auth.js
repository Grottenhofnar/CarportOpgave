document.getElementById("login-btn").addEventListener("click", async () => {
    const status = document.getElementById("login-status");
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    if (!email || !password) { status.textContent = "Udfyld email og adgangskode."; return; }
    try {
        const res = await fetch("/api/login", { method: "POST", headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: JSON.stringify({ email, password }) });
        if (!res.ok) { status.textContent = "Forkert email eller adgangskode."; return; }
        const u = await res.json();
        sessionStorage.setItem("email", u.email);
        sessionStorage.setItem("role", u.role);
        sessionStorage.setItem("profile", JSON.stringify(u));
        window.location.href = u.role === "admin" ? "/Admin.html" : "/MyOrders.html";
    } catch (e) { status.textContent = e.message; }
});

document.getElementById("toggle").addEventListener("click", (e) => {
    e.preventDefault(); document.getElementById("signup").classList.toggle("hidden");
});

document.getElementById("signup-btn").addEventListener("click", async () => {
    const status = document.getElementById("signup-status");
    const payload = {
        email: document.getElementById("su-email").value.trim(),
        password: document.getElementById("su-password").value,
        firstName: document.getElementById("su-firstname").value.trim(),
        lastName: document.getElementById("su-lastname").value.trim(),
        address: document.getElementById("su-address").value.trim(),
        postalCode: document.getElementById("su-postalcode").value.trim(),
        city: document.getElementById("su-city").value.trim(),
        phone: document.getElementById("su-phone").value.trim()
    };
    if (!payload.email || !payload.password) { status.textContent = "Udfyld email og adgangskode."; return; }
    try {
        const res = await fetch("/api/signup", { method: "POST", headers: { "Content-Type": "application/json" },
            credentials: "same-origin",
            body: JSON.stringify(payload) });
        status.textContent = res.ok ? "Bruger oprettet — log ind ovenfor." : await res.text();
    } catch (e) { status.textContent = e.message; }
});
