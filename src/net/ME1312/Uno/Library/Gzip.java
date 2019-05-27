package net.ME1312.Uno.Library;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Gzip Utility Class
 */
public final class Gzip {
    private Gzip(){}

    public static byte[] gzip(String data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(data.getBytes());
        gzip.close();
        byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }

    public static String ungzip(InputStream compressed) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(compressed);
        BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        gis.close();
        return sb.toString();
    }
}
