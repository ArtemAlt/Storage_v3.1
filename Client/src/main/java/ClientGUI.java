import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientGUI extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/ClientGUI.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Storage");
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(  event ->
                System.exit(0));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
