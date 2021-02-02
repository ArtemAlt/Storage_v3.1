import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class FilesList extends AbstractMessage {
    private final List<String> filesList;

    public FilesList(Path dir) throws IOException {
        String r = Files.probeContentType(dir.getFileName());
        filesList = Files.list(dir)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    public List<String> getFilesList() {
        return filesList;
    }
}
