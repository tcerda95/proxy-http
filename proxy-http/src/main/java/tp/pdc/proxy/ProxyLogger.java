package tp.pdc.proxy;

import tp.pdc.proxy.header.Method;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProxyLogger {

    private static final Executor loggerExecutor;
    private static final Path accessLogPath, errorLogPath;

    static {
        loggerExecutor = Executors.newSingleThreadExecutor();
        accessLogPath = Paths.get("access-log.log");
        errorLogPath = Paths.get("error-log.log");
    }

    //TODO: client ip o algo. Recurso en el host
    public static void LogAccess(Method method, InetSocketAddress destination, int statusCode, int responseBytes, String userAgent) {
        loggerExecutor.execute(() -> {
            Date time = new Date();
            StringBuilder builder = new StringBuilder();
            builder.append(time.toString())
                .append(" - ")
                .append(method)
                .append(" [")
                .append(destination.getHostName())
                .append(":")
                .append(destination.getPort())
                .append("]")
                .append(" S:").append(statusCode)
                .append(" B:").append(responseBytes)
                .append(" UA:" ).append(userAgent)
                .append("\n");

            try {
                Files.write(accessLogPath, builder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                //TODO no se pudo loggear;
            }
        });
    }

    public static void LogError(String errorMessage, Method method, InetSocketAddress destination, int statusCode) {
        loggerExecutor.execute(() -> {
            Date time = new Date();
            StringBuilder builder = new StringBuilder();
            builder.append(time.toString())
                .append(" - ")
                .append(" [error] ")
                .append(" request: ")
                .append(method)
                .append(" ")
                .append(destination.getHostName())
                .append(":")
                .append(destination.getPort())
                .append(" S:").append(statusCode)
                .append("\n");

            try {
                Files.write(errorLogPath, builder.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                //TODO no se pudo loggear;
            }
        });
    }

    public static void main (String[] args) {
        ProxyProperties p = ProxyProperties.getInstance();
        for (int i = 0; i < 5; i++) {
            ProxyLogger.LogAccess(Method.getByBytes(ByteBuffer.wrap("GET".getBytes()), 3), new InetSocketAddress("www.google.com", 80), 321, 43513, "Chrome-6.1.4");
            ProxyLogger.LogError("Se rompió todo amigo", Method.getByBytes(ByteBuffer.wrap("GET".getBytes()), 3), new InetSocketAddress("infobae.com", 80), 500);
        }
    }
}
