package org.xokyopo.client.test.multiclient.serialisation;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.netty.NettyClientConnection;
import org.xokyopo.clientservercommon.seirialization.MyHandlerFactory;
import org.xokyopo.clientservercommon.seirialization.executors.AuthorizationExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.FileListExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.FileOperationExecutor;
import org.xokyopo.clientservercommon.seirialization.executors.FilePartExecutor;

import java.util.concurrent.CountDownLatch;

public class MultiSerialisationClientsFileUploader {
    private final String repository = "client_repository";
    private final String remoteUrl = "localhost";
    private final int remotePort = 8999;
    public FilePartExecutor filePartExecutor;
    public FileListExecutor fileListExecutor;
    public AuthorizationExecutor authorizationExecutor;
    public FileOperationExecutor fileOperationExecutor;
    private Channel channel;
    private NettyClientConnection nettyClientConnection;
    private CountDownLatch runAwait;
    private String userName;
    private Long startTimer;

    public MultiSerialisationClientsFileUploader(CountDownLatch runAwait, String userName) {
        this.createExecutors();
        this.constructingServer();
        this.runAwait = runAwait;
        this.userName = userName;

        try {
            this.runTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //TODO max 640
        int clientCount = 600;
        CountDownLatch countDownLatch = new CountDownLatch(clientCount);
        for (int i = 0; i < clientCount; i++) {
            final int f = i;
            new Thread(() -> new MultiSerialisationClientsFileUploader(countDownLatch, ("client_" + f))).start();
        }
    }

    private void run() {
        Thread t = new Thread(() -> {
            try {
                this.nettyClientConnection.run(this.remoteUrl, this.remotePort);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void constructingServer() {
        this.nettyClientConnection = new NettyClientConnection(
                new MyHandlerFactory(
                        this::ifConnect,
                        this::ifDisconnect,
                        this.authorizationExecutor,
                        this.fileListExecutor,
                        this.fileOperationExecutor,
                        this.filePartExecutor
                )
        );
    }

    private void createExecutors() {
        this.authorizationExecutor = new AuthorizationExecutor(
                this::acceptAuth,
                null);

        this.fileListExecutor = new FileListExecutor(this::getRepository, null);

        this.fileOperationExecutor = new FileOperationExecutor(this::getRepository, this::finishOperation);

        this.filePartExecutor = new FilePartExecutor(
                this::getRepository,
                null,
                null,
                null
        );
    }

    private String getRepository(Channel channel) {
        return this.repository;
    }

    public void ifDisconnect(Channel channel) {
        this.channel = null;
    }

    public void ifConnect(Channel channel) {
        this.channel = channel;
        this.authorizationExecutor.sendLoginAndPass(this.userName, this.userName, channel);
    }

    public void acceptAuth(boolean isAuth) {
        this.runAwait.countDown();
    }

    private void runTest() throws Exception {
        this.run();
        this.runAwait.await();
//        this.startTimer = System.currentTimeMillis();
//        this.filePartExecutor.uploadFile("test","","", this.channel);
    }

    private void finishOperation(boolean b) {
//        long stopTimer = System.currentTimeMillis();
//        System.out.println(String.format("Передача файла %s завершилась за  %s мсек","test.iso", stopTimer - this.startTimer));
    }
}
