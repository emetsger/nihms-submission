/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.nihms.transport;

import org.dataconservancy.nihms.assembler.PackageStream;

import java.io.InputStream;
import java.util.Map;

/**
 * Abstracts the transport protocol used to deposit a package with a target submission system.
 */
public interface Transport {

    String TRANSPORT_SERVERID = "nihms.transport.serverid";

    /**
     * Property key carrying the user name used for authentication when using {@link AUTHMODE#userpass}.
     */
    String TRANSPORT_USERNAME = "nihms.transport.username";

    /**
     * Property key carrying the password used for authentication when using {@link AUTHMODE#userpass}.
     */
    String TRANSPORT_PASSWORD = "nihms.transport.password";

    /**
     * Property key identifying the mode of authentication; value encoded as the {@link Enum#name} form of {@link AUTHMODE}.
     */
    String TRANSPORT_AUTHMODE = "nihms.transport.authmode";

    /**
     * Property key identifying the protocol used for transport, value encoded as the {@link Enum#name} form of {@link PROTOCOL}.
     */
    String TRANSPORT_PROTOCOL = "nihms.transport.protocol";

    /**
     * Property key identifying the fully qualified domain name or IP address of the server to connect to for
     * depositing packages or files to.
     */
    String TRANSPORT_SERVER_FQDN = "nihms.transport.server-fqdn";

    /**
     * Property key identifying the port of the server to connect to for depositing packages or files to.
     */
    String TRANSPORT_SERVER_PORT = "nihms.transport.server-port";

    /**
     * Property key identifying the mime type of the {@code InputStream} being deposited by
     * {@link TransportSession#send(String, InputStream)} and {@link TransportSession#send(String, Map, InputStream)}.
     * <em>N.B.</em>: The preferred form of obtaining the name of the {@code InputStream} would be
     * {@link PackageStream.Metadata#name()}.
     */
    String TRANSPORT_MIME_TYPE = "deposit.transport.mime-type";

    /**
     * Property key identifying the packaging spect of the {@code InputStream} being deposited by {@link
     * TransportSession#send(String, InputStream)} and {@link TransportSession#send(String, Map, InputStream)}.
     * <em>N.B.</em>: The preferred form of obtaining the packaging specification of the {@code InputStream} would
     * be {@link PackageStream.Metadata#spec()}.
     */
    String TRANSPORT_PACKAGE_SPEC = "deposit.transport.package-spec";


    enum AUTHMODE {

        /**
         * The implementation will use the username and password from {@link #TRANSPORT_USERNAME} and {@link #TRANSPORT_PASSWORD}
         */
        userpass,

        /**
         * The transport implementation will perform authentication implicitly
         */
        implicit,

        /**
         * The transport implementation will look up authentication credentials using an implementation-specific
         * reference (e.g. a {@link #TRANSPORT_SERVERID} that can be used to look up authentication credentials)
         */
        reference

    }

    enum PROTOCOL {
        http,
        https,
        ftp,
        swordv2
    }

    TransportSession open(Map<String, String> hints);

}
