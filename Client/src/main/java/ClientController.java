import io.netty.handler.codec.serialization.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
public class ClientController implements Initializable {
    public TableColumn<FilesListInfo, String> serColDate;
    public TableColumn<FilesListInfo, String> serColName;
    public TableColumn<FilesListInfo, String> serColType;
    public TableColumn<FilesListInfo, Long> serColSize;
    public TableView<FilesListInfo> serViewList;
    public TextField serViewPath;
    public TableColumn<FilesListInfo, String> clColDate;
    public TableColumn<FilesListInfo, String> clColName;
    public TableColumn<FilesListInfo, String> clColType;
    public TableColumn<FilesListInfo, Long> clColSize;
    public TableView<FilesListInfo> clViewList;
    public TextField clViewPath;
    public Path userSerPath;
    public Path currentUserServPath;
    public Path userSerPathParent;
    public ComboBox<String> clDiskBox;
    public Button serverUpdate;
    public TextField login;
    public TextField password;
    public Button btnConnect;
    public HBox boxServer;
    private ObjectDecoderInputStream is;
    private ObjectEncoderOutputStream os;
    private String user;
    private String userPassword;

    public void prepareColumns() {
        serColType.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        serColName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        serColSize.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        serColSize.setCellFactory(column -> new TableCell<FilesListInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%,d bytes", item);
                    if (item == -1L) {
                        text = "[DIR]";
                    }
                    setText(text);
                }
            }
        });
        clDiskBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            clDiskBox.getItems().add(p.toString());
        }
        clDiskBox.getSelectionModel().select(0);
        clColType.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        clColName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        clColSize.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        clColSize.setCellFactory(column -> new TableCell<FilesListInfo, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    String text = String.format("%,d bytes", item);
                    if (item == -1L) {
                        text = "[DIR]";
                    }
                    setText(text);
                }
            }
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        serColDate.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDate().format(dtf)));
        clColDate.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDate().format(dtf)));
        clViewList.getSortOrder().add(clColType);

        serViewList.getSortOrder().add(serColType);

    }

    public void initialize(URL location, ResourceBundle resources) {
        prepareColumns();
        serverUpdate.setVisible(false);
//        aught();
        serViewList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path p = Paths.get(serViewPath.getText()).resolve(serViewList.getSelectionModel().getSelectedItem().getName());
                log.debug("Action on chek " + p.toString());
                if (Files.isDirectory(p)) {
                    updateServerInfo(p.toString());
                    serViewPath.setText(p.toString());
                    log.debug("Action " + p.toString());
                }
            }
        });
        clViewList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path p = Paths.get(clViewPath.getText()).resolve(clViewList.getSelectionModel().getSelectedItem().getName());
                if (Files.isDirectory(p)) {
                    updateClViewList(p);
                }
            }
        });
        try {
            Socket socket = new Socket("localhost", 8189);
            os = new ObjectEncoderOutputStream(socket.getOutputStream());
            is = new ObjectDecoderInputStream(socket.getInputStream());

            Thread t = new Thread(() -> {


//                try {
//
//                    getServerPath();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                updateClViewList(Paths.get(clViewPath.getText()));

                while (true) {
                    try {
                        AbstractMessage msg = (AbstractMessage) is.readObject();

                        if (msg instanceof ServerList) {
                            ServerList list = (ServerList) msg;
                            updateUI(() -> {
                                log.debug(" Client received -  " + list.toString());
                                serViewList.getItems().clear();
                                serViewList.getItems().addAll(list.getServerList());

                            });
                        } else if (msg instanceof FileMessage) {
                            FileMessage file = (FileMessage) msg;
                            Path p = Paths.get(clViewPath.getText()).resolve(file.getFileName());
                            Files.write(p, file.getData());
                            updateServerInfo(clViewPath.getText());
                            updateClViewList(Paths.get(clViewPath.getText()));

                        } else if (msg instanceof UserDirectory) {
                            UserDirectory dir = (UserDirectory) msg;
                            log.debug("User path from server - " + dir.getDir());
                            userSerPath = Paths.get(dir.getDir());
                            serViewPath.setText(dir.getDir());
                            log.debug("User path userServerPath " + userSerPath.toString());
                            if (serViewList.getItems().isEmpty()) {
                                updateServerInfo(userSerPath.toString());
                            }

                        } else if (msg instanceof ParentUserDirectory) {
                            ParentUserDirectory dir = (ParentUserDirectory) msg;
                            log.debug("Parent user path from server - " + dir.getDir());
                            userSerPath = Paths.get(dir.getDir());
                            serViewPath.setText(dir.getDir());
                            log.debug("Parent user path Parent UserServerPath " + userSerPath.toString());
                            updateServerInfo(userSerPath.toString());
                        }
                        log.debug("Current user path -" + userSerPath);
                        log.debug("Current text field - " + serViewPath.getText());

                    } catch (ClassNotFoundException | IOException e) {
                        log.debug("Error of serializing message ");
                    }
                }
            });
            t.setDaemon(true);
            t.start();

        } catch (
                IOException e) {
            log.error("e = ", e);
            Alert a = new Alert(Alert.AlertType.ERROR, "Server connection error", ButtonType.OK);
            a.showAndWait();
        }

    }

    public void getServerPath() throws IOException {
        os.writeObject(new DirectoryRequest());
        os.flush();
        log.debug("Send request for user directory");
    }

    public void getParentServerPath() throws IOException {
        os.writeObject(new ParentDirectoryRequest());
        os.flush();
        log.debug("Send request for user parent directory");
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        if (!clViewList.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Select file from PC", ButtonType.OK);
            alert.showAndWait();
        }
        Path p = Paths.get(clViewPath.getText()).resolve(clViewList.getSelectionModel().getSelectedItem().getName());
        log.debug("Selected client side- " + p.toString());
        os.writeObject(new FileMessage(p));
        os.flush();
        updateServerInfo(clViewPath.getText());
        updateClViewList(Paths.get(clViewPath.getText()));
    }

    public void download(ActionEvent actionEvent) throws IOException {
        if (!serViewList.isFocused()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Select file from Server", ButtonType.OK);
            alert.showAndWait();
        }
        log.debug("Selected server side- " + serViewList.getSelectionModel().getSelectedItem().getName());
        String fileName = serViewList.getSelectionModel().getSelectedItem().getName();
        os.writeObject(new FileRequest(fileName));
        os.flush();
        updateServerInfo(clViewPath.getText());
        updateClViewList(Paths.get(clViewPath.getText()));
    }


    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }


    public void updateClViewList(Path path) {
        updateUI(() -> {
                    try {
                        clViewPath.setText(path.normalize().toAbsolutePath().toString());
                        clViewList.getItems().clear();
                        clViewList.getItems().addAll(Files.list(path).map(FilesListInfo::new).collect(Collectors.toList()));
                        clViewList.sort();
                    } catch (IOException e) {
                        log.error("e = ", e);
                        Alert a = new Alert(Alert.AlertType.WARNING, "Can not update interface ", ButtonType.OK);
                        a.showAndWait();
                    }
                }
        );
    }

    public void btnClPathUp(ActionEvent actionEvent) {
        Path upPath = Paths.get(clViewPath.getText()).getParent();
        if (upPath != null) {
            updateClViewList(upPath);
        }
    }

    public void btnServerUP(ActionEvent actionEvent) {
        try {
            os.writeObject(new ParentDirectoryRequest());
            os.flush();
            log.debug("Request parent directory");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void selectDisk(ActionEvent a) {
        ComboBox<String> element = (ComboBox<String>) a.getSource();
        updateClViewList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public Path getSerCurrentPath() {
        return Paths.get(serViewPath.getText());
    }

    public Path getClCurrentPath() {
        return Paths.get(clViewPath.getText());
    }

    public void disconnect() {
        try {
            os.writeObject(new ExitRequest());
            os.flush();
            log.debug("Send exit request ");
        } catch (IOException e) {
            log.error("e = ", e);
            e.printStackTrace();
        }
    }

    public void updateServerInfo(String path) {
        try {
            os.writeObject(new ListRequest(path));
            os.flush();
            log.debug("From client to server request " + path);
        } catch (IOException e) {
            log.error("e = ", e);
            e.printStackTrace();
        }

    }

    public void btnExit(ActionEvent actionEvent) {
        disconnect();
        Platform.exit();
    }

    public void bntServerReload(ActionEvent actionEvent) {
        try {
            updateServerInfo(clViewPath.getText());
            getServerPath();
        } catch (IOException e) {
            log.error("e=", e);
        }
    }

    public void deletePC(ActionEvent actionEvent) {
        if (clViewList.isFocused()) {
            Path p = getClCurrentPath().resolve(clViewList.getSelectionModel().getSelectedItem().getName());
            try {
                Files.delete(p);
                updateClViewList(getClCurrentPath());
            } catch (IOException e) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Deleting error", ButtonType.OK);
                a.showAndWait();
            }
        } else {
            String fileName = serViewList.getSelectionModel().getSelectedItem().getName();
            try {
                os.writeObject(new FileDelete(fileName));
                os.flush();
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Deleting complete", ButtonType.OK);
                a.showAndWait();
                updateServerInfo(clViewPath.getText());
            } catch (IOException e) {
                Alert a = new Alert(Alert.AlertType.WARNING, "Deleting error", ButtonType.OK);
                a.showAndWait();
            }


        }
    }

    public void makeDir(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("Name");
        dialog.setTitle("Creat the directory");
        dialog.setHeaderText("Enter directory name");
        dialog.setContentText("Please enter name:");
        Optional<String> result = dialog.showAndWait();
        File dir = new File(Paths.get(clViewPath.getText(), result.get()).toString());
        if (dir.mkdir()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Creating complete", ButtonType.OK);
            a.showAndWait();
            updateClViewList(getClCurrentPath());
        } else {
            Alert a = new Alert(Alert.AlertType.WARNING, "Creating error", ButtonType.OK);
            a.showAndWait();
        }
    }

    public void creatFile(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog("Creat");
        dialog.setTitle("Creat the file");
        dialog.setHeaderText("Enter file name");
        dialog.setContentText("Please enter file name:");
        Optional<String> result = dialog.showAndWait();
        File newFile = new File(Paths.get(clViewPath.getText(), result.get()).toString());
        try {
            if (newFile.createNewFile()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Creating complete", ButtonType.OK);
                a.showAndWait();
                updateClViewList(getClCurrentPath());
            } else {
                Alert a = new Alert(Alert.AlertType.WARNING, "Creating error", ButtonType.OK);
                a.showAndWait();
            }
        } catch (IOException e) {
            log.debug("e= ", e);
        }

    }


    public void connect(ActionEvent actionEvent) {
        User user = new User(login.getText(), password.getText());
        try {
            os.writeObject(user);
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverUpdate.setVisible(true);


    }
}
