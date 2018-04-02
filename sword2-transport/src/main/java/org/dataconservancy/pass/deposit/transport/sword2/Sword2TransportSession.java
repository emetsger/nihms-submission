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

import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.transport.TransportResponse;
import org.dataconservancy.nihms.transport.TransportSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.Deposit;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDError;
import org.swordapp.client.ServiceDocument;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Sword2TransportSession implements TransportSession {

    private static final Logger LOG = LoggerFactory.getLogger(Sword2TransportSession.class);

    private static final String SPEC_URL_CREATING_SWORD_BINARY =
            "http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_creatingresource_binary";

    private static final String WARN_MISSING_SHOULD = "SWORD v2 deposit request is missing HTTP request header '%s' " +
            "recommended as SHOULD by %s";

    private boolean closed = false;

    private SWORDClient client;

    private ServiceDocument serviceDocument;

    private AuthCredentials authCreds;

    public Sword2TransportSession(SWORDClient client, ServiceDocument serviceDocument, AuthCredentials authCreds) {
        if (client == null) {
            throw new IllegalArgumentException("SWORDClient must not be null.");
        }

        if (serviceDocument == null) {
            throw new IllegalArgumentException("ServiceDocument must not be null.");
        }

        if (authCreds == null) {
            throw new IllegalArgumentException("AuthCredentials must not be null.");
        }

        this.client = client;
        this.serviceDocument = serviceDocument;
        this.authCreds = authCreds;
    }

    /**
     * <pre>
     * // Collection URI?  How does the client select the collection?  Hard-coded property?  Why would all submissions
     * // go to the same collection?  Some logic for looking at the package and selecting the appropriate collection?
     * // TODO: hints must carry (at least initially) hard-coded url to the SWORD collection being deposited to
     * Deposit deposit = new Deposit();
     * deposit.setFile(new FileInputStream(myFile)); // content stream
     * deposit.setMimeType("application/zip"); // metadata - obtained from PackageInputStream, fallback to TRANSPORT_MIME_TYPE property
     * deposit.setFilename("example.zip"); // destination resource? or obtain from PackageInputStream, fall back to 'SWORDV2_FILENAME'?  Used for content-disposition headers
     * deposit.setPackaging(UriRegistry.PACKAGE_SIMPLE_ZIP); // metadata - obtained from PackageInputStream.spec, fall back to 'SWORDV2_PACKAGE_SPEC';
     * deposit.setMd5(fileMD5);  // metadata, or from package stream
     * deposit.setInProgress(true); // ??  false if we are submitting a package
     * deposit.setSuggestedIdentifier("abcdefg"); // ??  Slug, same as deposit.setFilename?
     * </pre>
     *
     * @param packageStream
     * @param metadata
     * @return
     * @throws IllegalStateException if this session has been {@link #close() closed}
     * @see <a href="http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_creatingresource_binary">SWORD v2 Profile</a>
     */
    @Override
    public Sword2DepositReceiptResponse send(PackageStream packageStream, Map<String, String> metadata) {
        if (closed) {
            throw new IllegalStateException("SWORDv2 transport session has been closed.");
        }

        Deposit swordDeposit = new Deposit();
        PackageStream.Metadata streamMetadata = packageStream.metadata();

        // Satisfy MUSTs from
        // http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_creatingresource_binary

        if (streamMetadata.name() == null || streamMetadata.name().length() == 0) {
            throw new IllegalStateException("PackageStream MUST have a name() per " + SPEC_URL_CREATING_SWORD_BINARY);
        } else {
            swordDeposit.setFilename(streamMetadata.name());
        }

        // Log violations of SHOULD from
        // http://swordapp.github.io/SWORDv2-Profile/SWORDProfile.html#protocoloperations_creatingresource_binary

        if (streamMetadata.mimeType() == null || streamMetadata.mimeType().length() == 0) {
            LOG.warn(String.format(WARN_MISSING_SHOULD, "Content-Type", SPEC_URL_CREATING_SWORD_BINARY));
        } else {
            swordDeposit.setMimeType(streamMetadata.mimeType());
        }

        streamMetadata
                .checksums()
                .stream()
                .filter(sum -> PackageStream.Algo.MD5 == sum.algorithm())
                .findFirst().ifPresent(md5 -> swordDeposit.setMd5(md5.asHex()));

        if (swordDeposit.getMd5() == null) {
            LOG.warn(String.format(WARN_MISSING_SHOULD, "Content-MD5", SPEC_URL_CREATING_SWORD_BINARY));
        }

        if (streamMetadata.spec() == null || streamMetadata.spec().length() == 0) {
            LOG.warn(String.format(WARN_MISSING_SHOULD, "Packaging", SPEC_URL_CREATING_SWORD_BINARY));
        } else {
            swordDeposit.setPackaging(streamMetadata.spec());
        }

        // Other headers

        if (streamMetadata.sizeBytes() > -1) {
            swordDeposit.setContentLength(streamMetadata.sizeBytes());
        }

        swordDeposit.setInProgress(false);

        DepositReceipt receipt = null;

        try (InputStream stream = packageStream.open()) {
            swordDeposit.setFile(stream);
            receipt = client.deposit(selectCollection(serviceDocument, metadata), swordDeposit, authCreds);
        } catch (IOException e) {
            throw new RuntimeException("Error closing PackageStream: " + e.getMessage(), e);
        } catch (ProtocolViolationException|SWORDClientException|SWORDError e) {
            throw new RuntimeException("Error depositing SWORD package to '" + selectCollection(serviceDocument, metadata).getHref().toASCIIString() + "': " + e.getMessage(), e);
        }

        return new Sword2DepositReceiptResponse(receipt);
    }

    @Override
    public boolean closed() {
        return this.closed;
    }

    @Override
    public void close() throws Exception {
        if (this.closed()) {
            return;
        }

        this.closed = true;
    }

    /**
     * Selects the APP Collection that the SWORD deposit is being submitted to.
     *
     * TODO: see how DSpace communities and collections map to APP workspaces and collections
     * @param serviceDoc
     * @param metadata
     * @return
     */
    SWORDCollection selectCollection(ServiceDocument serviceDoc, Map<String, String> metadata) {
        // return the Collection that the package should be deposited to
        return null;
    }
}
