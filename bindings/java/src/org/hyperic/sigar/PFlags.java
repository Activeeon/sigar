package org.hyperic.sigar;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.util.PFlagsUtils;


/**
 * Each pflag is a regular key:value tuple with monitoring information of a 
 * process running on the same host where this Sigar instance is. 
 * A pflag exists only if a corresponding pflag file exists. The value of 
 * the pflag is exactly the same as the content of the corresponding file, and its 
 * key is exactly the same as the name of the file.
 * The file contains monitoring information about the process. 
 * This file's content will be exposed through the Sigar MBean so
 * other users have a regular way of accessing a process' monitoring 
 * information running on the host.
 * Example: 
 *    /path/to/pflags/apache-instance-cpu-usage   << contains '0.3'
 */
public class PFlags {
    public static final String PFLAGS_DIR_KEY = "sigar.pflag.path";
    private static final String SEP;
    
    static {
        SEP = File.separator;
    }

    /**
     * Path of the directory where pflags files are stored. 
     */
    private String path;

    public PFlags() throws SigarException {
        String ppath = System.getProperty(PFLAGS_DIR_KEY);
        setPath(ppath);
    }

    public PFlags(String path) throws SigarException {
        setPath(path);
    }

    private void setPath(String path) {
        File f = new File(path);
        if (f.exists() && f.isDirectory()) {
            this.path = path;
        } else {
            this.path = null;
        }
    }

    /**
     * Get the pflags map. 
     * It will contain key:value where key represents all the 
     * pflag files in the chosen path, and value represents the 
     * content of each of these files (monitoring information).
     * @return
     */
    public Map<String, String> getPFlags() {
        Map<String, String> map = new HashMap<String, String>();

        if (path == null)
            return map;

        String[] pflags = PFlagsUtils.getListOfFlags(path);
        for (String pflag : pflags) {
            if (!PFlagsUtils.isValidFlagKey(pflag))
                continue;

            String value = PFlagsUtils.getFlagValue(path + SEP + pflag);

            if (value != null)
                map.put(pflag, value.trim());
        }
        return map;
    }
}
