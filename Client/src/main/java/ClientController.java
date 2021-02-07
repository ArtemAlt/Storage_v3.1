import io.netty.handler.codec.serialization.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    public Path userSerPathParent;
    public ComboBox<String> clDiskBox;
    public Button serverUpdate;
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
        serViewList.getSortOrder().add(serColType);
        clColDate.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDate().format(dtf)));
        clViewList.getSortOrder().add(clColType);


    }

    public void aught() {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Authorization");
        dialog.setHeaderText("Please enter login and password");
        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        final TextField[] username = {new TextField()};
        username[0].setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username[0], 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);
        username[0].textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(() -> username[0].requestFocus());

        PasswordField finalPassword = password;
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                user = username[0].getText();
                userPassword = finalPassword.getText();
                return new Pair<>(username[0].getText(), finalPassword.getText());
            }
            return null;
        });
        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(usernamePassword -> {
            System.out.println("Username=" + usernamePassword.getKey() +
                    ", Password=" + usernamePassword.getValue());
            System.out.println(user + userPassword);
        });

    }

    public void initialize(URL location, ResourceBundle resources) {
        prepareColumns();
        aught();
        serViewList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path p = userSerPath.resolve(serViewList.getSelectionModel().getSelectedItem().getName());
                if (Files.isDirectory(p)) {
                   // updateSerViewList(p); todo: смена папки на сервере - если это папка? сменить текущую папку на сервере на эту и отобразить на клиенте
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
            System.err.println(user + userPassword);
            Thread t = new Thread(() -> {
                try {
                    getServerPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                updateServerInfo();
                updateClViewList(Paths.get(clViewPath.getText()));
                log.debug("Current user path -" + userSerPath);
                log.debug("Current text field - " + serViewPath.getText());
                while (true) {
                    try {
                        AbstractMessage msg = (AbstractMessage) is.readObject();

                        if (msg instanceof ServerList) {
                            ServerList list = (ServerList) msg;
                            updateUI(() -> {
                                log.debug(" Client received -  "+ list.toString());
                                serViewList.getItems().clear();
                                serViewList.getItems().addAll(list.getServerList());

                            });
                        } else if (msg instanceof FileMessage) {
                            FileMessage file = (FileMessage) msg;
                            Path p = Paths.get(clViewPath.getText()).resolve(file.getFileName());
                            Files.write(p, file.getData());
                            updateServerInfo();
                            updateClViewList(Paths.get(clViewPath.getText()));

                        } else if (msg instanceof UserDirectory) {
                            UserDirectory dir = (UserDirectory) msg;
                            log.debug("User path from server - " + dir.getDir());
                            userSerPath = Paths.get(dir.getDir());
                            serViewPath.setText(dir.getDir());

                        } else if (msg instanceof ParentUserDirectory) {
                            ParentUserDirectory dir = (ParentUserDirectory) msg;
                            log.debug("Parent user path from server - " + dir.getDir());
                            userSerPathParent = Paths.get(dir.getDir());
                        }
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
        updateServerInfo();
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
        updateServerInfo();
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

//    public void btnSerPathUp(ActionEvent actionEvent) {
//        try {
//            getParentServerPath();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        Path upPath = userSerPathParent;
//        if (upPath != null) {
//            updateSerViewList(upPath);
//        }
//    }

    public void btnClPathUp(ActionEvent actionEvent) {
        Path upPath = Paths.get(clViewPath.getText()).getParent();
        if (upPath != null) {
            updateClViewList(upPath);
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
    public void disconnect(){
        try {
            os.writeObject(new ExitRequest());
            os.flush();
            log.debug("Send exit request ");
        } catch (IOException e) {
            log.error("e = ", e);
            e.printStackTrace();
        }
    }
    public void updateServerInfo() {
        try {
            os.writeObject(new ListRequest());
            os.flush();
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
            updateServerInfo();
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
                updateServerInfo();
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
           if (newFile.createNewFile()){
               Alert a = new Alert(Alert.AlertType.INFORMATION, "Creating complete", ButtonType.OK);
               a.showAndWait();
               updateClViewList(getClCurrentPath());
           } else {
               Alert a = new Alert(Alert.AlertType.WARNING, "Creating error", ButtonType.OK);
               a.showAndWait();
           }
        } catch (IOException e) {
            log.debug("e= ",e);
        }

    }
}
