
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class ListRequest extends AbstractMessage {
    private String path;

    public ListRequest(String path) {
        this.path = path;
    }

    public String getPath() {
        log.debug("Current packaging path " + path);
        return path;
    }
}
