package org.xokyopo.server.protocol;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.netty.NettyServerConnection;
import org.xokyopo.clientservercommon.protocol.MyProtocolHandlerFactory;
import org.xokyopo.clientservercommon.protocol.executors.PAuthorizationExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFileListExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFileOperationExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFilePartServerExecutor;
import org.xokyopo.clientservercommon.utils.FileUtil;
import org.xokyopo.server.dao.DataBaseManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyProtoServer {
    private final String repository = "E:/server_repository";
    //    private final String repository = "server_repository";
    private final int serverPort = 8999;
    private final Map<Channel, String> userDirs;
    private final boolean printTransferFileLog = true;
    private PAuthorizationExecutor authorizationExecutor;
    private PFileOperationExecutor fileOperationExecutor;
    private PFilePartServerExecutor filePartExecutor;
    private PFileListExecutor fileListExecutor;
    private NettyServerConnection nettyServerConnection;

    public MyProtoServer() {
        this.createExecutors();
        this.constructingServer();
        this.userDirs = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        MyProtoServer myServer = new MyProtoServer();
        try {
            System.out.println("запускаю сервер");
            myServer.run();
        } catch (InterruptedException e) {
            System.out.println("Server start error");
            e.printStackTrace();
        }
    }

    public void run() throws InterruptedException {
        try {
            DataBaseManager.connection();
            nettyServerConnection.run(this.serverPort);
        } finally {
            DataBaseManager.disconnection();
        }
    }

    private void constructingServer() {
        this.nettyServerConnection = new NettyServerConnection(
                new MyProtocolHandlerFactory(
                        (ch) -> {
                        },
                        this::ifDisconnect,
                        this.authorizationExecutor,
                        this.fileListExecutor,
                        this.fileOperationExecutor,
                        this.filePartExecutor
                )
        );
    }

    private void createExecutors() {
        this.authorizationExecutor = new PAuthorizationExecutor(null, this::authorisationMethod);
        this.fileListExecutor = new PFileListExecutor(this::getUserRepository, null);
        this.fileOperationExecutor = new PFileOperationExecutor(this::getUserRepository, null);
        this.filePartExecutor = new PFilePartServerExecutor(this::getUserRepository);

        this.filePartExecutor.setInputStats((fileName, fileFullLength, fileCurrentLength) -> this.printStatistic("Получено", fileName.toString(), fileFullLength, fileCurrentLength));
        this.filePartExecutor.setOutputStats((fileName, fileFullLength, fileCurrentLength) -> this.printStatistic("Отправлено", fileName.toString(), fileFullLength, fileCurrentLength));
    }

    private boolean authorisationMethod(String login, String password, Channel channel) {
        boolean auth = this.checkLoginAndPassword(login, Integer.toString(password.hashCode()));
        if (auth) {
            this.userDirs.put(channel, login);
            System.out.println("Подключился пользователь: \t" + login + "\t всего сейчас подключено:" + this.userDirs.size());
        }
        return auth;
    }

    private boolean checkLoginAndPassword(String login, String password) {
        String pass = DataBaseManager.getUserPassword(login);
        if (pass == null) {
            DataBaseManager.addClient(login, password);
            return true;
        } else {
            return pass.equals(password);
        }
    }

    private String getUserRepository(Channel channel) {
        Path path = Paths.get(this.repository, this.userDirs.get(channel));
        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path.toString();
    }

    private void printStatistic(String msg, String filename, long fileFullLength, long fileCurrentLength) {
        if (this.printTransferFileLog) {
            System.out.println(String.format(
                    "%s %s из %s файла %s",
                    msg,
                    FileUtil.getHumanFileLength(fileCurrentLength),
                    FileUtil.getHumanFileLength(fileFullLength),
                    filename
            ));
        }
    }

    public void ifDisconnect(Channel channel) {
        System.out.println("Отключился пользователь:\t" + this.userDirs.get(channel));
        this.userDirs.remove(channel);
    }
}
