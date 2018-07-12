package com.wxx.server;

import java.net.InetAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 开启一个http服务器
 * 
 * @author 万祥新
 *
 */
public class StartHttpServer {
	protected static final int BIZGROUPSIZE = Runtime.getRuntime().availableProcessors() * 2;
	protected static final int BIZTHREADSIZE = 4;
	private static final EventLoopGroup bossGroup = new NioEventLoopGroup(BIZGROUPSIZE);
	private static final EventLoopGroup workerGroup = new NioEventLoopGroup(BIZTHREADSIZE);
	public static void main(String[] args) {
		initConfig(args);
		
		try {
			String ip = InetAddress.getLocalHost().getHostAddress();
			int port = (int) ConstantUtil.configMap.get(ConstantUtil.PORT);
			String path = (String) ConstantUtil.configMap.get(ConstantUtil.PATH);
			ServerBootstrap httpServer = new ServerBootstrap();
			httpServer.group(bossGroup, workerGroup);
			httpServer.channel(NioServerSocketChannel.class);
			httpServer.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					pipeline.addLast("decoder", new HttpRequestDecoder());
					pipeline.addLast(new HttpObjectAggregator(65536));
					pipeline.addLast(new HttpResponseEncoder());
					pipeline.addLast(new ChunkedWriteHandler());
					pipeline.addLast("handler", new HttpRequestHandler("/"+path));
				}
			});
			
			ChannelFuture c = httpServer.bind(InetAddress.getByName(ip), port).sync();
			String format="http://%s:%s/%s";
			System.out.println("http服务器已经启动>>>"+String.format(format, ip, port, path));
			c.channel().closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();

		}
	}
	private static void initConfig(String[] args) {
		ConstantUtil.configMap.put(ConstantUtil.PORT, 2500);
		ConstantUtil.configMap.put(ConstantUtil.PATH, "test");
		
		if(args != null) {
			if(args.length >= 1) {
				try {
					int port = Integer.parseInt(args[0]);
					if(port<0 || port>25535) {
						System.out.println("端口号错误，将使用默认端口");
					}else {
						ConstantUtil.configMap.put(ConstantUtil.PORT, port);
					}
				}catch(Exception e) {
					System.out.println("端口号错误，将使用默认端口");
				}
			}
			if(args.length >= 2) {
				ConstantUtil.configMap.put(ConstantUtil.PATH, args[1]);
			}
		}
		
		System.out.println(ConstantUtil.configMap);
	}
}
