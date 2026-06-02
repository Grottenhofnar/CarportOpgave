const shedChoice = document.getElementById("shed-choice");
const hidden = document.getElementsByClassName("dropdowns-h");

shedChoice.addEventListener("change", () => {
    if (shedChoice.value === "yes") {
        for (let i = 0; i < hidden.length; i++) {
            hidden[i].classList.remove("hidden");
            hidden[i].classList.add("visible");
        }
    }
    if (shedChoice.value === "no") {
        personalPage();
        for (let i = 0; i < hidden.length; i++) {
            hidden[i].classList.remove("visible");
            hidden[i].classList.add("hidden");
        }
    }
});
