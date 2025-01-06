package com.ririv.quickoutline.view;//package com.ririv.contents.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;


public class App extends Application {

//      注意javafx程序架子顺序：main启动程序，加载fxml,fxml加载指定的controller
        @Override
        public void start(Stage stage) throws IOException {
            FXMLLoader fxmlLoader = new FXMLLoader();

            URL url = getClass().getResource("MainView.fxml");
            fxmlLoader.setLocation(url);
            Parent content = fxmlLoader.load();


            Scene scene = new Scene(content);


//            stage.setResizable(false); //不可调整大小
            stage.setTitle("QuickOutline - 编辑与添加PDF目录");
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icon/icon.png"))));
            stage.setScene(scene);
            stage.show();
        }


    public static void main(String[] args) {
        try {
//            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            launch(args);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // 确保以非零退出时给出明确错误信息
        }

    }

}
