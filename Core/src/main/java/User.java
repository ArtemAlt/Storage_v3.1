public class User extends AbstractMessage {
    private String login;
    private String password;
    private boolean auth;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public User(String login, String password) {
        this.login = login;
        this.password = password;
        this.auth=false;
    }
}
