window.addEventListener("load", () => {
    const nav = performance.getEntriesByType("navigation")[0];

    const alreadyReloaded = sessionStorage.getItem("doubleReloadDone");

    if (nav?.type === "reload" && !alreadyReloaded) {

        localStorage.removeItem("carportRoofType");
        localStorage.removeItem("carportWidth");
        localStorage.removeItem("carportLength");

        localStorage.removeItem("shedChoice");
        localStorage.removeItem("shedWidth");
        localStorage.removeItem("shedLength");

        localStorage.removeItem("firstName");
        localStorage.removeItem("lastName");
        localStorage.removeItem("address");
        localStorage.removeItem("postalCode");
        localStorage.removeItem("city");
        localStorage.removeItem("email");
        localStorage.removeItem("phoneNumber");
        localStorage.removeItem("message");

        sessionStorage.setItem("doubleReloadDone", "true");

        window.location.reload();
    }

    if (alreadyReloaded) {
        sessionStorage.removeItem("doubleReloadDone");
    }
})
