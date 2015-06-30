package ggpol2.file.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.FutureListener;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * Simple SSL chat client modified from {@link TelnetClient}.
 */
public final class FileClient {

	
	private Logger logger = Logger.getLogger(this.getClass());
		
	private int mPort;
	
	private String mFilePathName;
		
    
    public FileClient(String filePathName) {
    	this.mPort = (FileClientConstants.IS_SSL ? FileClientConstants.SSL_PORT : FileClientConstants.PORT);
		this.mFilePathName=filePathName;
		
    }
    
    public FileClient start() throws Exception {
    	
    	File f_certificate = new File("src/client.pem");
   	 	File f_privatekey = new File("src/clientkey.pem");
    	
        // Configure SSL.
        //final SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        //final SslContext sslCtx = SslContextBuilder.forClient().trustManager(f_certificate).build();
   	 	
   	 	final SslContext sslCtx;
   	 	
   	 	if(FileClientConstants.IS_SSL){
   	 		sslCtx=SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
   	 		//sslCtx=SslContextBuilder.forClient().keyManager(f_certificate, f_privatekey,"12345").build();
   	 	}else{
   	 		sslCtx=null;
   	 	}
   	 		
   	 	
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                   @Override
                   public void initChannel(SocketChannel ch) throws Exception {
                       ChannelPipeline p = ch.pipeline();
                       if (sslCtx != null) {
                           p.addLast(sslCtx.newHandler(ch.alloc(), FileClientConstants.HOST, mPort));
                       }
                       p.addLast(new ChunkedWriteHandler(),
                    		     new FileClientHandler(mFilePathName));
                   }
               });
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);   
            
           

            // Start the connection attempt.
            ChannelFuture f = b.connect(FileClientConstants.HOST, mPort).sync();
            
           
           
            
            logger.info("f.isDone() "+f.isDone());
            
                      
            if (f.isCancelled()) {
                // Connection attempt cancelled by user
            	 logger.fatal("f.isCancelled()");
            } else if (!f.isSuccess()) {
                logger.fatal("Netty Error !!!!!");
                f.cause().printStackTrace();
            } else {
                // Connection established successfully
            	logger.info("Netty Connection Success !!");
            }
           
           
            f.channel().closeFuture().sync();
            
           
            
            
            
            
           
        } finally {
            // The connection is closed automatically on shutdown.
        	logger.info("~~group.shutdownGracefully() ");
            group.shutdownGracefully();
        }
        
        return this;
    	
    }
    
    
    
   
}

