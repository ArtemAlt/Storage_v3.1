public class AuthUser extends AbstractMessage {
    private User user;

    public User getUser() {
        return user;
    }

    public AuthUser(User user) {
        this.user = user;
    }
}
