import java.nio.file.Path;

public class ParentUserDirectory extends AbstractMessage {
    private String dir;

    public String getDir() {
        return dir;
    }

    public ParentUserDirectory(String path) {
        this.dir = path;
    }
}
