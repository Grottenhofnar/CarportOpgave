
const saveCarportRoof = document.getElementById("carport-roof");
const saveCarportWidth = document.getElementById("carport-width");
const saveCarportLength = document.getElementById("carport-length");

const saveShedChoice = document.getElementById("shed-choice");
const saveShedWidth = document.getElementById("shed-width");
const saveShedLength = document.getElementById("shed-length");

const saveFirstName = document.getElementById("fname");
const saveLastName = document.getElementById("lname");
const saveAddress = document.getElementById("address");
const savePostalCode = document.getElementById("postalcode");
const saveCity = document.getElementById("city");
const saveEmail = document.getElementById("email");
const savePhoneNumber = document.getElementById("phone-number");
const saveMessage = document.getElementById("message");

saveCarportRoof.addEventListener("input", () => {
    localStorage.setItem('carportRoofType', JSON.stringify(saveCarportRoof.value));
})
saveCarportWidth.addEventListener("input", () => {
    localStorage.setItem('carportWidth', saveCarportWidth.value);
})
saveCarportLength.addEventListener("input", () => {
    localStorage.setItem('carportLength', saveCarportLength.value);
})

saveShedChoice.addEventListener("input", () => {
    localStorage.setItem('shedChoice', JSON.stringify(saveShedChoice.value));
})
saveShedWidth.addEventListener("input", () => {
    localStorage.setItem('shedWidth', saveShedWidth.value);
})
saveShedLength.addEventListener("input", () => {
    localStorage.setItem('shedLength', saveShedLength.value);
})

saveFirstName.addEventListener("input", () => {
    localStorage.setItem('firstName', JSON.stringify(saveFirstName.value));
})
saveLastName.addEventListener("input", () => {
    localStorage.setItem('lastName', JSON.stringify(saveLastName.value));
})
saveAddress.addEventListener("input", () => {
    localStorage.setItem('address', JSON.stringify(saveAddress.value));
})
savePostalCode.addEventListener("input", () => {
    localStorage.setItem('postalCode', savePostalCode.value);
})
saveCity.addEventListener("input", () => {
    localStorage.setItem('city', JSON.stringify(saveCity.value));
})
saveEmail.addEventListener("input", () => {
    localStorage.setItem('email', JSON.stringify(saveEmail.value));
})
savePhoneNumber.addEventListener("input", () => {
    localStorage.setItem('phoneNumber', JSON.stringify(savePhoneNumber.value));
})
saveMessage.addEventListener("input", () => {
    localStorage.setItem('message', JSON.stringify(saveMessage.value));
})
