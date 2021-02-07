public class FileDelete extends AbstractMessage{
    private String fileName;

    public String getFilename() {
        return fileName;
    }

    public FileDelete(String filename) {
        this.fileName = filename;
    }

    public String getFileName() {
        return fileName;
    }
}
