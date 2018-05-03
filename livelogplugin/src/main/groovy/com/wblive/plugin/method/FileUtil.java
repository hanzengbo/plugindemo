package com.wblive.plugin.method;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by zengbo1 on 2018/5/3.
 */

public class FileUtil {

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }
    public static void writeFile(String filePath, String content) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(filePath, true);
            fw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
