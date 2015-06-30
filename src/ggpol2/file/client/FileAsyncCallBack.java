package ggpol2.file.client;


public interface FileAsyncCallBack {

 boolean onStart(boolean bStart);
 
 void onStop(String filePathName);

 void onResult(FileNameStatus fileNameStatus);

 boolean onComplete(boolean bComp);
}
