package model;

public class User {

    public int userId;
    public String username;
    public String password;
    public String firstName;
    public String lastName;
    public String address;
    public String postalCode;
    public String city;
    public String email;
    public String phonenumber;
    public String role;

    public User() {}

    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }
}
