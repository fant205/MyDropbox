package ru.alexey.mydropbox.cloud.app;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class FilesController implements Initializable {


    private String homeDir;

    private byte[] buf;

    @FXML
    public ListView<String> clientView;

    @FXML
    public ListView<String> serverView;

    private Network network;




    private void readLoop() {
        try {
            while (true) {
                String command = network.readString();
                System.out.println("client received: " + command);
                if (command.equals("#list#")) {
                    Platform.runLater(() -> serverView.getItems().clear());
                    int len = network.readInt();
                    for (int i = 0; i < len; i++) {
                        String file = network.readString();
                        Platform.runLater(() -> serverView.getItems().add(file));
                    }
                } else if (command.equals("#download#")) {
                    processingDownloadCommand();
                }
            }
        } catch (Exception e) {
            System.err.println("Connection lost");
            e.printStackTrace();
        }
    }

    private void processingDownloadCommand() throws IOException {
        String fileName = network.getIs().readUTF();
        long len = network.getIs().readLong();
        File file = Path.of(homeDir).resolve(fileName).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (int i = 0; i < (len + 255) / 256; i++) {
                int read = network.getIs().read(buf);
                fos.write(buf, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        clientView.getItems().clear();
        List<String> files = getFiles(homeDir);
        ObservableList<String> items = clientView.getItems();
        items.addAll(files);
    }

    // post init fx fields
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            buf = new byte[256];
            homeDir = System.getProperty("user.home");
            clientView.getItems().clear();
            clientView.getItems().addAll(getFiles(homeDir));
            network = new Network(8189);
            Thread readThread = new Thread(this::readLoop);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private List<String> getFiles(String dir) {
        String[] list = new File(dir).list();
        assert list != null;
        return Arrays.asList(list).stream().sorted().toList();
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        network.getOs().writeUTF("#file#");
        String file = clientView.getSelectionModel().getSelectedItem();
        network.getOs().writeUTF(file);
        File toSend = Path.of(homeDir).resolve(file).toFile();
        network.getOs().writeLong(toSend.length());
        try (FileInputStream fis = new FileInputStream(toSend)) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                network.getOs().write(buf, 0, read);
            }
        }
        network.getOs().flush();
    }

    public void download(ActionEvent actionEvent) throws IOException {
        network.getOs().writeUTF("#download#");
        String file = serverView.getSelectionModel().getSelectedItem();
        network.getOs().writeUTF(file);
        network.getOs().flush();
    }
}