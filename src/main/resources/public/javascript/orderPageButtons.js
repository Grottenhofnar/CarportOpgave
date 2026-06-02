const carportNext = document.getElementById("carport-next");

const shedNext = document.getElementById("shed-next");
const shedPrevious = document.getElementById("shed-previous");

const personalNext = document.getElementById("personal-next");
const personalPrevious = document.getElementById("personal-previous");

carportNext.addEventListener("click", () => {
    if (carportNext.value === "shed") {
        shedPage()
    }
})

shedNext.addEventListener("click", () => {
    if (shedNext.value === "personal") {
        personalPage()
    }
})

shedPrevious.addEventListener("click", () => {
    if (shedPrevious.value === "carport") {
        carportPage()
    }
})

personalNext.addEventListener("click", () => {
    if (personalNext.value === "end") {
        donePage()
    }
})

personalPrevious.addEventListener("click", () => {
    if (personalPrevious.value === "shed") {
        shedPage()
    }
})
