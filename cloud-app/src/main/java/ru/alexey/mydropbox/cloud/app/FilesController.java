package ru.alexey.mydropbox.cloud.app;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import ru.alexey.mydropbox.cloud.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FilesController implements Initializable {
    private Image IMAGE_FOLDER;
    private Image IMAGE_FILE;

    private String homeDir;

    @FXML
    public ListView<String> clientView;

    @FXML

    public ListView<String> serverView;

    private Network network;
    private Map<String, Integer> serverFiles;

    private void readLoop() {
        try {
            while (true) {
                CloudMessage message = network.read();
                if (message instanceof ListFiles listFiles) {
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
                }
            }
        } catch (Exception e) {
            System.err.println("Connection lost");
        }
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
            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(homeDir));
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
        return Arrays.asList(list);
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String file = clientView.getSelectionModel().getSelectedItem();
        network.write(new FileMessage(Path.of(homeDir).resolve(file)));
    }

    public void download(ActionEvent actionEvent) throws IOException {

        String file = serverView.getSelectionModel().getSelectedItem();
        network.write(new FileRequest(file));
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

//    public List<String> getFilesNames(List<FileSystemObject> fileSystemObjects) {
//        return fileSystemObjects.stream().map(p -> p.getName()).collect(Collectors.toList());
//    }
}