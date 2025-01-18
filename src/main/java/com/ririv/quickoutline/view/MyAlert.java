package com.ririv.quickoutline.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;


//https://blog.csdn.net/qq_40990854/article/details/85161449
public class MyAlert extends Alert {



    public MyAlert(AlertType alertType,String contentText,Window owner,ButtonType... buttons) {

        super(alertType,contentText,buttons);
        super.initOwner(owner);

        // 获取 DialogPane
        DialogPane dialogPane = this.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("BasicControls.css").toExternalForm());
        for (var button: buttons){
            switch (button.getButtonData()){
                case OK_DONE -> dialogPane.lookupButton(button).getStyleClass().addAll("my-button", "text-button-lightbg", "text-button-primary");
                case CANCEL_CLOSE -> dialogPane.lookupButton(button).getStyleClass().addAll("my-button", "text-button-lightbg","text-button-default");
            }

        }



        this.setHeaderText(null);
        configure();
    }


    public static Optional<ButtonType> showAlert(AlertType alertType,String contentText,Window owner,ButtonType... buttons) {
        Alert alert = new MyAlert(alertType,contentText,owner,buttons);
        return alert.showAndWait();
    }

    private void configure(){

        DialogPane pane = this.getDialogPane();
        pane.setBackground(new Background(new BackgroundFill(Paint.valueOf("White"),null,null)));
        Stage stage = (Stage) this.getDialogPane().getScene().getWindow();


        Image image;
        if (this.getAlertType() == AlertType.WARNING) {
            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "icon/warning.png"
            )));
            this.setTitle("警告");
        }
        else if (this.getAlertType() == AlertType.ERROR) {
            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "icon/error.png"
            )));
            this.setTitle("错误");
        }
        else if (this.getAlertType() == AlertType.CONFIRMATION){
            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "icon/confirmation.png"
            )));
            this.setTitle("确认");
        }
        else {
            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(
                    "icon/SmileFace.png"
            )));
            this.setTitle("成功");
        }
//        stage.getIcons().add(image);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);

        this.setGraphic(imageView);
    }
}
