package ggpol2.file.client;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class FileClientMain {
	
	private static  Logger logger = Logger.getLogger(FileClientMain.class);
	
	//콜백 리턴값
	private static ArrayList<FileNameStatus>mArrFileList=null;
	
	
	
	public static void main(String[] args) throws Exception {
    	
    	PropertyConfigurator.configure("resources/log4j.properties");
    	
    	//업로드할 파일객체 등록
    	mArrFileList=new ArrayList<FileNameStatus>();
        mArrFileList.add(rtnFiNmaeStatus("c:\\Tulips.jpg"));
        mArrFileList.add(rtnFiNmaeStatus("E:\\UTIL\\ML-3475ND\\ML-3475_Print.zip"));
        mArrFileList.add(rtnFiNmaeStatus("E:\\UTIL\\PRINT_DRIVER\\ML1750.zip"));
        
    	
        
        
    	//콜백연결
        FileClientHandler.setAsyncCallBack(fileAsyncCallBack);
        
        
    	ExecutorService executor = Executors.newCachedThreadPool();
    	   
	    for(FileNameStatus obj : mArrFileList){
        	// 비동기로 실행될 코드
		    Callable<FileClient> callable =   new Callable<FileClient>(){
		          @Override
		          public FileClient call() throws Exception {
		              return new FileClient(obj.getStrFilePathName()).start();
		          }
		    };
		    
		    Thread t =new Thread( new Runnable(){
	             @Override
	             public void run() {
	            	 
	            	 final int taskCnt = 100;
	            	 int progressBarStatus=0;
	            	 String fileNmae = obj.getStrFilePathName();
	            	 
	            	 while(progressBarStatus<taskCnt){
	            		 progressBarStatus = obj.getnFilePercent();
	            		 try {
	                         Thread.sleep(100);
	                     } catch (InterruptedException e) {
	                         e.printStackTrace();
	                     }
	            		 
	            		 logger.info("fileName="+fileNmae+" progressBarStatus :"+progressBarStatus+"%");
	            	 }
	                 
	             }
	    	});
		    t.start();
		    
		    Future<FileClient> future = executor.submit(callable);
		    logger.info("future.isDone()"+future.isDone());
	
		    if(future.isDone()) {
			    try{
			    	t.join();
			    }catch(InterruptedException e){
			    	e.printStackTrace();
			    }
		    }
		    else {
		    	t.interrupt();
		    }
	
		    FileClient result = future.get();
	    }
	    
	    executor.shutdown();
	   
    }

	//콜백
	private static FileAsyncCallBack fileAsyncCallBack = new FileAsyncCallBack() {
	
	    @Override
	    public void onResult(FileNameStatus fileNameStatus) {
	    	 for(FileNameStatus obj : mArrFileList){
	    		 if(obj.getStrFilePathName().equals(fileNameStatus.getStrFilePathName())){
	    			 obj.setnFilePercent(fileNameStatus.getnFilePercent());
	    		 }
	    	 }
	    }
	
	    @Override
	    public boolean onStart(boolean bStart) {
	        return bStart;
	    }
	
	    @Override
	    public boolean onComplete(boolean bComp) {
	        return bComp;
	    }
	};
	
	//파일이름,진행상태 객체 리턴
	private static FileNameStatus rtnFiNmaeStatus(String strFilePathName){
		 
		FileNameStatus obj = new FileNameStatus();
		obj.setStrFilePathName(strFilePathName);
		obj.setnFilePercent(0);
		return obj;
	}

}
