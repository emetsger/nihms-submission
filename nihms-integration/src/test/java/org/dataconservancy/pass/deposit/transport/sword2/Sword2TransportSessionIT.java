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
import org.junit.Test;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.ClientConfiguration;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.ServiceDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sword2TransportSessionIT extends BaseIT {

    private static final String DSPACE_ADMIN_USER = "dspace-admin@oapass.org";

    private static final String DSPACE_ADMIN_PASSWORD = "foobar";

    private static final String PACKAGE_RESOURCE = "simplezippackage.zip";

    private static final String SERVICEDOC_ENDPOINT = "http://192.168.99.100:8181/swordv2/servicedocument";

    @Test
    public void testSimple() throws Exception {
        File samplePackage = new File(this.getClass().getResource(PACKAGE_RESOURCE).getPath());
        assertNotNull(samplePackage);
        assertTrue("Missing sample package; cannot resolve '" + PACKAGE_RESOURCE + "' as a class path resource.",
                samplePackage.exists());

        ClientConfiguration swordConfig = new ClientConfiguration();
        swordConfig.setReturnDepositReceipt(true);
        swordConfig.setUserAgent("oapass/SWORDv2");

        AuthCredentials authCreds = new AuthCredentials(DSPACE_ADMIN_USER, DSPACE_ADMIN_PASSWORD);

        SWORDClient swordClient = new SWORDClient(swordConfig);

        ServiceDocument serviceDoc = null;
        try {
            serviceDoc = swordClient.getServiceDocument(SERVICEDOC_ENDPOINT, authCreds);
            assertNotNull(serviceDoc);
        } catch (Exception e) {
            String msg = String.format("Failed to connect to %s: %s", SERVICEDOC_ENDPOINT, e.getMessage());
            LOG.error(msg, e);
            fail(msg);
        }

        PackageStream.Metadata md = mock(PackageStream.Metadata.class);
        PackageStream packageStream = mock(PackageStream.class);

        when(packageStream.open()).thenReturn(new FileInputStream(samplePackage));
        when(packageStream.metadata()).thenReturn(md);
        when(md.name()).thenReturn(samplePackage.getName());
        when(md.spec()).thenReturn("http://purl.org/net/sword/package/SimpleZip");
        when(md.archive()).thenReturn(PackageStream.ARCHIVE.ZIP);
        when(md.archived()).thenReturn(true);
        when(md.compression()).thenReturn(PackageStream.COMPRESSION.ZIP);
        when(md.compressed()).thenReturn(true);
        when(md.sizeBytes()).thenReturn(samplePackage.length());
        when(md.mimeType()).thenReturn("application/zip");
        final PackageStream.Checksum md5 = new PackageStream.Checksum() {
            @Override
            public PackageStream.Algo algorithm() {
                return PackageStream.Algo.MD5;
            }

            @Override
            public byte[] value() {
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    return DigestUtils.digest(digest, new FileInputStream(samplePackage));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

            @Override
            public String asBase64() {
                String base64 = encodeBase64String(value());
                LOG.debug(">>>> base64 encoded md5 digest: '{}'", base64);
                return base64;
            }

            @Override
            public String asHex() {
                String hex = null;
                try {
                    hex = DigestUtils.md5Hex(new FileInputStream(samplePackage));
                } catch (IOException e) {
                    throw new RuntimeException("Error calculating the MD5 checksum for '" + samplePackage.getPath() +
                            "'");
                }
                LOG.debug(">>>> hex encoded md5 digest: '{}'", hex);
                return hex;
            }
        };
        when(md.checksum()).thenReturn(md5);
        when(md.checksums()).thenReturn(Collections.singletonList(md5));

        Sword2TransportSession underTest = new Sword2TransportSession(swordClient, serviceDoc, authCreds);

        Map<String, String> transportMd = new HashMap<>();
        transportMd.put(Sword2TransportHints.SWORD_COLLECTION_URL,
                "http://192.168.99.100:8181/swordv2/collection/123456789/2");

        Sword2DepositReceiptResponse response = underTest.send(packageStream, transportMd);
        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.getReceipt());
    }
}
