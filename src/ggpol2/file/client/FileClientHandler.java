package ggpol2.file.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class FileClientHandler extends SimpleChannelInboundHandler<Object>{
	
	private Logger logger = Logger.getLogger(this.getClass());
	
	//1MB POOLED 버퍼
	ByteBuf mPoolBuf;
	ByteBuf mBuffer;
	long mOffest=0L;
	
	//던져줄 진행율
    private volatile int mPercent;
	
	private long mFileLenth=0L;
	String mFilePathName;
	
	
	
	//콜백
    private static FileAsyncCallBack mFileAsyncCallBack =null;
    
	//생성자
    public FileClientHandler(String filePathName){
        this.mFilePathName=filePathName;
        
        mPoolBuf=PooledByteBufAllocator.DEFAULT.directBuffer(1048576);
    }
    
    
    //콜벡 셋팅
    public static void setAsyncCallBack(FileAsyncCallBack fileAsyncCallBack){
        mFileAsyncCallBack = fileAsyncCallBack;
    }
    
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("handlerAdded");

		mBuffer = mPoolBuf.alloc().buffer(4096);
		//mBuffer = ctx.alloc().buffer(4096);
	}
	

    @Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		logger.debug("handlerRemoved");

		if (ctx.channel().isActive()){
            logger.debug("!!!!!! ctx.channel().isActive()");
            ctx.close();
            mPoolBuf.release();
            mPoolBuf=null;
        }
				
		
		
	}
	
	/**
     * Protocol 초기화
     * [4byte|?byte|8byte|chunk ?byte]
     * [filenamelen|filenamedata|filedatalen|filedatadatachunks]
     *
     * @param file
     * @return
     */
    private ByteBuf initializeProtocol(File file) {
    	
    	
    	String f_name ="";
    	try {
    		f_name = URLEncoder.encode(file.getName(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	//ByteBuf buffer = m_pool_buf.alloc.buffer(file.getName().length() + 12);
    	
    	ByteBuf buffer = mPoolBuf.alloc().buffer(FileClientConstants.INIT_BUF_SIZE);
    	buffer.writeInt(f_name.length());		//파일이름 길이(4)
    	buffer.writeBytes(f_name.getBytes());	//파일이름에따라 틀림
        buffer.writeLong(file.length());		//파일크기(8)
        buffer.writeZero(buffer.capacity()-buffer.writerIndex());  //나머지 부분을 0으로 셋팅해서 버퍼크기를 맞춤
        logger.debug("buffer.writerIndex()"+buffer.writerIndex());
        
        return buffer;
    }
    
    
    @Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
    	 logger.debug("channelActive");
    	 logger.debug("mFilePathName:"+mFilePathName);
    	 
    	 File file  = new File(mFilePathName);
    	   
    	
    	 RandomAccessFile raf = null;
         long fileLength = -1;
         try {
               raf = new RandomAccessFile(file, "r");
             fileLength = raf.length();
         } catch (Exception e) {
        	  logger.fatal(e.getMessage());
              return;
         } finally {
             if (fileLength < 0 && raf != null) {
                    raf.close();
             }
         }

         mFileLenth=fileLength;
         
    	 
    	 ByteBuf bufferHead = initializeProtocol(file);
    	 
    	 // Write the initial line and the header.
         ctx.writeAndFlush(bufferHead);

         // Write the content.
         final ChannelFuture sendFileFuture;
         ChannelFuture lastContentFuture;
         
         if (ctx.pipeline().get(SslHandler.class) == null) {
        	 
        	 logger.debug("modern transfer");
        	 
        	 FileInputStream fis = new FileInputStream(file);
        	 
        	 mBuffer.writeBytes(fis, (int) Math.min(fileLength - mOffest,mBuffer.writableBytes()));
        	 mOffest += mBuffer.writerIndex();
        	 sendFileFuture = ctx.writeAndFlush(mBuffer);
        	 mBuffer.clear();
        	 sendFileFuture.addListener(new ChannelFutureListener(){
        		 private long offset=mOffest;
        	          	     
				@Override
				public void operationComplete(ChannelFuture future)	throws Exception {
					// TODO Auto-generated method stub
					 if (!future.isSuccess()) {
						 	logger.fatal("FAIL!!");
			                future.cause().printStackTrace();
			                future.channel().close();
			                fis.close();
			                return;
			            }
					 
					 ByteBuf buffer =  ctx.alloc().buffer(FileClientConstants.SND_BUF_SIZE);
					 //ByteBuf buffer =mPoolBuf.alloc().buffer(4096);
					 //buffer.clear();						 					 
				
					 int nWriteLen = (int)Math.min(mFileLenth-offset,buffer.writableBytes());
					 
					 mPercent=(int)(offset * 100.0 / mFileLenth + 0.5);
					 
					 //콜백에 전달
					 FileNameStatus fileNameStauts = new FileNameStatus();
					 fileNameStauts.setStrFilePathName(mFilePathName);
					 fileNameStauts.setnFilePercent(mPercent);
					 
					 mFileAsyncCallBack.onResult(fileNameStauts);
					 
					 //logger.debug("SENDING: offset["+offset+"] fileLength["+mFileLenth+"] buffer.writableBytes()["+buffer.writableBytes()+"]");

					 buffer.clear();
					 buffer.writeBytes(fis,nWriteLen);
					 offset += buffer.writerIndex();
					 					 				
					 //ChannelFuture chunkWriteFuture=future.channel().writeAndFlush(buffer);
					 ChannelFuture chunkWriteFuture=future.channel().writeAndFlush(buffer);
 		             if (offset < mFileLenth) {
 		            	 chunkWriteFuture.addListener(this);
			         } else {
			                // Wrote the last chunk - close the connection if thewrite is done.
			                logger.info("DONE:fileName["+mFilePathName+"] fileLength["+mFileLenth+"] offset["+offset+"]");
			                
			                fileNameStauts.setnFilePercent(100);
			                mFileAsyncCallBack.onResult(fileNameStauts);
			                
			                chunkWriteFuture.addListener(ChannelFutureListener.CLOSE);
			                fis.close();
			                //ctx.close(); //close하면 안됨
			         }
			          
				}
        		 
          	  });
        	 
         } else {
        	
        	 
        	 logger.info("ssl transfer");
             sendFileFuture = ctx.writeAndFlush(new ChunkedFile(raf, 0, fileLength, FileClientConstants.SND_BUF_SIZE),ctx.newProgressivePromise());
             sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
	             @Override
	             public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
	            	 
	            	 mPercent=(int)((progress*100)/mFileLenth);
	            	 
	                 if (total < 0) { // total unknown
	                    //logger.debug(future.channel() + " Transfer progress: " + progress+" Percentage: " + (int)mPercent+ "%");
	                 } else {
	                	 //logger.info(future.channel() + " Transfer progress: " + progress + " / " + total);
	                	 //logger.debug("int progress:"+(int)progress+" int total:"+(int)total+" Percentage: " + (int)mPercent+ "%");
	                 }
	                	                 	                 
 	                 //콜백에 전달
					 FileNameStatus fileNameStauts = new FileNameStatus();
					 fileNameStauts.setStrFilePathName(mFilePathName);
					 fileNameStauts.setnFilePercent(mPercent);
					 mFileAsyncCallBack.onResult(fileNameStauts);
	             }
	
	             @Override
	             public void operationComplete(ChannelProgressiveFuture future) {
	                 logger.info(future.channel() + " Transfer complete.");
	                 
	                 //콜백에 전달
	                 FileNameStatus fileNameStauts = new FileNameStatus();
					 fileNameStauts.setStrFilePathName(mFilePathName);
	                 fileNameStauts.setnFilePercent(mPercent);
					 mFileAsyncCallBack.onResult(fileNameStauts);
	                 
	                 sendFileFuture.addListener(ChannelFutureListener.CLOSE);
	                 
	             }
	         });
             
             //lastContentFuture = sendFileFuture;
             //lastContentFuture.addListener(ChannelFutureListener.CLOSE);
             //자원 해제
             //ctx.close();
             
         }
         
         
    	
         
      
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg)	throws Exception {
		// TODO Auto-generated method stub
		logger.debug("channelRead0");
		
	
		ByteBuf m = (ByteBuf) msg;
		
		ByteBuf buf=ctx.alloc().buffer(FileClientConstants.INIT_BUF_SIZE);
		
		buf.writeBytes(m);
		

		if(buf.readableBytes()>=FileClientConstants.INIT_BUF_SIZE){
			
			//파일이름 길이
			int nNameLen = buf.readInt();
			
			//파일이름
	        byte[] bytesName = new byte[nNameLen];
	        buf.readBytes(bytesName);
	       
	        String file_string = "";
	        for (int i = 0; i < nNameLen; i ++) {
	          
	        	file_string += (char)bytesName[i];
	        }
	        String newFileName=URLDecoder.decode(file_string,"UTF-8");
	        logger.info("newFileName :"+newFileName);
	        
	        //공백으로 채워진 곳 읽기
	        byte[] zeroBytes = new byte[buf.readableBytes()];
	        buf.readBytes(zeroBytes);
		
		}//if
		
	}


	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		logger.info("exceptionCaught");
        cause.printStackTrace();
        ctx.close();
    }
    

	

}
