package ru.alexey.mydropbox.cloud.app;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import ru.alexey.mydropbox.cloud.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FilesController implements Initializable {
    private Image IMAGE_FOLDER;
    private Image IMAGE_FILE;

    private String homeDir;

    @FXML
    public ListView<String> clientView;

    @FXML
    public ListView<String> serverView;

    @FXML
    public Button clientBack;
    @FXML
    public Button clientRename;
    @FXML
    public Button clientDelete;
    @FXML
    public Button authButt;
    @FXML
    public Button registerButt;
    @FXML
    public Button serverBack;
    @FXML
    public Button serverRename;
    @FXML
    public Button serverDelete;

    @FXML
    public Button upload;
    @FXML
    public Button download;

    @FXML
    public Label nicknameLabel;

    @FXML
    public Button share;

    @FXML
    public Button showMyFiles;

    @FXML
    public Button showShare;

    private Network network;
    private Map<String, Integer> serverFiles;
    private String shareFile;
    private String nickname;
    private boolean showingShareFiles;


    private void readLoop() {
        try {
            while (true) {
                CloudMessage message = network.read();
                if (message instanceof ListFilesResponse listFiles) {
                    Platform.runLater(() -> {
                        initServerView();
                        serverView.getItems().clear();
                        serverFiles = listFiles.getFiles();
                        serverView.getItems().addAll(convert(serverFiles));
                    });
                } else if (message instanceof FileMessage fileMessage) {
                    Path current = Path.of(homeDir).resolve(fileMessage.getName());
                    Files.write(current, fileMessage.getData());
                    Platform.runLater(() -> {
                        clientView.getItems().clear();
                        clientView.getItems().addAll(getFiles(homeDir));
                    });
                } else if (message instanceof SuccessAuthorization successAuthorization) {
                    clientBack.setDisable(false);
                    clientRename.setDisable(false);
                    clientDelete.setDisable(false);
                    authButt.setDisable(true);
                    registerButt.setDisable(true);
                    serverBack.setDisable(false);
                    serverRename.setDisable(false);
                    serverDelete.setDisable(false);
                    upload.setDisable(false);
                    download.setDisable(false);
                    share.setDisable(false);
                    showMyFiles.setDisable(false);
                    showShare.setDisable(false);


                    Platform.runLater(() -> {
                        nickname = successAuthorization.getNickname();
                        nicknameLabel.setText(successAuthorization.getNickname());
                        clientView.getItems().clear();
                        clientView.getItems().addAll(getFiles(homeDir));
                        Utils.info(" Авторизация", "Вы успешно авторизованы!", "Добро пожаловать, " + successAuthorization.getNickname() + "!");
                    });
                } else if (message instanceof SuccessRegistration successAuthorization) {

                    Platform.runLater(() -> {
                        Utils.info("Регистрация", "Вы успешно зарегестрированы!", "Теперь можно авторизоваться!");
                    });
                } else if (message instanceof UsersResponseMessage usersResponseMessage) {
                    Platform.runLater(() -> {
                        showUserSelectionDialog(usersResponseMessage);
                    });
                } else if (message instanceof ErrorMessage errorMessage) {
                    Platform.runLater(() -> {
                        Utils.error("Ошибка!", errorMessage.getMsg());
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showUserSelectionDialog(UsersResponseMessage usersResponseMessage) {
        List<String> users = usersResponseMessage.getUsers();
        users.remove(nickname);

        ChoiceDialog<String> dialog = new ChoiceDialog<>("", users);
        dialog.setTitle("Пользователи");
        dialog.setHeaderText("Публикация файла");
        dialog.setContentText("Выберите пользователя:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(targetUser -> {
            try {
                network.write(new ShareFileMessage(shareFile, nickname, targetUser));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<String> convert(Map<String, Integer> files) {
        return files.keySet().stream().sorted().toList();
    }

    // post init fx fields
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            homeDir = "client_files";
            IMAGE_FILE = new Image(Paths.get("images/file.png").toUri().toString());
            IMAGE_FOLDER = new Image(Paths.get("images/folder.png").toUri().toString());
            initClientView();
//            clientView.getItems().clear();
//            clientView.getItems().addAll(getFiles(homeDir));
            network = new Network(8189);
            Thread readThread = new Thread(this::readLoop);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> getFiles(String dir) {
        String[] list = new File(dir).list();
        assert list != null;
        return Arrays.asList(list).stream().sorted().toList();
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String file = clientView.getSelectionModel().getSelectedItem();
        network.write(new FileMessage(Path.of(homeDir).resolve(file)));
    }

    public void download(ActionEvent actionEvent) throws IOException {
        if (showingShareFiles) {
            String file = serverView.getSelectionModel().getSelectedItem();
            network.write(new FileOnShareRequest(file));
        } else {
            String file = serverView.getSelectionModel().getSelectedItem();
            network.write(new FileRequest(file));
        }


    }

    public void onMouseClicked(MouseEvent click) throws IOException {

        if (click.getClickCount() == 2) {
            String selectedItem = clientView.getSelectionModel().getSelectedItem();
            Path folder = Path.of(homeDir).resolve(selectedItem);
            if (!Files.isDirectory(folder)) {
                return;
            }

            //go into folder
            clientView.getItems().clear();
            homeDir = folder.toAbsolutePath().toString();
            clientView.getItems().addAll(getFiles(homeDir));

        }
    }


    public void parent(ActionEvent actionEvent) {



        homeDir = Paths.get(homeDir).getParent().toAbsolutePath().toString();
        clientView.getItems().clear();
        clientView.getItems().addAll(getFiles(homeDir));

    }

    public static void main(String[] args) {
        Path of = Path.of("client_file/test");
        System.out.println(of.getParent().toString());

    }

    public void initClientView() {
        clientView.setCellFactory(param -> new ListCell<String>() {
            private ImageView imageView = new ImageView();

            @Override
            public void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Path resolve = Paths.get(homeDir).resolve(name);
                    if (Files.isDirectory(resolve)) {
                        imageView.setImage(IMAGE_FOLDER);
                    } else {
                        imageView.setImage(IMAGE_FILE);
                    }
                    setText(name);
                    setGraphic(imageView);
                }
            }
        });


    }

    public void initServerView() {
        serverView.setCellFactory(param -> new ListCell<String>() {
            private ImageView imageView = new ImageView();

            @Override
            public void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (serverFiles.get(name).equals(1)) {
                        imageView.setImage(IMAGE_FOLDER);
                    } else {
                        imageView.setImage(IMAGE_FILE);
                    }
                    setText(name);
                    setGraphic(imageView);
                }
            }
        });


    }

    public void onMouseClickedOnServer(MouseEvent click) throws IOException {
        if (click.getClickCount() == 2) {
            String file = serverView.getSelectionModel().getSelectedItem();
            network.write(new PathInRequest(file));
        }
    }


    public void toParentOnServer(ActionEvent click) throws IOException {
        network.write(new PathUpRequest());
    }

    public void renameOnClient(ActionEvent actionEvent) throws IOException {
        String file = clientView.getSelectionModel().getSelectedItem();
        if (file == null || file.isEmpty()) {
            Utils.error("Ошибка!", "Выберите файл для переименования!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(file);
        dialog.setTitle("Переименование файла");
        dialog.setHeaderText(null);
        dialog.setContentText("Введите новое имя");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            Path source = Path.of(homeDir).resolve(file);
            try {
                Files.move(source, Path.of(homeDir).resolve(name));
            } catch (IOException e) {
                e.printStackTrace();
            }
            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(homeDir));
        });

    }

    public void deleteOnClient(ActionEvent actionEvent) throws IOException {

        String file = clientView.getSelectionModel().getSelectedItem();
        if (file == null || file.isEmpty()) {
            Utils.error("Ошибка!", "Выберите файл для удаления!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтверджение");
        alert.setHeaderText(file);
        alert.setContentText("Вы действительно хотите удалить файл?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            // ... user chose OK
            try {
                Path pathToBeDeleted = Path.of(homeDir).resolve(file);
                if (Files.isDirectory(pathToBeDeleted)) {
                    Files.walk(pathToBeDeleted)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);

                } else {
                    Files.delete(pathToBeDeleted);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(homeDir));
        } else {
            // ... user chose CANCEL or closed the dialog
        }

    }

    public void renameOnServer(ActionEvent actionEvent) {

        String oldName = serverView.getSelectionModel().getSelectedItem();
        if (oldName == null || oldName.isEmpty()) {
            Utils.error("Ошибка!", "Выберите на сервере файл для переименования!");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(oldName);
        dialog.setTitle("Переименование файла");
        dialog.setHeaderText(null);
        dialog.setContentText("Введите новое имя");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            try {
                network.write(new RenameRequest(oldName, newName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


    }

    public void deleteOnServer(ActionEvent actionEvent) {

        String file = serverView.getSelectionModel().getSelectedItem();
        if (file == null || file.isEmpty()) {
            Utils.error("Ошибка!", "Выберите на сервере файл для удаления!");
            return;
        }

        try {
            network.write(new DeleteRequest(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void register(ActionEvent actionEvent) {

        // Create the custom dialog.
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Регистрация");
        dialog.setHeaderText("Введите Ваш логин, пароль и ник");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Логин", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Логин");
        PasswordField password = new PasswordField();
        password.setPromptText("Пароль");
        TextField nickname = new TextField();
        username.setPromptText("Ник");

        grid.add(new Label("Логин:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(password, 1, 1);
        grid.add(new Label("Ник:"), 0, 2);
        grid.add(nickname, 1, 2);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                Map<String, String> map = new HashMap<>();
                map.put("login", username.getText());
                map.put("pass", password.getText());
                map.put("nickname", nickname.getText());
                return map;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();

        result.ifPresent(map -> {
//            System.out.println("Username=" + map.get("login") + ", Password=" + map.get("pass") + ", nickname: " + map.get("nickname"));
            try {
                network.write(new RegistrationMessage(map.get("login"), map.get("pass"), map.get("nickname")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public void authorize(ActionEvent actionEvent) {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Авторизация");
        dialog.setHeaderText("Введите Ваш логин и пароль");

        // Set the icon (must be included in the project).
//        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("Логин", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Логин");
        PasswordField password = new PasswordField();
        password.setPromptText("Пароль");

        grid.add(new Label("Логин:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(password, 1, 1);

        // Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> {

            try {
                network.write(new AuthMessage(usernamePassword.getKey(), usernamePassword.getValue()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void shareFile(ActionEvent actionEvent) {

        shareFile = serverView.getSelectionModel().getSelectedItem();
        if (shareFile == null || shareFile.isEmpty()) {
            Utils.error("Ошибка!", "Выберите на сервере файл для публикации!");
            return;
        }

        try {
            network.write(new UsersRequestMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void showShare(ActionEvent actionEvent) throws IOException {
        showingShareFiles = true;
        network.write(new ShowShareMessage(nickname));

    }

    public void showMyFiles(ActionEvent actionEvent) throws IOException {
        showingShareFiles = false;
        network.write(new ListFilesRequest());
    }
}