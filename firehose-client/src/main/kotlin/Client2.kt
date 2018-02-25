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
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class NettyClient(host: String, port: Int) {
    private var eventGroup: EventLoopGroup = NioEventLoopGroup()
    private var bootstrap: Bootstrap
    private var channelFuture: ChannelFuture? = null
    private val openned = AtomicBoolean(false)

    init {
        val sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        bootstrap = Bootstrap()
        bootstrap.group(eventGroup)
        bootstrap.channel(NioSocketChannel::class.java)
        bootstrap.remoteAddress(InetSocketAddress(host, port))
        bootstrap.handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(channel: SocketChannel) {
                val pipeline = channel.pipeline()
                pipeline.addLast(sslCtx.newHandler(channel.alloc()));
                pipeline.addLast("length-decoder",
                                 LengthFieldBasedFrameDecoder(
                                     Short.MAX_VALUE.toInt(), 0, 4, 0, 4))
                pipeline.addLast("stringDecoder", StringDecoder(CharsetUtil.UTF_8));

                pipeline.addLast("length-encoder", LengthFieldPrepender(4));
                pipeline.addLast("stringEncoder", StringEncoder(CharsetUtil.UTF_8))

                pipeline.addLast(ClientHandler())
            }
        })
    }

    fun open() {
        if (openned.compareAndSet(false, true)) {
            channelFuture = bootstrap.connect().addListener(object : ChannelFutureListener {
                override fun operationComplete(future: ChannelFuture) {
                    if (!future.isSuccess) {
                        println("Connection failed")
                        future.channel().eventLoop().schedule({
                                                                  println("Reconnecting....")
                                                                  openned.set(false)
                                                                  open()
                                                              }, 1, TimeUnit.SECONDS)
                    } else {
                        println("Connection succeded")
//                        future.channel().closeFuture().addListener(object : ChannelFutureListener {
//                            override fun operationComplete(future: ChannelFuture?) {
//                                println("Close")
//                            }
//                        })
                    }
                }
            })
        }
    }

    fun close() {
        if (openned.compareAndSet(true, false)) {
            eventGroup.shutdownGracefully()
        }
    }

    fun send(command: Command) {
        if (openned.get()) {
            channelFuture?.channel()?.writeAndFlush(OBJECT_MAPPER.writeValueAsString(command))
        }
    }

    inner class ClientHandler : SimpleChannelInboundHandler<String>() {
        override fun channelRead0(p0: ChannelHandlerContext?, p1: String) {
            println("Client received: " + p1)
        }

        override fun channelActive(channelHandlerContext: ChannelHandlerContext) {
            send(Command(commandType = CommandType.SESSION_INIT,
                         sessionInit = SessionInitCommand(sequenceNo = 0)))
        }

        override fun channelInactive(ctx: ChannelHandlerContext?) {
            println("Channel inactive")
        }

        override fun exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable) {
            print("Exception caught ${cause.message}")
            channelHandlerContext.channel().eventLoop().schedule({
                                                                     println("Reconnecting....")
                                                                     openned.set(false)
                                                                     open()
                                                                 }, 1, TimeUnit.SECONDS)
        }
    }
}

fun main(args: Array<String>) {

    val client = NettyClient("localhost", 1234)
    client.open()


//    sleep(5_000)
//    client.close()
}


