
(function () {
    const onlyDigits = (s) => /^[0-9]+$/.test(s);

    function validateField(name, value, opts) {
        opts = opts || {};
        const v = (value || "").trim();
        const required = opts.required;

        switch (name) {
            case "firstName":
            case "lastName":
                if (required && !v) return "Skal udfyldes.";
                if (v && v.length < 2) return "Skal være mindst 2 tegn.";
                return null;

            case "email":
                if (required && !v) return "Email skal udfyldes.";
                if (v && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v))
                    return "Ugyldig email (skal indeholde @ og et domæne).";
                return null;

            case "phone":
                if (required && !v) return "Telefon skal udfyldes.";
                if (v && !onlyDigits(v.replace(/\s/g, "")))
                    return "Telefon må kun indeholde tal.";
                if (v && v.replace(/\s/g, "").length < 8)
                    return "Telefon skal være mindst 8 cifre.";
                return null;

            case "postalCode":
                if (required && !v) return "Postnummer skal udfyldes.";
                if (v && !onlyDigits(v)) return "Postnummer må kun indeholde tal.";
                if (v && v.length !== 4) return "Postnummer skal være 4 cifre.";
                return null;

            case "city":
                if (required && !v) return "By skal udfyldes.";
                return null;

            case "address":
                if (required && !v) return "Adresse skal udfyldes.";
                return null;

            default:
                return null;
        }
    }

    function validateContact(data, requiredFields) {
        requiredFields = requiredFields || [];
        const errors = {};
        ["firstName", "lastName", "email", "phone", "postalCode", "city", "address"].forEach((f) => {
            const msg = validateField(f, data[f], { required: requiredFields.indexOf(f) !== -1 });
            if (msg) errors[f] = msg;
        });
        return { ok: Object.keys(errors).length === 0, errors };
    }

    function digitsOnly(inputEl) {
        if (!inputEl) return;
        inputEl.addEventListener("input", () => {
            const cleaned = inputEl.value.replace(/[^0-9]/g, "");
            if (inputEl.value !== cleaned) inputEl.value = cleaned;
        });
    }

    window.ContactValidation = { validateField, validateContact, digitsOnly };
})();
