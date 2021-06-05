package org.xokyopo.client.test.protoclient;

import io.netty.channel.Channel;
import org.xokyopo.clientservercommon.network.netty.NettyClientConnection;
import org.xokyopo.clientservercommon.protocol.MyProtocolHandlerFactory;
import org.xokyopo.clientservercommon.protocol.executors.PFileListExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFileOperationExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PFilePartClientExecutor;
import org.xokyopo.clientservercommon.protocol.executors.PStringExecutor;
import org.xokyopo.clientservercommon.utils.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class MyProtoClient {
    private final String repository = "client_repository";
    private final String hostName = "";
    private final int port = 8999;
    private NettyClientConnection nettyClientConnection;
    private Channel channel;

    private PStringExecutor pStringExecutor;
    private PFileListExecutor pFileListExecutor;
    private PFileOperationExecutor pFileOperationExecutor;
    private PFilePartClientExecutor pFilePartClientExecutor;


    public MyProtoClient() {
        this.createExecutors();
        this.connectionConfig();
    }

    public static void main(String[] args) throws IOException {
        MyProtoClient myProtoClient = new MyProtoClient();
        myProtoClient.run();

        while (true) {
            myProtoClient.applyCommand(myProtoClient.getUserInput());
        }
    }

    private void connectionConfig() {
        this.nettyClientConnection = new NettyClientConnection(
                new MyProtocolHandlerFactory(
                        this::ifConnect,
                        this::ifDisconnect,
                        this.pStringExecutor,
                        this.pFileListExecutor,
                        this.pFileOperationExecutor,
                        this.pFilePartClientExecutor
                )
        );
    }

    private void createExecutors() {
        this.pStringExecutor = new PStringExecutor(null);
        this.pFileListExecutor = new PFileListExecutor(this::getUserRepository, list -> list.forEach(f -> System.out.println(f.getName())));
        this.pFileOperationExecutor = new PFileOperationExecutor(this::getUserRepository, b -> System.out.println("опрасция выполнена: " + b));
        this.pFilePartClientExecutor = new PFilePartClientExecutor(
                this::getUserRepository,
                (b) -> {
                    System.out.println("файл загружен");
                },
                (fp, ffl, ftl) -> {
                    System.out.println(String.format("Получено %s из %s файла %s", FileUtil.getHumanFileLength(ftl), FileUtil.getHumanFileLength(ffl), fp));
                },
                (fp, ffl, ftl) -> {
                    System.out.println(String.format("Отправлено %s из %s файла %s", FileUtil.getHumanFileLength(ftl), FileUtil.getHumanFileLength(ffl), fp));
                });
    }

    private String getUserRepository(Channel channel) {
        Path path = Paths.get(this.repository);
        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path.toString();
    }

    private void run() {
        Thread thread = new Thread(() -> {
            try {
                this.nettyClientConnection.run(this.hostName, this.port);
            } catch (InterruptedException e) {
                System.out.println("MyProtoClient.run");
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void ifConnect(Channel channel) {
        this.channel = channel;
        System.out.println("Подключились к серверу");
    }

    public void ifDisconnect(Channel channel) {
        this.channel = null;
        System.out.println("Отключились к серверу");
    }

    public String getUserInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public void applyCommand(String userInput) throws IOException {
        if (userInput.startsWith("/dir ")) {
            String[] arg = userInput.split(" ", 2);
            System.out.println("open dir: " + arg[1]);
            this.pFileListExecutor.sendRequest(arg[1], this.channel);
        } else if (userInput.startsWith("/del ")) {
            String[] arg = userInput.split(" ", 2);
            this.pFileOperationExecutor.deleteFile(arg[1], this.channel);
        } else if (userInput.startsWith("/ren ")) {
            String[] arg = userInput.split(" ", 3);
            this.pFileOperationExecutor.moveFile(arg[1], arg[2], this.channel);
        } else if (userInput.startsWith("/lod ")) {
            String[] arg = userInput.split(" ", 4);
            String fileName = arg[1];
            String fileSource = arg[2];
            String fileDest = arg[3];
            this.pFilePartClientExecutor.getFile(fileName, fileSource, fileDest, channel);
        } else if (userInput.startsWith("/upl ")) {
            String[] arg = userInput.split(" ", 4);
            String fileName = arg[1];
            String fileSource = arg[2];
            String fileDest = arg[3];
            this.pFilePartClientExecutor.sendFile(fileName, fileSource, fileDest, this.channel);
        } else {
            this.pStringExecutor.send(userInput, this.channel);
        }
    }
}
