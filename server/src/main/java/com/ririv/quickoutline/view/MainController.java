package com.ririv.quickoutline.view;

import com.google.common.eventbus.Subscribe;
import com.ririv.quickoutline.view.utils.LocalizationManager;
import jakarta.inject.Inject;
import com.ririv.quickoutline.view.event.AppEventBus;
import com.ririv.quickoutline.view.event.ShowMessageEvent;
import com.ririv.quickoutline.view.event.ShowSuccessDialogEvent;
import com.ririv.quickoutline.view.event.SwitchTabEvent;
import com.ririv.quickoutline.exception.EncryptedPdfException;
import com.ririv.quickoutline.view.state.CurrentFileState;
import com.ririv.quickoutline.utils.OsDesktopUtil;
import com.ririv.quickoutline.view.controls.message.Message;
import com.ririv.quickoutline.view.controls.message.MessageContainer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static com.ririv.quickoutline.view.MyAlert.showAlert;

public class MainController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MainController.class);
    private final ResourceBundle bundle = LocalizationManager.getResourceBundle();

    public TextField filepathTF;
    public Button browseFileBtn;
    public StackPane root;

    public MessageContainer messageManager;
    public BorderPane leftPane;

    private final CurrentFileState currentFileState;

    private final AppEventBus eventBus;

    @FXML
    private Node bookmarkTabView;
    @FXML
    private Node tocGeneratorTabView;
    @FXML
    private Node pageLabelTabView;
    @FXML
    private Node pdfPreviewTabView;
    @FXML
    private Node markdownTabView;


//    命名规则 必须 是：<fx:id> + "Controller"。
//    因为你现有的 Node 叫 markdownTabView，
//    所以 Controller 的字段名必须叫 markdownTabViewController。
    @FXML
    private MarkdownTabController markdownTabViewController;


    public enum FnTab {
        bookmark, toc, setting, label, preview, tocGenerator, markdown
    }

    private final ObjectProperty<FnTab> currentTabProperty = new SimpleObjectProperty<>(FnTab.bookmark);

    @Inject
    public MainController(CurrentFileState currentFileState, AppEventBus eventBus) {
        this.currentFileState = currentFileState;
        this.eventBus = eventBus;
        // 1. 注册自身为订阅者
        this.eventBus.register(this);
    }

    @FXML
    public void initialize() {
        // 2. 旧的 subscribe 调用已被移除

        // Bind tab visibility to currentTabProperty
        bookmarkTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.bookmark));
        tocGeneratorTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.tocGenerator));
        pageLabelTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.label));
        pdfPreviewTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.preview));
        markdownTabView.visibleProperty().bind(currentTabProperty.isEqualTo(FnTab.markdown));

        // Drag and drop logic
        root.setOnDragOver(event -> {
            String pdfFormatPattern = ".+\\.[pP][dD][fF]$";
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                File file = dragboard.getFiles().getFirst();
                if (file.getName().matches(pdfFormatPattern)) {
                    event.acceptTransferModes(TransferMode.LINK);
                }
            }
        });

        root.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            File file = dragboard.getFiles().getFirst();
            openFile(file);
        });

        browseFileBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("fileChooser.fileTypeText"), "*.pdf"));
            File file = fileChooser.showOpenDialog(null);
            openFile(file);
        });

         filepathTF.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    Path path = currentFileState.getSrcFile();
                    return (path != null) ? path.toString() : "";
                }, currentFileState.srcFileProperty())
         );

    }

    private void openFile(File file) {
        if (file == null) return;

        Path newFilePath = file.toPath();
        Path oldFile = currentFileState.getSrcFile();
        if (oldFile != null && oldFile.equals(newFilePath)) return;

        Thread t = new Thread(() -> {
            try {
                // 后台校验 PDF 是否可打开
                currentFileState.validateFile(newFilePath);
                // 校验通过后设置 srcFile（内部会在需要时切换到 FX 线程）
                currentFileState.setSrcFileValidated(newFilePath);
            } catch (java.io.IOException e) {
                messageManager.showMessage(bundle.getString("message.cannotOpenDoc") + e.getMessage(), Message.MessageType.ERROR);
                logger.error(String.valueOf(e));
            } catch (EncryptedPdfException e) {
                messageManager.showMessage(bundle.getString("message.encryptedDoc"), Message.MessageType.WARNING);
                logger.warn(String.valueOf(e));
            } catch (com.itextpdf.io.exceptions.IOException e) {
                logger.info(String.valueOf(e));
                messageManager.showMessage(bundle.getString("message.corruptedDoc") + e.getMessage(), Message.MessageType.ERROR);
                logger.error(String.valueOf(e));
            }
        }, "open-file-validate");
        t.setDaemon(true);
        t.start();
    }

    // 3. 创建新的、带 @Subscribe 注解的方法
    @Subscribe
    public void onSwitchTab(SwitchTabEvent event) {
        currentTabProperty.set(event.targetTab);
    }

    @Subscribe
    public void onShowMessage(ShowMessageEvent event) {
        messageManager.showMessage(event.message, event.messageType);
    }

    @Subscribe
    public void onShowSuccessDialog(ShowSuccessDialogEvent event) {
        ButtonType openDirAndSelectFileButtonType = new ButtonType(bundle.getString("btnType.openFileLocation"), ButtonBar.ButtonData.OK_DONE);
        ButtonType openFileButtonType = new ButtonType(bundle.getString("btnType.openFile"), ButtonBar.ButtonData.OK_DONE);
        var result = showAlert(Alert.AlertType.INFORMATION,
                bundle.getString("alert.FileSavedAt") + currentFileState.getDestFile().toString(), root.getScene().getWindow(),
                openDirAndSelectFileButtonType, openFileButtonType, new ButtonType("OK", ButtonBar.ButtonData.CANCEL_CLOSE));
        try {
            String destFilePath = currentFileState.getDestFile().toString();
            if (result.isPresent() && result.get() == openDirAndSelectFileButtonType) {
                OsDesktopUtil.openFileLocation(destFilePath);
            } else if (result.isPresent() && result.get().equals(openFileButtonType)) {
                OsDesktopUtil.openFile(destFilePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dispose() {
        // 调用子组件的 dispose
        if (markdownTabViewController != null) {
            markdownTabViewController.dispose();
        }

        // 如果是多标签：
        // for (var controller : tabControllers) { controller.dispose(); }
    }
    
}
