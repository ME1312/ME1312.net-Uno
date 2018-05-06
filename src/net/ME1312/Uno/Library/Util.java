package net.ME1312.Uno.Library;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Uno Utility Class
 */
public final class Util {
    private Util(){}
    public interface ExceptionRunnable {
        void run() throws Throwable;
    }
    public interface ReturnRunnable<R> {
        R run();
    }

    /**
     * Checks values to make sure they're not null
     *
     * @param values Values to check
     * @return If any are null
     */
    public static boolean isNull(Object... values) {
        boolean ret = false;
        for (Object value : values) {
            if (value == null) ret = true;
        }
        return ret;
    }

    /**
     * Get keys by value from map
     *
     * @param map Map to search
     * @param value Value to search for
     * @param <K> Key
     * @param <V> Value
     * @return Search results
     */
    public static <K, V> List<K> getBackwards(Map<K, V> map, V value) {
        List<K> values = new ArrayList<K>();

        for (K key : map.keySet()) {
            if (map.get(key).equals(value)) {
                values.add(key);
            }
        }

        return values;
    }

    /**
     * Get an item from a map ignoring case
     *
     * @param map Map to search
     * @param key Key to search with
     * @param <V> Value
     * @return Search Result
     */
    public static <V> V getCaseInsensitively(Map<String, V> map, String key) {
        HashMap<String, String> insensitivity = new HashMap<String, String>();
        for (String item : map.keySet()) insensitivity.put(item.toLowerCase(), item);
        if (insensitivity.keySet().contains(key.toLowerCase())) {
            return map.get(insensitivity.get(key.toLowerCase()));
        } else {
            return null;
        }
    }

    /**
     * Gets a new Variable that doesn't match the existing Variables
     *
     * @param existing Existing Variables
     * @param generator Variable Generator
     * @param <V> Variable Type
     * @return Variable
     */
    public static <V> V getNew(Collection<? extends V> existing, ReturnRunnable<V> generator) {
        V result = null;
        while (result == null) {
            V tmp = generator.run();
            if (!existing.contains(tmp)) result = tmp;
        }
        return result;
    }

    /**
     * Read Everything from Reader
     *
     * @param rd Reader
     * @return Reader Contents
     * @throws IOException
     */
    public static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Copy from the Class Loader
     *
     * @param loader ClassLoader
     * @param resource Location From
     * @param destination Location To
     */
    public static void copyFromJar(ClassLoader loader, String resource, String destination) {
        InputStream resStreamIn = loader.getResourceAsStream(resource);
        File resDestFile = new File(destination);
        try {
            OutputStream resStreamOut = new FileOutputStream(resDestFile);
            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = resStreamIn.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            resStreamOut.close();
            resStreamIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Determines if an Exception will occur
     *
     * @param runnable Runnable
     * @return If an Exception occured
     */
    public static boolean isException(ExceptionRunnable runnable) {
        try {
            runnable.run();
            return false;
        } catch (Throwable e) {
            return true;
        }
    }

    /**
     * Delete Directory
     *
     * @param folder Location
     */
    public static void deleteDirectory(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) {
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    /**
     * Copy a Directory
     *
     * @param from Source
     * @param to Destination
     */
    public static void copyDirectory(File from, File to) {
        if (from.isDirectory()) {
            if (!to.exists()) {
                to.mkdirs();
            }

            String files[] = from.list();

            for (String file : files) {
                File srcFile = new File(from, file);
                File destFile = new File(to, file);

                copyDirectory(srcFile, destFile);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new FileInputStream(from);
                out = new FileOutputStream(to, false);

                byte[] buffer = new byte[1024];

                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                in.close();
                out.close();
            } catch (Exception e) {
                try {
                    if (in != null) in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                try {
                    if (out != null) out.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private static List<String> zipsearch(File origin, File file) {
        List<String> list = new LinkedList<String>();
        if (file.isFile()) {
            list.add(file.getAbsoluteFile().toString().substring(origin.getAbsoluteFile().toString().length()+1, file.getAbsoluteFile().toString().length()));
        }
        if (file.isDirectory()) for (File next : file.listFiles()) {
            list.addAll(zipsearch(origin, next));
        }
        return list;
    }

    public static void zip(File file, OutputStream zip) {
        byte[] buffer = new byte[1024];

        try{
            ZipOutputStream zos = new ZipOutputStream(zip);

            for(String next : zipsearch(file, file)){

                ZipEntry ze= new ZipEntry(next);
                zos.putNextEntry(ze);

                FileInputStream in = new FileInputStream(file.getAbsolutePath() + File.separator + next);

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                in.close();
            }

            zos.closeEntry();
            zos.close();
        } catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public static void unzip(InputStream zip, File dir) {
        byte[] buffer = new byte[1024];
        try{
            ZipInputStream zis = new ZipInputStream(zip);
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                File newFile = new File(dir + File.separator + ze.getName());
                if (newFile.exists()) {
                    if (newFile.isDirectory()) {
                        Util.deleteDirectory(newFile);
                    } else {
                        newFile.delete();
                    }
                }
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                } else if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
            }
            zis.closeEntry();
            zis.close();
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

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

    /**
     * Get a Random Integer
     *
     * @param min Minimum Value
     * @param max Maximum Value
     * @return Random Integer
     */
    public static int random(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    /**
     * Parse escapes in a Java String
     *
     * @param str String
     * @return Unescaped String
     */
    public static String unescapeJavaString(String str) {

        StringBuilder sb = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == str.length() - 1) ? '\\' : str
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                            && str.charAt(i + 1) <= '7') {
                        code += str.charAt(i + 1);
                        i++;
                        if ((i < str.length() - 1) && str.charAt(i + 1) >= '0'
                                && str.charAt(i + 1) <= '7') {
                            code += str.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= str.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + str.charAt(i + 2) + str.charAt(i + 3)
                                        + str.charAt(i + 4) + str.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
