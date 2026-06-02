saveCarportRoof.value = localStorage.getItem("carportRoofType")
    ? JSON.parse(localStorage.getItem("carportRoofType"))
    : "rooftype";

saveCarportWidth.value = localStorage.getItem("carportWidth") || "width";

saveCarportLength.value = localStorage.getItem("carportLength") || "length";

saveShedChoice.value = localStorage.getItem("shedChoice")
    ? JSON.parse(localStorage.getItem("shedChoice"))
    : "answer";

saveShedWidth.value = localStorage.getItem("shedWidth") || "width";

saveShedLength.value = localStorage.getItem("shedLength") || "length";

saveFirstName.value = localStorage.getItem("firstName")
    ? JSON.parse(localStorage.getItem("firstName"))
    : "";

saveLastName.value = localStorage.getItem("lastName")
    ? JSON.parse(localStorage.getItem("lastName"))
    : "";

saveAddress.value = localStorage.getItem("address")
    ? JSON.parse(localStorage.getItem("address"))
    : "";

savePostalCode.value = localStorage.getItem("postalCode") || "";

saveCity.value = localStorage.getItem("city")
    ? JSON.parse(localStorage.getItem("city"))
    : "";

saveEmail.value = localStorage.getItem("email")
    ? JSON.parse(localStorage.getItem("email"))
    : "";

savePhoneNumber.value = localStorage.getItem("phoneNumber")
    ? JSON.parse(localStorage.getItem("phoneNumber"))
    : "";

saveMessage.value = localStorage.getItem("message")
    ? JSON.parse(localStorage.getItem("message"))
    : "";
