package netty.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import netty.service.FilesService;
import netty.service.FilesServiceImpl;
import ru.alexey.mydropbox.cloud.model.*;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
public class CloudFileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path currentDir;
    private FilesService filesService;
    private List<String> connectedUsers;

    public CloudFileHandler() {
        currentDir = Path.of("server_files");
        filesService = new FilesServiceImpl();
        filesService.start();
        connectedUsers = new ArrayList<>();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        ctx.writeAndFlush(new ListFiles(currentDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) {
        try {
            if (cloudMessage instanceof FileRequest fileRequest) {
                ctx.writeAndFlush(new FileMessage(currentDir.resolve(fileRequest.getName())));
            } else if (cloudMessage instanceof FileMessage fileMessage) {
                Files.write(currentDir.resolve(fileMessage.getName()), fileMessage.getData());
                ctx.writeAndFlush(new ListFilesResponse(currentDir));
            } else if (cloudMessage instanceof PathInRequest pathInRequest) {
                Path resolve = currentDir.resolve(pathInRequest.getFolderName());
                if (Files.isDirectory(resolve)) {
                    currentDir = resolve;
                    ctx.writeAndFlush(new ListFilesResponse(currentDir));
                }
                log.error("Указанный файл не явлется директорией!: " + resolve.toString());
            } else if (cloudMessage instanceof PathUpRequest pathUpRequest) {
                currentDir = currentDir.getParent();
                ctx.writeAndFlush(new ListFilesResponse(currentDir));
            } else if (cloudMessage instanceof DeleteRequest deleteRequest) {
                deleteRequest(ctx, deleteRequest);
            } else if (cloudMessage instanceof RenameRequest renameRequest) {
                renameRequest(ctx, renameRequest);
            } else if (cloudMessage instanceof AuthMessage authMessage) {
                authMessage(ctx, authMessage);
            } else if (cloudMessage instanceof RegistrationMessage registrationMessage) {
                registrationMessage(ctx, registrationMessage);
            } else if (cloudMessage instanceof UsersRequestMessage usersRequestMessage) {
                usersRequestMessage(ctx, usersRequestMessage);
            } else if (cloudMessage instanceof ShareFileMessage shareFileMessage) {
                shareFileMessage(ctx, shareFileMessage);
            } else if (cloudMessage instanceof ShowShareMessage showShareMessage) {
                showShareMessage(ctx, showShareMessage);
            } else if (cloudMessage instanceof ListFilesRequest listFilesRequest) {
                listFilesRequest(ctx, listFilesRequest);
            } else if (cloudMessage instanceof FileOnShareRequest fileOnShareRequest) {
                fileOnShareRequest(ctx, fileOnShareRequest);
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.writeAndFlush(new ErrorMessage(e.getLocalizedMessage()));
        }
    }

    private void fileOnShareRequest(ChannelHandlerContext ctx, FileOnShareRequest fileOnShareRequest) throws IOException {
        String filePath = filesService.findFile(fileOnShareRequest.getFile());
        Path file = Path.of(filePath);
        ctx.writeAndFlush(new FileMessage(file));
    }

    private void listFilesRequest(ChannelHandlerContext ctx, ListFilesRequest listFilesRequest) throws IOException {
        ctx.writeAndFlush(new ListFilesResponse(currentDir));
    }

    private void showShareMessage(ChannelHandlerContext ctx, ShowShareMessage showShareMessage) {
        Map<String, Integer> shareFiles = filesService.findShareFiles(showShareMessage.getTargetUser());
        ctx.writeAndFlush(new ListFilesResponse(shareFiles));
    }

    private void shareFileMessage(ChannelHandlerContext ctx, ShareFileMessage shareFileMessage) {
        Path path = currentDir.resolve(shareFileMessage.getFileName());
        filesService.shareFile(shareFileMessage.getFileName(), path.toAbsolutePath().toString(), shareFileMessage.getOwnerUser(), shareFileMessage.getTargetUser());
    }

    public static void main(String[] args) {
        Path root = Path.of("server_files/test");
        Path server_files = Path.of("server_files/test/2");
        Path resolve = server_files.resolve("22.txt");
        System.out.println(resolve.toString());
        System.out.println(resolve.toAbsolutePath().toString());
        System.out.println(resolve.toString().replaceAll(root.toString(), ""));

    }

    private void usersRequestMessage(ChannelHandlerContext ctx, UsersRequestMessage usersRequestMessage) {
        List<String> usersList = null;
        try {
            usersList = filesService.findUsers();
        } catch (Exception e) {
            e.printStackTrace();
            ctx.writeAndFlush(new ErrorMessage(e.getLocalizedMessage()));
            return;
        }
        ctx.writeAndFlush(new UsersResponseMessage(usersList));
    }

    private void registrationMessage(ChannelHandlerContext ctx, RegistrationMessage msg) {
        //add new user in db
        try {
            filesService.createUser(msg.getLogin(), msg.getPassword(), msg.getNickname());
        } catch (Exception e) {
            e.printStackTrace();
            ctx.writeAndFlush(new ErrorMessage(e.getLocalizedMessage()));
            return;
        }

        //send success to user
        ctx.writeAndFlush(new SuccessRegistration());
    }

    private void authMessage(ChannelHandlerContext ctx, AuthMessage authMessage) throws IOException {
        String nickName = filesService.getNickNameByLoginAndPassword(authMessage.getLogin(), authMessage.getPassword());
        if (nickName != null) {
            if (connectedUsers.contains(nickName)) {
                ctx.writeAndFlush(new ErrorMessage("Учетная запись уже используется"));
                return;
            }
            Path path = currentDir.resolve(authMessage.getLogin());
            if (Files.exists(path)) {
                currentDir = path;
            } else {
                Files.createDirectories(path);
                currentDir = path;
            }

            connectedUsers.add(nickName);
            ctx.writeAndFlush(new SuccessAuthorization(nickName));
            ctx.writeAndFlush(new ListFilesResponse(currentDir));
        } else {
            ctx.writeAndFlush(new ErrorMessage("Неверные логин или пароль"));
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
        ctx.writeAndFlush(new ListFilesResponse(currentDir));
    }

    private void renameRequest(ChannelHandlerContext ctx, RenameRequest renameRequest) throws IOException {
        Path source = currentDir.resolve(renameRequest.getOldName());
        Files.move(source, currentDir.resolve(renameRequest.getNewName()));
        ctx.writeAndFlush(new ListFilesResponse(currentDir));
    }


}