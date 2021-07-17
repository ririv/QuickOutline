import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;


public class FxmlTest extends Application {


    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader();

        URL url = getClass().getResource("MainView.fxml");


        fxmlLoader.setLocation(url);
        Parent content = fxmlLoader.load();
        // TreeItem名字和图标
        TreeItem<String> rootItem = new TreeItem<> ();
        rootItem.setExpanded(true);
        // 每个Item下又可以添加新的Item
        for (int i = 1; i < 6; i++) {
            TreeItem<String> item = new TreeItem<> ("Message" + i);
            item.getChildren().add(new TreeItem<>("第三级"));
            rootItem.getChildren().add(item);
        }
        // 创建TreeView
        TreeView<String> tree = new TreeView<> (rootItem);

        VBox root = new VBox();
        root.getChildren().add(tree);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
