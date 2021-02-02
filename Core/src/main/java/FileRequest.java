
public class FileRequest extends AbstractMessage {
    private String fileName;

    public String getFilename() {
        return fileName;
    }

    public FileRequest(String filename) {
        this.fileName = filename;
    }
}
