package org.xokyopo.clientservercommon.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtil {
    public static String getHumanFileLength(double fileLength) {
        float baseLength = 1024;
        String[] prefixes = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};
        int count;
        for (count = 0; fileLength >= baseLength; count++) {
            fileLength = fileLength / baseLength;
        }
        return String.format("%.2f %s", fileLength, prefixes[count]);
    }

    public static void recurseDelete(Path path) throws IOException {
        List<Path> pathList = Files.walk(path).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (Path file : pathList) {
            Files.deleteIfExists(file);
        }
    }

    public static Path getFileShortPath(Path workDir, Path absolutePath) {
        if (workDir.getNameCount() == absolutePath.getNameCount() - 1) {
            return Paths.get("");
        } else {
            return absolutePath.subpath(workDir.getNameCount(), absolutePath.getNameCount() - 1);
        }
    }

    public static Path getFileShortPath(String workDir, String absolutePath) {
        return getFileShortPath(Paths.get(workDir), Paths.get(absolutePath));
    }

    public static Path getFileShortPath(String workDir, Path absolutePath) {
        return getFileShortPath(Paths.get(workDir), absolutePath);
    }

    public static Path getFileShortPath(Path workDir, String absolutePath) {
        return getFileShortPath(workDir, Paths.get(absolutePath));
    }
}
