package com.ririv.quickoutline.view;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ririv.quickoutline.di.AppModule;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.slf4j.LoggerFactory.getLogger;


public class App extends Application {

    private Injector injector;

    private MainController mainController; // 你需要持有主控制器的引用

    private static final Logger logger = getLogger(App.class);

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
            this.mainController = fxmlLoader.getController();

            Scene scene = new Scene(root, 1280, 800);


//            stage.setResizable(false); //不可调整大小
            stage.setTitle(bundle.getString("app.title"));

//            javafx 25预览特性 https://gist.github.com/mstr2/0befc541ee7297b6db2865cc5e4dbd09
//            stage.initStyle(StageStyle.EXTENDED);
            stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("images/icon.png"))));
            stage.setScene(scene);
            stage.show();
        }

    @Override
    public void stop() throws Exception {
        logger.info("Application stopping...");

        // 1. 【新增】关闭主控制器 (触发 MarkdownTabController -> LocalWebServer 的关闭)
        if (mainController != null) {
            try {
                mainController.dispose();
            } catch (Exception e) {
                logger.error("Failed to dispose MainController", e);
            }
        }

        // 2. 关闭外部编辑器同步服务
        if (injector != null) {
            try {
                SyncWithExternalEditorService editorService = injector.getInstance(SyncWithExternalEditorService.class);
                if (editorService != null) {
                    editorService.shutdown();
                }
            } catch (Exception ignore) {}

            // 3. 释放文件会话资源
            try {
                CurrentFileState currentFileState = injector.getInstance(com.ririv.quickoutline.view.state.CurrentFileState.class);
                if (currentFileState != null) {
                    currentFileState.close();
                }
            } catch (Exception ignore) {}
        }

        super.stop();

        logger.info("JVM Force Exit.");

        // 【核心代码】 0 表示正常退出
        // 这行代码会强制杀死所有残留线程（包括 WebServer 的线程），彻底关闭程序
         System.exit(0);
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
