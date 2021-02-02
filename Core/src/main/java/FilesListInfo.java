public class FilesListInfo extends AbstractMessage {
    private String name;
    private String type;
    private String date;
    private String size;

    public FilesListInfo(String name, String type, String date, String size) {
        this.name = name;
        this.type = type;
        this.date = date;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }
}
