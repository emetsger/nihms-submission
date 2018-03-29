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
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.ServiceDocument;

import java.io.InputStream;
import java.util.Map;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Sword2TransportSession implements TransportSession {

    private boolean closed = false;

    private SWORDClient client;

    private ServiceDocument serviceDocument;

    public Sword2TransportSession(SWORDClient client, ServiceDocument serviceDocument) {
        this.client = client;
        this.serviceDocument = serviceDocument;
    }

    /**
     * <pre>
     * // Collection URI?  How does the client select the collection?  Hard-coded property?  Why would all submissions
     * // go to the same collection?  Some logic for looking at the package and selecting the appropriate collection?
     * // TODO: hints must carry (at least initially) hard-coded url to the SWORD collection being deposited to
     * Deposit deposit = new Deposit();
     * deposit.setFile(new FileInputStream(myFile)); // content stream
     * deposit.setMimeType("application/zip"); // metadata - obtained from PackageInputStream, fallback to TRANSPORT_MIME_TYPE property
     * deposit.setFilename("example.zip"); // destination resource? or obtain from PackageInputStream, fall back to 'SWORDV2_FILENAME'?
     * deposit.setPackaging(UriRegistry.PACKAGE_SIMPLE_ZIP); // metadata - obtained from PackageInputStream.spec, fall back to 'SWORDV2_PACKAGE_SPEC';
     * deposit.setMd5(fileMD5);  // metadata, or from package stream
     * deposit.setInProgress(true); // ??  false if we are submitting a package
     * deposit.setSuggestedIdentifier("abcdefg"); // ??
     * </pre>
     *
     * @param packageStream
     * @param metadata
     * @return
     * @throws IllegalStateException if this session has been {@link #close() closed}
     */
    @Override
    public TransportResponse send(PackageStream packageStream, Map<String, String> metadata) {
        if (closed) {
            throw new IllegalStateException("SWORDv2 transport session has been closed.");
        }
        return null;
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
