package nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NioServer {

    private ServerSocketChannel server;
    private Selector selector;
    private Path currentFolder;

    public NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();

        server.bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        currentFolder = Paths.get("server_files");

        System.out.println("Сервер запущен!");
    }

    public void start() throws IOException {
        while (server.isOpen()) {
//            System.out.println("лог 1");
            selector.select();

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
//                System.out.println("лог 2");
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
//                    System.out.println("лог 3");
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }

                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();
        while (channel.isOpen()) {
            int read = channel.read(buffer);
            if (read == -1) {
                channel.close();
                return;
            }

            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                sb.append((char) buffer.get());
            }
            buffer.clear();
        }

        String cmd = sb.toString();
        if (cmd.startsWith(Constants.COMMAND_LS)) {
            ls(channel);
        } else if (cmd.startsWith(Constants.COMMAND_CAT)) {
            cat(channel, cmd);
        } else if (cmd.startsWith(Constants.COMMAND_CD)) {
            cd(channel, cmd);
        } else {
            channel.write(ByteBuffer.wrap("Команда не поддерживается!\n->".getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void ls(SocketChannel channel) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Path path : Files.list(currentFolder).toList()) {
            Path file = path.getFileName();
            String fileName = file.toString();
            sb.append(fileName + "\n");
        }
        sb.append(currentFolder.toString() + Constants.POINTER);
        channel.write(ByteBuffer.wrap(sb.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private void cat(SocketChannel channel, String cmd) throws IOException {

        cmd = cmd.replaceFirst(Constants.COMMAND_CAT, "");
        if (cmd.isEmpty() || cmd.trim().isEmpty()) {
            channel.write(ByteBuffer.wrap("Введите имя файла!\n->".getBytes(StandardCharsets.UTF_8)));
            return;
        }

        Path path = currentFolder.resolve(cmd.trim());

        if (!Files.exists(path)) {
            channel.write(ByteBuffer.wrap("Файл с таким именем не найден!\n->".getBytes(StandardCharsets.UTF_8)));
            return;
        }

        byte[] bytes1 = Files.readAllBytes(path);
        String pointer = currentFolder.toString() + Constants.POINTER;
        byte[] bytes2 = pointer.getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bytes1.length + bytes2.length];

        System.arraycopy(bytes1, 0, all, 0, bytes1.length);
        System.arraycopy(bytes2, 0, all, bytes1.length, bytes2.length);

        channel.write(ByteBuffer.wrap(all));
    }


    private void cd(SocketChannel channel, String cmd) throws IOException {
        cmd = cmd.replaceFirst(Constants.COMMAND_CD, "");
        if (cmd.isEmpty() || cmd.trim().isEmpty()) {
            channel.write(ByteBuffer.wrap("Введите имя папки!\n->".getBytes(StandardCharsets.UTF_8)));
            return;
        }

        Path path = currentFolder.resolve(cmd.trim());

        if (!Files.exists(path)) {
            channel.write(ByteBuffer.wrap("Папка с таким именем не найдена!\n->".getBytes(StandardCharsets.UTF_8)));
            return;
        }

        currentFolder = path.normalize();
        if (currentFolder.toString().isEmpty()) {
            currentFolder = currentFolder.toAbsolutePath();
        }
        String msg = currentFolder.toString() + Constants.POINTER;
        channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));

    }

    private void handleAccept() throws IOException {
        SocketChannel socketChannel = server.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        String msg = "Hi, Alexey!\n" + currentFolder.toString() + Constants.POINTER;
        socketChannel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws IOException {
        new NioServer().start();
    }
}


