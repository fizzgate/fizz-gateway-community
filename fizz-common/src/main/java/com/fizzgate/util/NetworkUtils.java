/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fizzgate.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author hongqiaowei
 */

public class NetworkUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtils.class);

    private static final int          maxServerId = 1023;

    private static       int          serverId    = -1;

    private static       String       serverIp;

    private static       Set<String>  serverIps   = new LinkedHashSet<>();

    private static final String       SERVER_IP   = "SERVER_IP";
    
    private static final String       LOCAL_IP    = "127.0.0.1";

    private NetworkUtils() {
    }

    /**
     * @return user settings, or the first non-local IP of IP address list.
     */
    public static String getServerIp() {
        if (serverIp == null) {
        	for (Iterator<String> iterator = getServerIps().iterator(); iterator.hasNext();) {
        		serverIp = iterator.next();
				if (!LOCAL_IP.equals(serverIp)) {
					break;
				}
			}
        }
        return serverIp;
    }

    public synchronized static Set<String> getServerIps() {
        if (serverIps.isEmpty()) {
            try {
                String ip = System.getProperty(SERVER_IP);
                if (StringUtils.isBlank(ip)) {
                    ip = System.getenv(SERVER_IP);
                }
                if (StringUtils.isBlank(ip)) {
                    Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                    while (nis.hasMoreElements()) {
                        NetworkInterface ni = (NetworkInterface) nis.nextElement();
                        Enumeration<InetAddress> ias = ni.getInetAddresses();
                        while (ias.hasMoreElements()) {
                            InetAddress ia = ias.nextElement();
                            if (ia.isSiteLocalAddress()) {
                                ip = ia.getHostAddress();
                                serverIps.add(ip);
                            }
                        }
                    }
                    if (serverIps.isEmpty()) {
                        InetAddress ia = InetAddress.getLocalHost();
                        ip = ia.getHostAddress();
                        serverIps.add(ip);
                    }
                } else {
                    serverIps.add(ip);
                }
                LOGGER.info("server ip: {}", serverIps);
            } catch (SocketException | UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return serverIps;
    }

    public static int getServerId() {
        if (serverId == -1) {
            try {
                StringBuilder b = ThreadContext.getStringBuilder();
                Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = nis.nextElement();
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        for (int i = 0; i < mac.length; i++) {
                            b.append(String.format("%02X", mac[i]));
                        }
                    }
                }
                serverId = b.toString().hashCode();
            } catch (Exception e) {
                serverId = (new SecureRandom().nextInt());
                LOGGER.error(null, e);
            }
            serverId = serverId & maxServerId;
            LOGGER.info("server id: {}", serverId);
        }
        return serverId;
    }
}
