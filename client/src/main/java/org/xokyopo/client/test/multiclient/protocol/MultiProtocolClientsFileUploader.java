package org.xokyopo.client.test.multiclient.protocol;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.netty.NettyClientConnection;
import org.xokyopo.clientservercommon.protocol.MyProtocolHandlerFactory;
import org.xokyopo.clientservercommon.protocol.executors.PAuthorizationExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFileListExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFileOperationExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFilePartClientExecutor;

import java.util.concurrent.CountDownLatch;

public class MultiProtocolClientsFileUploader {
    private final String repository = "client_repository";
    private final String remoteUrl = "localhost";
    private final int remotePort = 8999;
    public PFilePartClientExecutor filePartExecutor;
    public PFileListExecutor fileListExecutor;
    public PAuthorizationExecutor authorizationExecutor;
    public PFileOperationExecutor fileOperationExecutor;
    private Channel channel;
    private NettyClientConnection nettyClientConnection;

    private CountDownLatch runAwait;
    private String userName;
    private Long startTimer;

    public MultiProtocolClientsFileUploader(CountDownLatch runAwait, String userName) {
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
        int clientCount = 640; // max 640
        CountDownLatch countDownLatch = new CountDownLatch(clientCount);
        for (int i = 0; i < clientCount; i++) {
            final int f = i;
            new Thread(() -> new MultiProtocolClientsFileUploader(countDownLatch, ("client_" + f))).start();
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
                new MyProtocolHandlerFactory(
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
        this.authorizationExecutor = new PAuthorizationExecutor(this::acceptAuth, null);
        this.fileListExecutor = new PFileListExecutor(this::getRepository, null);
        this.fileOperationExecutor = new PFileOperationExecutor(this::getRepository, this::finishOperation);
        this.filePartExecutor = new PFilePartClientExecutor(this::getRepository, null);
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
        System.out.println(this.userName + ": авторизировался" + isAuth);
        this.runAwait.countDown();
    }

    private void runTest() throws Exception {
        this.run();
        this.runAwait.await();
//        this.startTimer = System.currentTimeMillis();
        this.filePartExecutor.sendFile("test", "", "", this.channel);
    }

    private void finishOperation(boolean b) {
//        long stopTimer = System.currentTimeMillis();
//        System.out.println(String.format("Передача файла %s завершилась за  %s мсек","test.iso", stopTimer - this.startTimer));
    }
}
