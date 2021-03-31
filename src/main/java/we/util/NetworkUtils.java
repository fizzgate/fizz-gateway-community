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

package we.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Enumeration;

/**
 * @author hongqiaowei
 */

public class NetworkUtils {

    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    private static final int    maxServerId = 1023;

    private static       int    serverId    = -1;

    private static       String serverIp;

    private static final String SERVER_IP = "SERVER_IP";

    public static String getServerIp() {
        try {
            if (serverIp == null) {
                serverIp = System.getenv(SERVER_IP);
                log.info("SERVER_IP is " + serverIp);
                if (StringUtils.isBlank(serverIp)) {
                    boolean found = false;
                    Enumeration<NetworkInterface> nis = null;
                    nis = NetworkInterface.getNetworkInterfaces();
                    while (nis.hasMoreElements()) {
                        NetworkInterface ni = (NetworkInterface) nis.nextElement();
                        Enumeration<InetAddress> ias = ni.getInetAddresses();
                        while (ias.hasMoreElements()) {
                            InetAddress ia = ias.nextElement();
                            if (ia.isSiteLocalAddress()) {
                                serverIp = ia.getHostAddress();
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            break;
                        }
                    }
                    if (!found) {
                        InetAddress ia = InetAddress.getLocalHost();
                        serverIp = ia.getHostAddress();
                    }
                }
            }
            return serverIp;
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
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
                log.error(null, e);
            }
            serverId = serverId & maxServerId;
            log.info("server id is " + serverId);
        }
        return serverId;
    }
}
