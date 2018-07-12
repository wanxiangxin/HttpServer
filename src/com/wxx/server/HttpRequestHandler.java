package com.wxx.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.multipart.MemoryAttribute;

/**
 * http数据的处理
 * 
 * @author 万祥新
 *
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private String uri;
	private static int count =1;

	public HttpRequestHandler(String uri) {
		this.uri = uri;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		// System.out.println(request);
		if (request.getUri().indexOf(uri) != 0) {
			return;
		}

		Map<String, String> requestParams = new HashMap<>();

		if (request.getMethod() == HttpMethod.GET) {
			QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
			Map<String, List<String>> parame = decoder.parameters();
			Iterator<Entry<String, List<String>>> iterator = parame.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, List<String>> next = iterator.next();
				requestParams.put(next.getKey(), next.getValue().get(0));
			}
		} else if (request.getMethod() == HttpMethod.POST) {
			HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
			List<InterfaceHttpData> postData = decoder.getBodyHttpDatas();
			for (InterfaceHttpData data : postData) {
				if (data.getHttpDataType() == HttpDataType.Attribute) {
					MemoryAttribute attribute = (MemoryAttribute) data;
					requestParams.put(attribute.getName(), attribute.getValue());
				}
			}
		}
		FullHttpResponse httpResponse = null;
		String responseContent = readFile();
		httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
				Unpooled.wrappedBuffer(responseContent.getBytes("GBK")));
		httpResponse.headers().set("Content-Type", "text/html; charset=GBK");
		httpResponse.headers().set("Content-Length", httpResponse.content().readableBytes());
		ctx.writeAndFlush(httpResponse);
		logTime();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel incoming = ctx.channel();
		System.out.println("Client:" + incoming.remoteAddress() + "异常");
		cause.printStackTrace();
		ctx.close();
	}
	
	private static void logTime() {
		String format = "(时间%s)";
		String time = String.format(format, new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));
		System.out.println("第"+count+"被访问  "+time);
		count++;
	}
	
	public static String readFile() {
		File file = new File(new File(System.getProperty("user.dir")), "data.json");
	    String fileContent = "未知错误";
	    BufferedReader reader = null;
	    try {
	    	if(!file.exists()) {
	    		return "文件不存在";
	    	}
	    	
	    	if(!file.isFile()) {
	    		return "不是一个文件";
	    	}
	    	
	    	InputStreamReader read = new InputStreamReader(
	            new FileInputStream(file), "utf-8");
	        reader = new BufferedReader(read);
	        StringBuilder stringBuilder = new StringBuilder();
	        String tmp = "";
	        boolean isFirst = true;
	        while ((tmp = reader.readLine()) != null) {
	        	if(!isFirst) {
	        		stringBuilder.append("\n");
	        	}
	        	stringBuilder.append(tmp);
	        	isFirst = false;
	        }
	        fileContent = stringBuilder.toString();
	    } catch (Exception e) {
	    	fileContent = e.getMessage();
	        e.printStackTrace();
	    } finally {
	    	if(reader != null) {
	    		try {
	    			reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    }
	    return fileContent;
	  }
}