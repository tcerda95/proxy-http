package tp.pdc.proxy;

import com.sun.xml.internal.ws.spi.db.FieldSetter;
import tp.pdc.proxy.header.Method;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyLogger {

    private static final ExecutorService loggerExecutor;
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

            String log = builder.toString();
            try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(accessLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                out.write(log.getBytes(), 0, log.length());
            } catch (IOException x) {
                System.err.println(x);
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

            String log = builder.toString();
            try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(errorLogPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                out.write(log.getBytes(), 0, log.length());
            } catch (IOException x) {
                System.err.println(x);
            }
        });
    }

    public static void main (String[] args) {
        // TODO: ver excepción rara si saco este proxyProperties
        ProxyProperties p = ProxyProperties.getInstance();
        for (int i = 0; i < 5; i++) {
            ProxyLogger.LogAccess(Method.GET, new InetSocketAddress("www.google.com", 80), 321, 43513, "Chrome-6.1.4");
            ProxyLogger.LogError("Se rompió todo amigo", Method.GET, new InetSocketAddress("infobae.com", 80), 500);
        }
        //TODO: Medio raro que no se cierra solo
        loggerExecutor.shutdown();
    }
}
