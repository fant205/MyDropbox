package netty.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import netty.service.AuthService;
import netty.service.DataBaseAuthServiceImpl;
import ru.alexey.mydropbox.cloud.model.*;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class CloudFileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path currentDir;
    private AuthService authService;
    private List<String> connectedUsers;

    public CloudFileHandler() {
        currentDir = Path.of("server_files");
        authService = new DataBaseAuthServiceImpl();
        authService.start();
        connectedUsers = new ArrayList<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new ListFiles(currentDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) {
        try {
            if (cloudMessage instanceof FileRequest fileRequest) {
                ctx.writeAndFlush(new FileMessage(currentDir.resolve(fileRequest.getName())));
            } else if (cloudMessage instanceof FileMessage fileMessage) {
                Files.write(currentDir.resolve(fileMessage.getName()), fileMessage.getData());
                ctx.writeAndFlush(new ListFiles(currentDir));
            } else if (cloudMessage instanceof PathInRequest pathInRequest) {
                Path resolve = currentDir.resolve(pathInRequest.getFolderName());
                if (Files.isDirectory(resolve)) {
                    currentDir = resolve;
                    ctx.writeAndFlush(new ListFiles(currentDir));
                }
                log.error("Указанный файл не явлется директорией!: " + resolve.toString());
            } else if (cloudMessage instanceof PathUpRequest pathUpRequest) {
                currentDir = currentDir.getParent();
                ctx.writeAndFlush(new ListFiles(currentDir));
            } else if (cloudMessage instanceof DeleteRequest deleteRequest) {
                deleteRequest(ctx, deleteRequest);
            } else if (cloudMessage instanceof RenameRequest renameRequest) {
                renameRequest(ctx, renameRequest);
            } else if (cloudMessage instanceof AuthMessage authMessage) {
                authMessage(ctx, authMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void authMessage(ChannelHandlerContext ctx, AuthMessage authMessage) throws IOException {
        String nickName = authService.getNickNameByLoginAndPassword(authMessage.getLogin(), authMessage.getPassword());
        if (nickName != null) {
            if (connectedUsers.contains(nickName)) {
                ctx.writeAndFlush(new ErrorAuthorization("Учетная запись уже используется"));
            }
            connectedUsers.add(nickName);
            ctx.writeAndFlush(new SuccessAuthorization());
//            ctx.writeAndFlush(new ListFiles(currentDir));
        } else {
            ctx.writeAndFlush(new ErrorAuthorization("Неверные логин или пароль"));
        }
    }

    private void deleteRequest(ChannelHandlerContext ctx, DeleteRequest deleteRequest) throws IOException {
        Path pathToBeDeleted = currentDir.resolve(deleteRequest.getName());
        if (Files.isDirectory(pathToBeDeleted)) {
            Files.walk(pathToBeDeleted)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

        } else {
            Files.delete(pathToBeDeleted);
        }
        ctx.writeAndFlush(new ListFiles(currentDir));
    }

    private void renameRequest(ChannelHandlerContext ctx, RenameRequest renameRequest) throws IOException {
        Path source = currentDir.resolve(renameRequest.getOldName());
        Files.move(source, currentDir.resolve(renameRequest.getNewName()));
        ctx.writeAndFlush(new ListFiles(currentDir));
    }


}