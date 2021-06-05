package org.xokyopo.client.ui;

import com.sun.javafx.collections.ObservableListWrapper;
import io.netty.channel.Channel;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.xokyopo.client.ui.dialogs.rename.GFileRenameDialog;
import org.xokyopo.client.ui.entity.UiFileRep;
import org.xokyopo.clientservercommon.network.netty.NettyClientConnection;
import org.xokyopo.clientservercommon.seirialization.MyHandlerFactory;
import org.xokyopo.clientservercommon.seirialization.executors.AuthorizationExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.FileListExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.FileOperationExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.FilePartExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.messages.entitys.FileRep;
import org.xokyopo.clientservercommon.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class GFileManagerController implements Initializable {
    private final String repository = "client_repository";
    private final String remoteUrl = "localhost";
    private final int remotePort = 8999;
    @FXML
    public TextField loginField;
    @FXML
    public TextField passField;
    @FXML
    public AnchorPane loginPane;
    @FXML
    public Button loginOK;
    @FXML
    public Button LoginCancel;
    @FXML
    public AnchorPane fileManagerPain;
    @FXML
    public Button fmRename;
    @FXML
    public Button fmCopy;
    @FXML
    public Button fmDelete;
    @FXML
    public HBox fmProgressBarBox;
    @FXML
    public Label fmProgressLabel;
    @FXML
    public ProgressBar fmProgressBar;
    @FXML
    public TableView<UiFileRep> fmLeftTable;
    @FXML
    public TableView<UiFileRep> fmRightTable;
    @FXML
    public TextField fmLeftResourcePath;
    @FXML
    public TextField fmRightResourcePath;
    public FilePartExecutor filePartExecutor;
    public FileListExecutor fileListExecutor;
    public AuthorizationExecutor authorizationExecutor;
    public FileOperationExecutor fileOperationExecutor;
    private UiAlert uiAlert;
    private Channel channel;
    private NettyClientConnection nettyClientConnection;
    private CountDownLatch countDownLatch;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.uiAlert = new UiAlert();
        this.setTableFactory(this.fmLeftTable, "name", "type", "size");
        this.setTableFactory(this.fmRightTable, "name", "type", "size");
        this.setRemotePath("");
        this.setLocalPath("");
        this.creatingExecutors();
        this.createNettyClientConnection();

        this.showLoginPain();
        this.fmProgressBarBox.setVisible(false);
    }

    private void creatingExecutors() {
        this.filePartExecutor = new FilePartExecutor(
                this::getClientRepository,
                (ch) -> Platform.runLater(this::updateLeftTable),
                (fn, fl, tl) -> Platform.runLater(() -> this.loadStatistic(fn, fl, tl)),
                (fn, fl, tl) -> Platform.runLater(() -> this.uploadStatistic(fn, fl, tl)));

        this.fileListExecutor = new FileListExecutor(
                null,
                (fileList) -> Platform.runLater(() -> this.fillingRightTable(fileList)));

        this.fileOperationExecutor = new FileOperationExecutor(
                null,
                (b) -> Platform.runLater(() -> {
                    this.updateRightTable();
                    this.fmProgressBarBox.setVisible(false);
                }));

        this.authorizationExecutor = new AuthorizationExecutor(
                request -> Platform.runLater(() -> {
                    if (request) {
                        this.showFileManagerPain();
                    } else {
                        this.showLoginPain();
                        uiAlert.showWarningAlert("Авторизация", "Ошибка авторизации", "Логин или Пароль указаны не верно");
                    }
                }),
                null);
    }

    private void createNettyClientConnection() {
        this.nettyClientConnection = new NettyClientConnection(
                new MyHandlerFactory(
                        this::ifConnect,
                        this::ifDisconnect,
                        this.authorizationExecutor,
                        this.fileListExecutor,
                        this.fileOperationExecutor,
                        this.filePartExecutor));
    }

    private void connection() {
        this.countDownLatch = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            try {
                nettyClientConnection.run(remoteUrl, remotePort);
            } catch (Exception e) {
                this.countDownLatch.countDown();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void setTableFactory(TableView<?> tableView, String... objectFieldsName) {
        for (int i = 0; i < Math.min(tableView.getColumns().size(), objectFieldsName.length); i++) {
            tableView.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(objectFieldsName[i]));
        }
    }


    @FXML
    public void loginOKAction(ActionEvent actionEvent) {
        this.loginField.setEditable(false);
        this.passField.setEditable(false);
        //TODO (loginOKAction) переделать проверку и выброс ошибки.
        if (this.channel == null) {
            this.connection();
            this.awaitConnection();
        }
        if (this.channel != null) {
            this.authorizationExecutor.sendLoginAndPass(this.loginField.getText(), this.passField.getText(), this.channel);
        } else {
            uiAlert.showErrorAlert("Соединение", "Ошибка подключения", "Удаленный ресурс не найден.");
            this.showLoginPain();
        }
    }

    @FXML
    public void LoginCancelAction(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(0);
    }

    private void showLoginPain() {
        this.loginPane.setVisible(true);
        this.loginField.setEditable(true);
        this.passField.setEditable(true);
        this.fileManagerPain.setVisible(false);
    }

    private void showFileManagerPain() {
        this.loginPane.setVisible(false);
        this.fileManagerPain.setVisible(true);
        this.fillingTable();
    }

    private void fillingTable() {
        this.updateLeftTable();
        this.updateRightTable();
    }

    private void updateLeftTable() {
        this.fillingLeftTable(Paths.get(this.getClientRepository(null), this.getLocalPath()).toFile().listFiles());
    }

    private void updateRightTable() {
        this.fileListExecutor.getFileList(this.getRemotePath(), this.channel);
    }

    private void fillingLeftTable(File... files) {
        if (files != null) {
            LinkedList<UiFileRep> uiFileRepList = Arrays.stream(files).map(UiFileRep::new).collect(Collectors.toCollection(LinkedList::new));
            if (!this.getLocalPath().equals("")) uiFileRepList.addFirst(new UiFileRep("..", true, 0L));
            this.fmLeftTable.setItems(new ObservableListWrapper<>(uiFileRepList));
        }
    }

    private void fillingRightTable(List<FileRep> fileRepList) {
        LinkedList<UiFileRep> uiFileRepList = fileRepList.stream().map(UiFileRep::new).collect(Collectors.toCollection(LinkedList::new));
        if (!this.getRemotePath().equals("")) uiFileRepList.addFirst(new UiFileRep("..", true, 0L));
        this.fmRightTable.setItems(new ObservableListWrapper<>(uiFileRepList));
    }

    @FXML
    public void fmRenameAction(ActionEvent actionEvent) {
        GFileRenameDialog fileRenameDialog = new GFileRenameDialog();
        String oldName;
        if (this.fmLeftTable.isFocused()) {
            oldName = this.getSelectedItem(this.fmLeftTable).getName();
            try {
                this.localRename(oldName, fileRenameDialog.showAndWait(oldName));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.updateLeftTable();
        } else if (this.fmRightTable.isFocused()) {
            oldName = this.getSelectedItem(this.fmRightTable).getName();
            this.remoteRename(oldName, fileRenameDialog.showAndWait(oldName));
        }
    }

    private void localRename(String oldName, String newName) throws IOException {
        if (!oldName.equals(newName)) {
            Files.move(
                    Paths.get(this.getClientRepository(null), this.getLocalPath(), oldName),
                    Paths.get(this.getClientRepository(null), this.getLocalPath(), newName)
            );
        }
    }

    private void remoteRename(String oldName, String newName) {
        if (!oldName.equals(newName)) {
            this.fileOperationExecutor.moveFile(
                    Paths.get(this.getRemotePath(), oldName).toString(),
                    Paths.get(this.getRemotePath(), newName).toString(), this.channel
            );
        }
    }

    private UiFileRep getSelectedItem(TableView<UiFileRep> treeView) {
        return treeView.getSelectionModel().getSelectedItem();
    }

    @FXML
    public void fmCopyAction(ActionEvent actionEvent) {
        if (this.fmLeftTable.isFocused()) {
            Thread t = new Thread(() -> {
                try {
                    this.filePartExecutor.uploadFile(this.getSelectedItem(this.fmLeftTable).getName(), this.getLocalPath(), this.getRemotePath(), this.channel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.setDaemon(true);
            t.start();
        } else if (this.fmRightTable.isFocused()) {
            this.filePartExecutor.loadFile(this.getSelectedItem(this.fmRightTable).getName(), this.getRemotePath(), this.getLocalPath(), this.channel);
        }
    }

    @FXML
    public void fmDeleteAction(ActionEvent actionEvent) {
        if (this.fmLeftTable.isFocused()) {
            try {
                FileUtil.recurseDelete(Paths.get(this.getClientRepository(null), this.getLocalPath(), this.getSelectedItem(this.fmLeftTable).getName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.updateLeftTable();
        } else if (this.fmRightTable.isFocused()) {
            this.fileOperationExecutor.deleteFile(Paths.get(this.getRemotePath(), this.getSelectedItem(this.fmRightTable).getName()).toString(), this.channel);
        }
    }

    private String getClientRepository(Channel channel) {
        Path path = Paths.get(this.repository);
        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return this.repository;
    }

    private void ifConnect(Channel channel) {
        if (this.countDownLatch != null) this.countDownLatch.countDown();
        this.channel = channel;
        System.out.println("ifConnect");
    }

    private void ifDisconnect(Channel channel) {
        this.channel = null;
        Platform.runLater(this::showLoginPain);
        System.out.println("ifDisconnect");
    }

    private void awaitConnection() {
        if (this.countDownLatch != null) {
            try {
                this.countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadStatistic(String fileName, long fileFullLength, long fileTransmittedLength) {
        this.updateProgressBar("Загружается: " + fileName, (double) fileTransmittedLength / (double) fileFullLength);
    }

    private void uploadStatistic(String fileName, long fileFullLength, long fileTransmittedLength) {
        this.updateProgressBar("Отправляется: " + fileName, (double) fileTransmittedLength / (double) fileFullLength);
    }

    private void updateProgressBar(String msg, double progress) {
        if (progress < 1) {
            this.fmProgressBarBox.setVisible(true);
            this.fmProgressLabel.setText(msg);
            this.fmProgressBar.setProgress(progress);
        } else {
            this.fmProgressBarBox.setVisible(false);
        }
    }

    @FXML
    public void fmLeftTableMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() >= 2 && mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            this.setLocalPath(this.getNewPath(this.getSelectedItem(this.fmLeftTable), this.getLocalPath()));
            this.updateLeftTable();
        }
    }

    @FXML
    public void fmRightTableMouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() >= 2 && mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            this.setRemotePath(this.getNewPath(this.getSelectedItem(this.fmRightTable), this.getRemotePath()));
            this.updateRightTable();
        }
    }

    private String getNewPath(UiFileRep uiFileRep, String currentPath) {
        if (uiFileRep != null && uiFileRep.isDir()) {
            return (uiFileRep.getName().equals("..")) ? this.pathUp(currentPath) : this.pathDown(currentPath, uiFileRep.getName());
        }
        return currentPath;
    }


    private String pathUp(String path) {
        Path p = Paths.get(path);
        return (p.getNameCount() > 1) ? p.getParent().toString() : "";
    }

    private String pathDown(String path, String child) {
        return Paths.get(path, child).toString();
    }

    //TODO переделать на геттеры и сеттеры.
    public String getLocalPath() {
        return fmLeftResourcePath.getText();
    }

    public void setLocalPath(String fmLeftResourcePath) {
        this.fmLeftResourcePath.setText(fmLeftResourcePath);
    }

    public String getRemotePath() {
        return fmRightResourcePath.getText();
    }

    public void setRemotePath(String fmRightResourcePath) {
        this.fmRightResourcePath.setText(fmRightResourcePath);
    }
}
