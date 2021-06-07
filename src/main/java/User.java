import java.util.HashMap;
import java.util.Map;

public class User {
    static final String ONLINE = "ONLINE";
    static final String OFFLINE = "OFFLINE";

    String UserName;
    String UserPassword;
    String UserStatus;

    User(String UserName, String UserPassword){
        this.UserName = UserName;
        this.UserPassword = UserPassword;
        this.UserStatus = OFFLINE;
    }
}
