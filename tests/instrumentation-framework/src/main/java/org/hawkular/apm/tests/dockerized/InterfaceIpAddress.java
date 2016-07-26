/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for accessing ip addresses of given interface.
 *
 * @author Pavol Loffay
 */
public class InterfaceIpAddress {

    private static Map<String, Set<String>> interfaceIpAddressMap;


    private InterfaceIpAddress() {
    }

    /**
     * @param intfc The interface name
     * @return
     */
    public static Set<String> getIpAddresses(String intfc) {
        if (interfaceIpAddressMap == null) {
            interfaceIpAddressMap = init();
        }

        return interfaceIpAddressMap.get(intfc);
    }


    private static Map<String, Set<String>> init() {
        Map<String, Set<String>> interfacesIpAddressesMap = new HashMap<>();

        Enumeration networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            throw new RuntimeException("Failed to get network interfaces", ex);
        }
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) networkInterfaces.nextElement();
            networkInterface.getDisplayName();

            Set<String> ipAddresses = new HashSet<>();

            Enumeration ee = networkInterface.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress inetAddress = (InetAddress) ee.nextElement();
                ipAddresses.add(inetAddress.getHostAddress());
            }

            interfacesIpAddressesMap.put(networkInterface.getDisplayName(), Collections.unmodifiableSet(ipAddresses));
        }

        return Collections.unmodifiableMap(interfacesIpAddressesMap);
    }
}
