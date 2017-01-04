/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.apm.tests.dockerized;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class for accessing ip addresses of given interface.
 *
 * @author Pavol Loffay
 */
public class InterfaceIpV4Address {

    /**
     * interface name -> ip addresses
     */
    private static Map<String, List<InetAddress>> interfaceIpAddressMap;


    private InterfaceIpV4Address() {
    }

    /**
     * @param intfc The interface name
     * @return List of ipv4 addresses of given interface. Only site local addresses are returned.
     */
    public static List<InetAddress> getIpAddresses(String intfc) {
        if (interfaceIpAddressMap == null) {
            interfaceIpAddressMap = init();
        }

        return interfaceIpAddressMap.get(intfc);
    }

    private static Map<String, List<InetAddress>> init() {
        Map<String, List<InetAddress>> interfacesIpAddressesMap = new HashMap<>();

        Enumeration networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            throw new RuntimeException("Failed to get network interfaces", ex);
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) networkInterfaces.nextElement();
            networkInterface.getDisplayName();

            List<InetAddress> ipAddresses = new ArrayList<>();

            Enumeration ee = networkInterface.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress inetAddress = (InetAddress) ee.nextElement();
                if (inetAddress.isSiteLocalAddress()) {
                    ipAddresses.add(inetAddress);
                }
            }

            interfacesIpAddressesMap.put(networkInterface.getDisplayName(), Collections.unmodifiableList(ipAddresses));
        }

        return Collections.unmodifiableMap(interfacesIpAddressesMap);
    }
}
