/*
 * Copyright 2018 Johns Hopkins University
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
package org.dataconservancy.pass.deposit.transport.sword2;

import org.dataconservancy.nihms.transport.Transport;
import org.dataconservancy.nihms.transport.TransportSession;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.ServiceDocument;

import java.util.Map;

import static org.dataconservancy.pass.deposit.transport.sword2.Sword2TransportHints.SWORD_SERVICE_DOC_URL;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Sword2Transport implements Transport {

    static final String MISSING_REQUIRED_HINT = "Missing required transport hint '%s'";

    private Sword2ClientFactory clientFactory;

    public Sword2Transport(Sword2ClientFactory clientFactory) {
        if (clientFactory == null) {
            throw new IllegalArgumentException("SWORD client factory must not be null.");
        }
        this.clientFactory = clientFactory;
    }

    /**
     * Hints <em>must</em> carry:
     * <ul>
     *     <li>Service document URL</li>
     *     <li>Username and pass for retrieving service doc</li>
     * </ul>
     * Hints may carry:
     * <ul>
     *     <li>on-behalf-of user</li>
     * </ul>
     * @param hints
     * @return
     */
    @Override
    public Sword2TransportSession open(Map<String, String> hints) {
        SWORDClient client = clientFactory.newInstance(hints);
        String serviceDocUrl = getServiceDocUrl(hints);

        if (!AUTHMODE.userpass.name().equals(hints.get(TRANSPORT_AUTHMODE))) {
            throw new IllegalArgumentException("This transport only supports AUTHMODE " + AUTHMODE.userpass.name() +
                    " (was: '" + hints.get(TRANSPORT_AUTHMODE) + "'");
        }

        if (hints.get(TRANSPORT_USERNAME) == null || hints.get(TRANSPORT_USERNAME).trim().length() == 0) {
            throw new IllegalArgumentException(String.format(MISSING_REQUIRED_HINT, TRANSPORT_USERNAME));
        }

        if (hints.get(TRANSPORT_PASSWORD) == null || hints.get(TRANSPORT_PASSWORD).trim().length() == 0) {
            throw new IllegalArgumentException(String.format(MISSING_REQUIRED_HINT, TRANSPORT_PASSWORD));
        }

        ServiceDocument serviceDocument = null;
        AuthCredentials authCreds = null;
        try {
            if (hints.containsKey(Sword2TransportHints.SWORD_ON_BEHALF_OF_USER) &&
                    (hints.get(Sword2TransportHints.SWORD_ON_BEHALF_OF_USER) != null) &&
                    (hints.get(Sword2TransportHints.SWORD_ON_BEHALF_OF_USER).trim().length() > 0)) {
                authCreds = new AuthCredentials(hints.get(TRANSPORT_USERNAME), hints.get(TRANSPORT_PASSWORD),
                        hints.get(Sword2TransportHints.SWORD_ON_BEHALF_OF_USER));
            } else {
                authCreds = new AuthCredentials(hints.get(TRANSPORT_USERNAME), hints.get(TRANSPORT_PASSWORD));
            }

            serviceDocument = client.getServiceDocument(serviceDocUrl, authCreds);
        } catch (SWORDClientException|ProtocolViolationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return new Sword2TransportSession(client, serviceDocument, authCreds);
    }

    /**
     * Obtains the SWORD Service Document URL from the supplied hints, or throws a {@code RuntimeException}.
     *
     * @param hints the transport configuration hints
     * @return the SWORD service document URL
     * @throws RuntimeException if the hints do not contain the service document url
     */
    private String getServiceDocUrl(Map<String, String> hints) {
        if (hints.get(SWORD_SERVICE_DOC_URL) == null || hints.get(SWORD_SERVICE_DOC_URL).trim().length() == 0) {
            throw new IllegalArgumentException(String.format(MISSING_REQUIRED_HINT, SWORD_SERVICE_DOC_URL));
        }

        return hints.get(SWORD_SERVICE_DOC_URL);
    }
}
