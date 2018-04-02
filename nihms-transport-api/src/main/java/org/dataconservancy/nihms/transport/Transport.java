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

import java.util.Map;

/**
 * Abstracts the transport protocol used to deposit a package with a target submission system.  Callers are able to
 * {@link #open(Map) open} a {@link TransportSession} by supplying configuration hints, which allows the implementation
 * to perform necessary connection initialization, prior to a package being transported using
 * {@link TransportSession#send(PackageStream, Map)}.
 */
public interface Transport {

    /**
     * Property identifying the value that can be used to look up authentication credentials by reference (i.e. the
     * value of this property serves as a key for an implementation to look up authentication credentials)
     */
    String TRANSPORT_SERVERID = "nihms.transport.serverid";

    /**
     * Property key that carries the user name used for authentication when using {@link AUTHMODE#userpass}.
     */
    String TRANSPORT_USERNAME = "nihms.transport.username";

    /**
     * Property key that carries the password used for authentication when using {@link AUTHMODE#userpass}.
     */
    String TRANSPORT_PASSWORD = "nihms.transport.password";

    /**
     * Property key that identifies the mode of authentication, the {@link Enum#name} form of {@link AUTHMODE}.
     */
    String TRANSPORT_AUTHMODE = "nihms.transport.authmode";

    /**
     * Property key that identifies the protocol used for transport, the {@link Enum#name} form of {@link PROTOCOL}.
     */
    String TRANSPORT_PROTOCOL = "nihms.transport.protocol";

    /**
     * Property identifying the server (or IP address) that the package will be deposited to
     */
    String TRANSPORT_SERVER_FQDN = "nihms.transport.server-fqdn";

    /**
     * Property identifying the TCP port that will be used by the transport to deposit the package
     */
    String TRANSPORT_SERVER_PORT = "nihms.transport.server-port";

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
        ftp
    }

    /**
     * Open a {@link TransportSession} with the underlying transport.  The returned {@code TransportSession} should be
     * ready to use by the caller, without the caller having to perform any further setup (the implementation of this
     * method should perform all necessary actions to allow {@link TransportSession#send(PackageStream, Map)} to
     * succeed).  The supplied {@code hints} may be used by the implementation to configure and open the session.  Well
     * known properties include those documented in {@link Transport}, and individual implementations may document
     * properties as well.
     * <p>
     * The returned {@code TransportSession} should be {@link TransportSession#closed() open}, otherwise implementations
     * are encouraged to throw a {@code RuntimeException} if a closed {@code TransportSession} would be returned.
     * </p>
     *
     * @param hints transport implementation configuration hints
     * @return an open {@code TransportSession}, ready to transfer packages using the underlying transport
     */
    TransportSession open(Map<String, String> hints);

}
