const carportBtn = document.getElementById("carport-btn");
const shedBtn = document.getElementById("shed-btn");
const personalBtn = document.getElementById("personal-btn");
const doneBtn = document.getElementById("done-btn");

const carport = document.getElementById("carport");
const shed = document.getElementById("shed");
const personal = document.getElementById("personal");
const done = document.getElementById("done")

function showPage(page) {
    carport.classList.remove("active");
    shed.classList.remove("active");
    personal.classList.remove("active");
    done.classList.remove("active");

    page.classList.add("active");
}

function carportPage() {
    showPage(carport);
    shedBtn.classList.remove("active-page");
    personalBtn.classList.remove("active-page");
    doneBtn.classList.remove("active-page");
    doneBtn.classList.add("not-active");
    carportBtn.classList.add("active-page");
}

function shedPage() {
    showPage(shed);
    carportBtn.classList.remove("active-page");
    personalBtn.classList.remove("active-page");
    doneBtn.classList.remove("active-page");
    doneBtn.classList.add("not-active");
    shedBtn.classList.add("active-page");
}

function personalPage() {
    showPage(personal);
    carportBtn.classList.remove("active-page");
    shedBtn.classList.remove("active-page");
    doneBtn.classList.remove("active-page");
    doneBtn.classList.add("not-active");
    personalBtn.classList.add("active-page");
}

function donePage() {
    showPage(done);
    carportBtn.classList.remove("active-page");
    shedBtn.classList.remove("active-page");
    personalBtn.classList.remove("active-page");
    doneBtn.classList.remove("not-active");
    doneBtn.classList.add("active-page");
}

carportBtn.addEventListener("click", () => {
    carportPage()
});

shedBtn.addEventListener("click", () => {
    shedPage()
});

personalBtn.addEventListener("click", () => {
    personalPage()
});

doneBtn.addEventListener("click", () => {
    donePage()
});
