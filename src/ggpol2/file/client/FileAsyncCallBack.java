package ggpol2.file.client;


public interface FileAsyncCallBack {

 boolean onStart(boolean bStart);

 void onResult(FileNameStatus fileNameStatus);

 boolean onComplete(boolean bComp);
}
