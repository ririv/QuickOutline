package com.ririv.quickoutline.view;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ririv.quickoutline.di.AppModule;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;


public class App extends Application {

    private Injector injector;

    @Override
    public void init() {
        injector = Guice.createInjector(new AppModule());
    }

//      注意javafx程序架子顺序：main启动程序，加载fxml，fxml加载指定的controller
        @Override
        public void start(Stage stage) throws IOException {
            ResourceBundle bundle = LocalizationManager.getResourceBundle();

            FXMLLoader fxmlLoader = new FXMLLoader(
                    getClass().getResource("MainView.fxml"),
                    bundle
            );

            fxmlLoader.setControllerFactory(injector::getInstance);

            Parent root = fxmlLoader.load();

            Scene scene = new Scene(root, 800, 600);


//            stage.setResizable(false); //不可调整大小
            stage.setTitle(bundle.getString("app.title"));
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
