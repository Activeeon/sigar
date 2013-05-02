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
import javax.management.MBeanServer;


public class SigarMBeanStarter {
    private static final int TIME_SECONDS = 60 * 10; // 10 minutes.

    public static void main(String args[]) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        SigarRegistry a = new SigarRegistry();
        a.registerMBeans(mbs);
        System.out.println("Sigar MBean running for " + TIME_SECONDS + " seconds...");
        System.out
                .println("Use jconsole to connect to the MBean server, and take a look at the domain 'sigar'.");
        Thread.sleep(1000 * TIME_SECONDS);
    }

}
