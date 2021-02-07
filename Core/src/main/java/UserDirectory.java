import java.nio.file.Path;

public class UserDirectory extends AbstractMessage {
    private String dir;

    public String getDir() {
        return dir;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public UserDirectory(String path) {
        this.dir = path;
    }
}
