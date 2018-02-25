
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.CharsetUtil
import java.lang.Thread.sleep
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean


class NettyClient(private val host: String, private val port: Int) {
    private var eventGroup: EventLoopGroup? = null
    private var channelFuture: ChannelFuture? = null
    private val openned = AtomicBoolean(false)

    fun open() {
        if (openned.compareAndSet(false, true)) {
            eventGroup = NioEventLoopGroup()

            val sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
            val clientBootstrap = Bootstrap()
            clientBootstrap.group(eventGroup)
            clientBootstrap.channel(NioSocketChannel::class.java)
            clientBootstrap.remoteAddress(InetSocketAddress(host, port))
            clientBootstrap.handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(socketChannel: SocketChannel) {
                    val pipeline = socketChannel.pipeline()
                    pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));
                    pipeline.addLast("length-decoder",
                                     LengthFieldBasedFrameDecoder(
                                         Integer.MAX_VALUE, 0, 4, 0, 4))
                    pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8));

                    pipeline.addLast("length-encoder", LengthFieldPrepender(4));
                    pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))

                    pipeline.addLast(ClientHandler())
                }
            })
            channelFuture = clientBootstrap.connect().sync()
        }
    }

    fun close() {
        if (openned.compareAndSet(true, false)) {
            eventGroup?.shutdownGracefully()
        }
    }

    fun send(command: Command) {
        if (openned.get()) {
            channelFuture?.channel()?.writeAndFlush(OBJECT_MAPPER.writeValueAsString(command))
        }
    }
}

fun main(args: Array<String>) {

    val client = NettyClient("localhost", 1234)
    client.open()
    client.send(Command(commandType = CommandType.SESSION_INIT,
                        sessionInit = SessionInitCommand(sequenceNo = 0)))

    sleep(5_000)
    client.close()
}


class ClientHandler : SimpleChannelInboundHandler<String>() {
    override fun channelRead0(p0: ChannelHandlerContext?, p1: String) {
        println("Client received: " + p1)
    }

    override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
        //channelHandlerContext.writeAndFlush(Unpooled.copiedBuffer("Netty Rocks!", CharsetUtil.UTF_8))
    }

    override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        channelHandlerContext.close()
    }
}