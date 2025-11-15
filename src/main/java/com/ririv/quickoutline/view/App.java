package com.ririv.quickoutline.view;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ririv.quickoutline.di.AppModule;
import com.ririv.quickoutline.service.syncWithExternelEditor.SyncWithExternalEditorService;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
        // 获取 SyncWithExternalEditorService 实例并调用 shutdown
        SyncWithExternalEditorService editorService = injector.getInstance(SyncWithExternalEditorService.class);
        if (editorService != null) {
            editorService.shutdown();
        }
        // 主动释放当前文件会话资源（PDDocument/渲染线程等）
        try {
            com.ririv.quickoutline.view.state.CurrentFileState currentFileState = injector.getInstance(com.ririv.quickoutline.view.state.CurrentFileState.class);
            if (currentFileState != null) {
                currentFileState.close();
            }
        } catch (Exception ignore) {
        }
        super.stop(); // 调用父类的 stop 方法
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
