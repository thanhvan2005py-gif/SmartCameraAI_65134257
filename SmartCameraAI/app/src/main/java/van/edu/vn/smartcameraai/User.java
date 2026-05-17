package van.edu.vn.smartcameraai;

public class User {
    public String name;
    public String email;
    public String uid;

    public User() {
        // Constructor mặc định cho Firebase
    }

    public User(String name, String email, String uid) {
        this.name = name;
        this.email = email;
        this.uid = uid;
    }
}