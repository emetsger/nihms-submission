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

import static org.dataconservancy.nihms.transport.Transport.TRANSPORT_PASSWORD;
import static org.dataconservancy.nihms.transport.Transport.TRANSPORT_USERNAME;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Sword2Transport implements Transport {

    private Sword2ClientFactory clientFactory;

    public Sword2Transport(Sword2ClientFactory clientFactory) {
        if (clientFactory == null) {
            throw new IllegalArgumentException("SWORD client factory must not be null.");
        }
        this.clientFactory = clientFactory;
    }

    /**
     * Hints must carry:
     * <ul>
     *     <li>Service document URL</li>
     * </ul>
     * @param hints
     * @return
     */
    @Override
    public TransportSession open(Map<String, String> hints) {
        SWORDClient client = clientFactory.newInstance(hints);
        String serviceDocUrl = getServiceDocUrl(hints);

        if (!AUTHMODE.userpass.name().equals(hints.get(TRANSPORT_AUTHMODE))) {
            throw new RuntimeException("This transport only supports AUTHMODE " + AUTHMODE.userpass.name() +
                    " (was: '" + hints.get(TRANSPORT_AUTHMODE) + "'");
        }

        ServiceDocument serviceDocument = null;
        try {
            serviceDocument = client.getServiceDocument(serviceDocUrl,
                    new AuthCredentials(hints.get(TRANSPORT_USERNAME), hints.get(TRANSPORT_PASSWORD)));
        } catch (SWORDClientException|ProtocolViolationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return new Sword2TransportSession(client, serviceDocument);
    }

    /**
     * Obtains the SWORD Service Document URL from the supplied hints, or throws a {@code RuntimeException}.
     *
     * @param hints the transport configuration hints
     * @return the SWORD service document URL
     * @throws RuntimeException if the hints do not contain the service document url
     */
    private String getServiceDocUrl(Map<String, String> hints) {
        if (!hints.containsKey(Sword2TransportHints.SWORD_SERVICE_DOC_URL)) {
            throw new RuntimeException("SWORD transport hints does not contain a Service Document URL!");
        }

        String serviceDocUrl = hints.get(Sword2TransportHints.SWORD_SERVICE_DOC_URL);
        if (serviceDocUrl == null || serviceDocUrl.trim().length() == 0) {
            throw new RuntimeException("SWORD transport hints contained a null or empty string for the Service " +
                    "Document URL (check the value of property key '" + Sword2TransportHints.SWORD_SERVICE_DOC_URL +
                    "'");
        }

        return serviceDocUrl;
    }
}
