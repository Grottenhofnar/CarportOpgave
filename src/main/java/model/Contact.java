package model;

public class Contact {
    public String firstName;
    public String lastName;
    public String address;
    public String postalCode;
    public String city;
    public String email;
    public String phone;
    public String message;

    public boolean hasMinimum() {
        return firstName != null && !firstName.isBlank()
                && phone != null && !phone.isBlank();
    }
}
