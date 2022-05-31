package ru.alexey.mydropbox.cloud.server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class FilesHandler implements Runnable {

    private final String serverDir = "server_files";
    private DataInputStream is;
    private DataOutputStream os;

    public FilesHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        System.out.println("Client accepted");
        sendListOfFiles(serverDir);
    }

    private void sendListOfFiles(String dir) throws IOException {
        os.writeUTF("#list#");
        List<String> files = getFiles(serverDir);
        os.writeInt(files.size());
        for (String file : files) {
            os.writeUTF(file);
        }
        os.flush();
    }

    private List<String> getFiles(String dir) {
        String[] list = new File(dir).list();
        assert list != null;
        return Arrays.asList(list);
    }

    @Override
    public void run() {
        byte[] buf = new byte[256];
        try {
            while (true) {
                String command = is.readUTF();
                System.out.println("received: " + command);
                if (command.equals("#file#")) {
                    String fileName = is.readUTF();
                    long len = is.readLong();
                    File file = Path.of(serverDir).resolve(fileName).toFile();
                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        for (int i = 0; i < (len + 255) / 256; i++) {
                            int read = is.read(buf);
                            fos.write(buf, 0 , read);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendListOfFiles(serverDir);
                } else if (command.equals("#download#")) {
                    sendFileFromServer();
                }
            }
        } catch (Exception e) {
            System.err.println("Connection was broken");
        }
    }

    private void sendFileFromServer() throws IOException {
        byte[] buf = new byte[256];
        os.writeUTF("#download#");
        String fileName = is.readUTF();
        os.writeUTF(fileName);
        File toSend = Path.of(serverDir).resolve(fileName).toFile();
        os.writeLong(toSend.length());
        try (FileInputStream fis = new FileInputStream(toSend)) {
            while (fis.available() > 0) {
                int read = fis.read(buf);
                os.write(buf, 0, read);
            }
        }
        os.flush();
    }
}
