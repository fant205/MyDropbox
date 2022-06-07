package netty.handler;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import ru.alexey.mydropbox.cloud.model.*;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class CloudFileHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private Path currentDir;

    public CloudFileHandler() {
        currentDir = Path.of("server_files");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new ListFiles(currentDir));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudMessage cloudMessage) throws Exception {
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
        }
    }
}