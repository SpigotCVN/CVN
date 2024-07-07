package io.github.spigotcvn.cvn.utils;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;

public class URLUtil {
    /**
     * Download a URL to a file
     * @param url The URL to download
     * @param downloadTo The output stream to download to
     */
    public static void download(URL url, OutputStream downloadTo) {
        try(InputStream in = url.openStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while((bytesRead = in.read(buffer)) != -1) {
                downloadTo.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Download a URL to a file
     * @param url The URL to download
     * @param downloadTo The file to download to
     */
    public static void download(URL url, File downloadTo) {
        try (OutputStream out = new FileOutputStream(downloadTo)) {
            download(url, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Download a URL to a file
     * @param url The URL to download
     * @param downloadTo The path to download to
     */
    public static void download(URL url, Path downloadTo) {
        download(url, downloadTo.toFile());
    }

    /**
     * Download a URL to a file
     * @param url The URL to download
     * @param downloadTo The path to download to
     */
    public static void download(URL url, String downloadTo) {
        download(url, new File(downloadTo));
    }
}
