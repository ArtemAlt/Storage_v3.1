import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class FilesListInfo extends AbstractMessage {


    public enum FileType {
        FILE("F"), DIRECTORY("D");
        private String name;

        public String getName() {
            return name;
        }

        FileType(String name) {
            this.name = name;
        }
    }

    private String name;
    private FileType type;
    private LocalDateTime date;
    private long size;

    public FilesListInfo(Path path) {
        try {
            this.name = path.getFileName().toString();
            this.size = Files.size(path);
            this.type = Files.isDirectory(path)? FileType.DIRECTORY : FileType.FILE;
            if (this.type == FileType.DIRECTORY){
                this.size=-1L;
            }
            this.date = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
            } catch (IOException e) {
            throw  new RuntimeException("Can not update "+path);
        }


    }
    public FilesListInfo getFilesListInfo(){
        return this;
    }






    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName() + getType().toString()+getDate().toString()+getSize();

    }

    public void setName(String name) {
        this.name = name;
    }

    public FileType getType() {
        return type;
    }

    public void setType(FileType type) {
        this.type = type;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
