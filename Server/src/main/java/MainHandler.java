import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainHandler extends ChannelInboundHandlerAdapter {
    private Path server = Paths.get("server_storage");
    private Path clientDir;
    private Path currentClientDir;
    private Path parentCurrentClientDir;
    private String clientNick;
    private static int count;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        count++;
        clientNick = "user" + count;
        clientDir = Paths.get("server_storage",clientNick);
        currentClientDir = clientDir;
        log.debug("Connected- " + clientNick + " directory- " + clientDir);
        if (!Files.exists(clientDir)) {
            Files.createDirectory(clientDir);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {


        if (msg instanceof ListRequest) {
            ListRequest path = (ListRequest) msg;
            ctx.writeAndFlush(new ServerList(Files.list(currentClientDir).map(FilesListInfo::new).collect(Collectors.toList())));
            log.debug("Server sent list " );

        } else if (msg instanceof FileRequest) {
            FileRequest request = (FileRequest) msg;
            ctx.writeAndFlush(new FileMessage(clientDir.resolve(request.getFilename())));

        } else if (msg instanceof FileMessage) {
            FileMessage file = (FileMessage) msg;
            Files.write(clientDir.resolve(file.getFileName()), file.getData());

        } else if (msg instanceof DirectoryRequest) {

            ctx.writeAndFlush(new UserDirectory(clientDir.toString()));
            log.debug("Send to client- " + clientNick + " server user directory - " + clientDir.toString());

        } else if (msg instanceof ParentDirectoryRequest) {
            log.debug("Request for parent dir - " + clientDir.getParent().toString());
            ctx.writeAndFlush(new ParentUserDirectory(clientDir.getParent().toString()));

        } else if (msg instanceof ExitRequest) {
            log.debug("Disconnected- " + clientNick);
            ctx.close();
        } else if (msg instanceof FileDelete) {
            FileDelete file = (FileDelete) msg;
            Files.delete(clientDir.resolve(file.getFileName()));
            log.debug("Deleted on server- " + file.getFileName());

        }
    }

}
