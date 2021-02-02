import io.netty.handler.codec.serialization.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClientController implements Initializable {
    public ListView<String> clientInfo;
    public ListView<String> serverInfo;
    //    public TableView<FilesListInfo> clientInfo;
//    public TableView<FilesListInfo> serverInfo;
    public Button download;
    public Button upload;
    public Button open;
    public Button exit;
    //    public TableColumn<FilesListInfo, String> clName;
//    public TableColumn<FilesListInfo, String> clDate;
//    public TableColumn<FilesListInfo, String> clType;
//    public TableColumn<FilesListInfo, String> clSize;
//    public TableColumn<FilesListInfo, String> srName;
//    public TableColumn<FilesListInfo, String> srDate;
//    public TableColumn<FilesListInfo, String> srType;
//    public TableColumn<FilesListInfo, String> srSize;
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;
    public Path clientDir;
    final DirectoryChooser dc = new DirectoryChooser();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        clientDir = Paths.get("client_storage");
        clientDir = Paths.get(System.getProperty("user.dir"));

        try {
            Socket socket = new Socket("localhost", 8189);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());
            updateClientInfo();
            updateServerInfo();
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        AbstractMessage msg = (AbstractMessage) is.readObject();
                        if (msg instanceof FilesList) {
                            FilesList list = (FilesList) msg;
                            updateUI(() -> {
                                serverInfo.getItems().clear();
                                serverInfo.getItems().addAll(list.getFilesList());
                            });


                        } else if (msg instanceof FileMessage) {
                            FileMessage file = (FileMessage) msg;
                            Files.write(clientDir.resolve(file.getFileName()), file.getData());
                            updateClientInfo();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setDaemon(true);
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void updateServerInfo() {
        try {
            os.writeObject(new ListRequest());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void updateClientInfo() {
        updateUI(() -> {
            try {
                clientInfo.getItems().clear();
                clientInfo.getItems()
                        .addAll(Files.list(clientDir)
                                .map(p -> p.getFileName().toString())
                                .collect(Collectors.toSet()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = clientInfo.getSelectionModel().getSelectedItem();
        os.writeObject(new FileMessage(clientDir.resolve(fileName)));
        os.flush();
        updateServerInfo();
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = serverInfo.getSelectionModel().getSelectedItem();
        os.writeObject(new FileRequest(fileName));
        os.flush();

    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void openFolder(ActionEvent actionEvent) {
        Stage stage = (Stage) open.getScene().getWindow();
        clientInfo.getItems().clear();
        File dir = dc.showDialog(stage);
        if (dir != null) {
            clientDir = dir.toPath();// не изменяет текущий католог пользователя
            List<File> files = Arrays.asList(Objects.requireNonNull(dir.listFiles()));
            printLog(clientInfo, files);
        }
    }

    private void printLog(ListView<String> list, List<File> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        for (File file : files) {
            list.getItems().addAll(file.getName() + "\n");
        }
    }


}
