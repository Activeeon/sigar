/*
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
 * This file is part of SIGAR.
 * 
 * SIGAR is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.sigar.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarLoader;
import org.hyperic.sigar.SigarLog;
import org.hyperic.sigar.SigarProxy;

public class SigarRegistry extends AbstractMBean {
    private static final Map VERSION_ATTRS = new LinkedHashMap();

    private static final MBeanConstructorInfo MBEAN_CONSTR_DEFAULT = new MBeanConstructorInfo(
        SigarRegistry.class.getName(), "Creates a new instance of this class. Will create the Sigar "
            + "instance this class uses when constructing other MBeans", new MBeanParameterInfo[0]);
    private static final MBeanInfo MBEAN_INFO = new MBeanInfo(
        SigarRegistry.class.getName(),
        "Sigar MBean registry. Provides a central point for creation and destruction of Sigar MBeans. Any Sigar MBean created via this instance will automatically be cleaned up when this instance is deregistered from the MBean server.",
        getAttributeInfo(), new MBeanConstructorInfo[] { MBEAN_CONSTR_DEFAULT }, null, null);

    private static final String MBEAN_TYPE = "SigarRegistry";

    private final String objectName;

    private final ArrayList managedBeans;

    private static MBeanAttributeInfo[] getAttributeInfo() {
        VERSION_ATTRS.put("JarVersion", "1.6.2.151");
        VERSION_ATTRS.put("NativeVersion", Sigar.NATIVE_VERSION_STRING);
        VERSION_ATTRS.put("JarBuildDate", "2009-01-17_03-06-10");
        VERSION_ATTRS.put("NativeBuildDate", Sigar.NATIVE_BUILD_DATE);
        VERSION_ATTRS.put("JarSourceRevision", "3981");
        VERSION_ATTRS.put("NativeSourceRevision", Sigar.NATIVE_SCM_REVISION);
        VERSION_ATTRS.put("NativeLibraryName", SigarLoader.getNativeLibraryName());

        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[VERSION_ATTRS.size()];
        int i = 0;
        Iterator it = VERSION_ATTRS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String name = (String) entry.getKey();
            attrs[(i++)] = new MBeanAttributeInfo(name, entry.getValue().getClass().getName(), name, true,
                false, false);
        }

        return attrs;
    }

    public SigarRegistry() {
        super(new Sigar(), (short) 3);
        this.objectName = "sigar:Type=SigarRegistry";

        this.managedBeans = new ArrayList();
    }

    public String getObjectName() {
        return this.objectName;
    }

    public Object getAttribute(String attr) throws AttributeNotFoundException {
        Object obj = VERSION_ATTRS.get(attr);
        if (obj == null) {
            throw new AttributeNotFoundException(attr);
        }
        return obj;
    }

    public MBeanInfo getMBeanInfo() {
        return MBEAN_INFO;
    }

    private void registerMBean(AbstractMBean mbean) {
        try {
            ObjectName name = new ObjectName(mbean.getObjectName());

            if (this.mbeanServer.isRegistered(name)) {
                return;
            }
            ObjectInstance instance = this.mbeanServer.registerMBean(mbean, name);

            this.managedBeans.add(instance.getObjectName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void postRegister(Boolean success) {
        super.postRegister(success);

        if (!success.booleanValue()) {
            return;
        }
        CpuInfo[] info;
        try {
            info = this.sigar.getCpuInfoList();
        } catch (SigarException e) {
            throw unexpectedError("CpuInfoList", e);
        }

        for (int i = 0; i < info.length; i++) {
            String idx = String.valueOf(i);
            ReflectedMBean mbean = new ReflectedMBean(this.sigarImpl, "CpuCore", idx);
            mbean.setType("CpuList");
            registerMBean(mbean);
            
            mbean = new ReflectedMBean(this.sigarImpl, "CpuCoreUsage", idx);
            mbean.setType("CpuPercList");
            registerMBean(mbean);
        }

        ReflectedMBean mbean = new ReflectedMBean(this.sigarImpl, "Cpu");
        mbean.putAttributes(info[0]);
        registerMBean(mbean);

        mbean = new ReflectedMBean(this.sigarImpl, "CpuUsage");
        mbean.setType("CpuPerc");
        registerMBean(mbean);

        try {
            FileSystem[] fslist = this.sigarImpl.getFileSystemList();
            for (int i = 0; i < fslist.length; i++) {
                FileSystem fs = fslist[i];
                if (fs.getType() == 2) {
                    String name = fs.getDirName();
                    mbean = new ReflectedMBean(this.sigarImpl, "FileSystem", name);

                    mbean.setType(mbean.getType() + "Usage");
                    mbean.putAttributes(fs);
                    registerMBean(mbean);
                }
            }
        } catch (SigarException e) {
            SigarLog.getLogger(this.getClass().getName()).warn("FileSystemList", e);
        }

        try {
            String[] ifnames = this.sigarImpl.getNetInterfaceList();
            for (int i = 0; i < ifnames.length; i++) {
                String name = ifnames[i];
                NetInterfaceConfig ifconfig = this.sigar.getNetInterfaceConfig(name);
                try {
                    this.sigarImpl.getNetInterfaceStat(name);
                } catch (SigarException e) {
                    continue;
                }
                mbean = new ReflectedMBean(this.sigarImpl, "NetInterface", name);

                mbean.setType(mbean.getType() + "Stat");
                mbean.putAttributes(ifconfig);
                registerMBean(mbean);
            }
        } catch (SigarException e) {
            SigarLog.getLogger(this.getClass().getName()).warn("NetInterfaceList", e);
        }

        mbean = new ReflectedMBean(this.sigarImpl, "NetInfo");
        try {
            mbean.putAttribute("FQDN", this.sigarImpl.getFQDN());
        } catch (SigarException e) {
        }
        
        registerMBean(mbean);

        registerMBean(new ReflectedMBean(this.sigarImpl, "Mem"));

        registerMBean(new ReflectedMBean(this.sigarImpl, "PFlags"));
        
        registerMBean(new ReflectedMBean(this.sigarImpl, "Swap"));

        registerMBean(new SigarLoadAverage(this.sigarImpl));

        registerMBean(new ReflectedMBean(this.sigarImpl, "ProcStat"));
    }

    public void preDeregister() throws Exception {
        for (int i = this.managedBeans.size() - 1; i >= 0; i--) {
            ObjectName next = (ObjectName) this.managedBeans.remove(i);
            if (this.mbeanServer.isRegistered(next)) {
                try {
                    this.mbeanServer.unregisterMBean(next);
                } catch (Exception e) {
                }
            }
        }

        super.preDeregister();
    }

    public void registerMBeans(MBeanServer mbs) {
        try {
            // Create the SIGAR registry
            SigarRegistry registry = new SigarRegistry();

            ObjectName name = new ObjectName(registry.getObjectName());
            if (!mbs.isRegistered(name)) {
                mbs.registerMBean(registry, name);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
