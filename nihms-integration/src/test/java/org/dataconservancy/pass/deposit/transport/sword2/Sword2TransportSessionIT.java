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

import org.apache.commons.codec.digest.DigestUtils;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.integration.BaseIT;
import org.dataconservancy.nihms.transport.TransportResponse;
import org.junit.Before;
import org.junit.Test;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.ClientConfiguration;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.ServiceDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sword2TransportSessionIT extends BaseIT {

    /**
     * User the SWORD client will authenticate as
     */
    private static final String DSPACE_ADMIN_USER = "dspace-admin@oapass.org";

    /**
     * Password the SWORD client will authenticate with
     */
    private static final String DSPACE_ADMIN_PASSWORD = "foobar";

    /**
     * A hard-coded classpath resource that resolves to a package composed of a simple zip file according to the
     * packaging specification {@link #SPEC_SIMPLE_ZIP}.
     */
    private static final String SIMPLE_ZIP_PACKAGE_RESOURCE = "simplezippackage.zip";

    /**
     * Package specification URI identifying a simple zip file.
     */
    private static final String SPEC_SIMPLE_ZIP = "http://purl.org/net/sword/package/SimpleZip";

    /**
     * Mime type of zip files.
     */
    private static final String APPLICATION_ZIP = "application/zip";

    /**
     * FIXME: Hard-coded (for now) SWORD Service Document endpoint
     */
    private static final String SERVICEDOC_ENDPOINT = "http://192.168.99.100:8181/swordv2/servicedocument";

    /**
     * FIXME: Hard-coded (for now) SWORD Collection being deposited to
     */
    private static final String SWORD_COLLECTION_URL = "http://192.168.99.100:8181/swordv2/collection/123456789/2";

    /**
     * Configured SWORD client
     */
    private SWORDClient swordClient;

    /**
     * Authentication credentials used by the client when communicating with a SWORD endpoint.  This object encapsulates
     * the On-Behalf-Of semantics..
     */
    private AuthCredentials authCreds;

    /**
     * SWORD Service document retrieved from {@link #SERVICEDOC_ENDPOINT}.
     */
    private ServiceDocument serviceDoc;

    /**
     * Resolved package identified by {@link #SIMPLE_ZIP_PACKAGE_RESOURCE}
     */
    private File sampleZipPackage;

    /**
     * Performs basic IT setup:
     * <ul>
     *     <li>Discovers and makes available sample packages as {@code File} objects.</li>
     *     <li>Provides basic {@code SWORDClient} configuration, including authentication credentials.</li>
     *     <li>Retrieves the {@code ServiceDocument}.</li>
     * </ul>
     */
    @Before
    public void setUp() {
        sampleZipPackage = new File(this.getClass().getResource(SIMPLE_ZIP_PACKAGE_RESOURCE).getPath());
        assertNotNull(sampleZipPackage);
        assertTrue("Missing sample package; cannot resolve '" + SIMPLE_ZIP_PACKAGE_RESOURCE +
                        "' as a class path resource.", sampleZipPackage.exists());


        ClientConfiguration swordConfig = new ClientConfiguration();
        swordConfig.setReturnDepositReceipt(true);
        swordConfig.setUserAgent("oapass/SWORDv2");

        authCreds = new AuthCredentials(DSPACE_ADMIN_USER, DSPACE_ADMIN_PASSWORD);

        swordClient = new SWORDClient(swordConfig);

        serviceDoc = getServiceDocument(swordClient, SERVICEDOC_ENDPOINT, authCreds);
    }

    /**
     * Deposits a package using the 'SimpleZip' packaging specification.  The response should be successful, and the
     * {@link org.swordapp.client.DepositReceipt} non-null.
     */
    @Test
    public void testSimple() throws FileNotFoundException {
        PackageStream.Metadata md = preparePackageMd(sampleZipPackage, SPEC_SIMPLE_ZIP, APPLICATION_ZIP);
        PackageStream packageStream = preparePackageStream(md, sampleZipPackage);

        Sword2TransportSession underTest = new Sword2TransportSession(swordClient, serviceDoc, authCreds);

        Map<String, String> transportMd = new HashMap<>();
        transportMd.put(Sword2TransportHints.SWORD_COLLECTION_URL,
                SWORD_COLLECTION_URL);

        Sword2DepositReceiptResponse response = (Sword2DepositReceiptResponse)
                underTest.send(packageStream, transportMd);
        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.getReceipt());
    }

    /**
     * Purposefully supply an invalid checksum when depositing a package.  The response should indicate failure, and
     * provide a Throwable that contains the {@link org.swordapp.client.SWORDError#errorBody error body}, with a message
     * indicating a checksum failure.
     */
    @Test
    public void testWithBadMd5Checksum() throws FileNotFoundException {
        PackageStream.Metadata md = preparePackageMd(sampleZipPackage, SPEC_SIMPLE_ZIP, APPLICATION_ZIP);
        PackageStream.Checksum invalidChecksum = new PackageStream.Checksum() {
            @Override
            public PackageStream.Algo algorithm() {
                return PackageStream.Algo.MD5;
            }

            @Override
            public byte[] value() {
                return new byte[128];
            }

            @Override
            public String asBase64() {
                return encodeBase64String(value());
            }

            @Override
            public String asHex() {
                return DigestUtils.md5Hex("hello world!");
            }
        };
        when(md.checksum()).thenReturn(invalidChecksum);
        when(md.checksums()).thenReturn(Collections.singletonList(invalidChecksum));

        PackageStream packageStream = preparePackageStream(md, sampleZipPackage);

        Sword2TransportSession underTest = new Sword2TransportSession(swordClient, serviceDoc, authCreds);

        Map<String, String> transportMd = new HashMap<>();
        transportMd.put(Sword2TransportHints.SWORD_COLLECTION_URL,
                SWORD_COLLECTION_URL);

        TransportResponse response = underTest.send(packageStream, transportMd);

        assertNotNull(response);
        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error().getMessage().contains("MD5 checksum for the deposited file did not match"));
    }

    /**
     * Purposefully supply an invalid packaging specification when depositing a package.  The response should indicate
     * failure, and provide a Throwable that contains the {@link org.swordapp.client.SWORDError#errorBody error body},
     * with a message indicating an invalid packaging spec was supplied.
     */
    @Test
    public void testWithUnsupportedPackagingType() throws FileNotFoundException {
        PackageStream.Metadata md = preparePackageMd(sampleZipPackage, SPEC_SIMPLE_ZIP, APPLICATION_ZIP);
        when(md.spec()).thenReturn("http://invalid.spec/url");
        PackageStream packageStream = preparePackageStream(md, sampleZipPackage);

        Sword2TransportSession underTest = new Sword2TransportSession(swordClient, serviceDoc, authCreds);

        Map<String, String> transportMd = new HashMap<>();
        transportMd.put(Sword2TransportHints.SWORD_COLLECTION_URL,
                SWORD_COLLECTION_URL);

        TransportResponse response = underTest.send(packageStream, transportMd);

        assertNotNull(response);
        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error().getMessage().contains("Unacceptable packaging type in deposit request"));
    }

    /**
     * Purposefully deposit to an invalid collection URL.  Response should indicate failure, and the cause of the
     * failure should be an {@link InvalidCollectionUrl} exception.
     */
    @Test
    public void testDepositToNonExistentCollectionUrl() throws FileNotFoundException {
        PackageStream.Metadata md = preparePackageMd(sampleZipPackage, SPEC_SIMPLE_ZIP, APPLICATION_ZIP);
        PackageStream packageStream = preparePackageStream(md, sampleZipPackage);

        Sword2TransportSession underTest = new Sword2TransportSession(swordClient, serviceDoc, authCreds);
        String invalidCollectionUrl = SWORD_COLLECTION_URL + "/123456";

        Map<String, String> transportMd = new HashMap<>();
        transportMd.put(Sword2TransportHints.SWORD_COLLECTION_URL, invalidCollectionUrl);

        TransportResponse response = underTest.send(packageStream, transportMd);

        assertNotNull(response);
        assertFalse(response.success());
        assertNotNull(response.error());
        assertTrue(response.error() instanceof InvalidCollectionUrl);
    }

    /**
     * Generate the package stream for the supplied file.  The supplied {@code md} will be returned in response to
     * {@link PackageStream#metadata()}.
     *
     * @param md
     * @param packageFile
     * @return
     * @throws FileNotFoundException
     */
    private static PackageStream preparePackageStream(PackageStream.Metadata md, File packageFile)
            throws FileNotFoundException {
        PackageStream packageStream = mock(PackageStream.class);

        when(packageStream.open()).thenReturn(new FileInputStream(packageFile));
        when(packageStream.metadata()).thenReturn(md);
        return packageStream;
    }

    /**
     * Generate the package metadata for the supplied file.  The returned metadata will use {@code packageSpec} for the
     * {@link PackageStream.Metadata#spec()}, and {@code packageMimeType} for {@link PackageStream.Metadata#mimeType()}.
     *
     * @param packageFile
     * @param packageSpec
     * @param packageMimeType
     * @return
     */
    private static PackageStream.Metadata preparePackageMd(File packageFile, String packageSpec,
                                                           String packageMimeType) {
        PackageStream.Metadata md = mock(PackageStream.Metadata.class);
        when(md.name()).thenReturn(packageFile.getName());
        when(md.spec()).thenReturn(packageSpec);
        when(md.archive()).thenReturn(PackageStream.ARCHIVE.ZIP);
        when(md.archived()).thenReturn(true);
        when(md.compression()).thenReturn(PackageStream.COMPRESSION.ZIP);
        when(md.compressed()).thenReturn(true);
        when(md.sizeBytes()).thenReturn(packageFile.length());
        when(md.mimeType()).thenReturn(packageMimeType);
        final PackageStream.Checksum md5 = new PackageStream.Checksum() {
            @Override
            public PackageStream.Algo algorithm() {
                return PackageStream.Algo.MD5;
            }

            @Override
            public byte[] value() {
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    return DigestUtils.digest(digest, new FileInputStream(packageFile));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

            @Override
            public String asBase64() {
                return encodeBase64String(value());
            }

            @Override
            public String asHex() {
                String hex = null;
                try {
                    hex = DigestUtils.md5Hex(new FileInputStream(packageFile));
                } catch (IOException e) {
                    throw new RuntimeException("Error calculating the MD5 checksum for '" +
                            packageFile.getPath() + "'");
                }
                return hex;
            }
        };
        when(md.checksum()).thenReturn(md5);
        when(md.checksums()).thenReturn(Collections.singletonList(md5));

        return md;
    }

    /**
     * Retrieve the service document from {@code serviceDocEndpoint} using the supplied {@code swordClient} and
     * {@code authCreds}.
     *
     * @param swordClient
     * @param serviceDocEndpoint
     * @param authCreds
     * @return
     */
    private static ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocEndpoint,
                                                      AuthCredentials authCreds) {
        ServiceDocument serviceDoc = null;
        try {
            serviceDoc = swordClient.getServiceDocument(serviceDocEndpoint, authCreds);
            assertNotNull("SWORD Service Document obtained from '" + serviceDocEndpoint + "' (auth creds: " +
                    "'" + authCreds.getUsername() + "':'" + authCreds.getPassword() + "') was null.", serviceDoc);
        } catch (Exception e) {
            String msg = String.format("Failed to connect to %s: %s", serviceDocEndpoint, e.getMessage());
            LOG.error(msg, e);
            fail(msg);
        }
        return serviceDoc;
    }
}
