package org.hyperic.sigar.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PFlagsUtils {
    private static final int MAXIMUM_PFLAG_VALUE_LENGTH = 256;

    public static String[] getListOfFlags(String path) {
        if (path == null)
            return new String[] {};

        File folder = new File(path);
        List<String> list = new ArrayList<String>();
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                list.add(listOfFiles[i].getName());
            }
        }
        return list.toArray(new String[] {});
    }

    public static boolean isValidFlagKey(String flag){
        return (!flag.contains("."));
    }
    
    public static String getFlagValue(String filepath) {
        // Return flag's content.
        FileReader fr = null;

        try {
            fr = new FileReader(filepath);
            try {
                if (fr.ready()) {
                    char[] cbuf = new char[MAXIMUM_PFLAG_VALUE_LENGTH];
                    fr.read(cbuf);
                    return new String(cbuf);
                }
            } catch (IOException e) {
            }
        } catch (FileNotFoundException e) {
        } finally {
            if (fr != null)
                try {
                    fr.close();
                } catch (IOException e) {
                }
        }
        return null;
    }
}
