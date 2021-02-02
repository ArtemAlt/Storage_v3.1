import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    private Path clientDir;
    private String clientNick;
    private static int count;


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        count++;
        clientNick = "user" + count;
        clientDir = Paths.get("server_storage", clientNick);
        if (!Files.exists(clientDir)){
            Files.createDirectory(clientDir);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ListRequest){
            ctx.writeAndFlush(new FilesList(clientDir));
        } else if (msg instanceof FileRequest){
            FileRequest request = (FileRequest) msg;
            ctx.writeAndFlush( new FileMessage(clientDir.resolve(request.getFilename())));
        } else if (msg instanceof FileMessage){
            FileMessage file = (FileMessage) msg;
            Files.write(clientDir.resolve(file.getFileName()),file.getData());
        }
    }

}
