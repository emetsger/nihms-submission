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
import org.apache.commons.io.IOUtils;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.integration.BaseIT;
import org.junit.Test;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.ClientConfiguration;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.ServiceDocument;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Sword2TransportSessionIT extends BaseIT {

    private static final String DSPACE_ADMIN_USER = "dspace-admin@oapass.org";

    private static final String DSPACE_ADMIN_PASSWORD = "foobar";

    private static final String PACKAGE_CONTENT = "This is a package!";

    private static final String SERVICEDOC_ENDPOINT = "http://192.168.99.100/swordv2/servicedocument";

    @Test
    public void testSimple() throws Exception {
        ClientConfiguration swordConfig = new ClientConfiguration();
        swordConfig.setReturnDepositReceipt(true);
        swordConfig.setUserAgent("oapass/SWORDv2");

        AuthCredentials authCreds = new AuthCredentials(DSPACE_ADMIN_USER, DSPACE_ADMIN_PASSWORD);

        SWORDClient swordClient = new SWORDClient(swordConfig);

        ServiceDocument serviceDoc = swordClient.getServiceDocument(SERVICEDOC_ENDPOINT, authCreds);
        assertNotNull(serviceDoc);

        PackageStream.Metadata md = mock(PackageStream.Metadata.class);
        PackageStream packageStream = mock(PackageStream.class);

        when(packageStream.open()).thenReturn(IOUtils.toInputStream(PACKAGE_CONTENT, "UFT-8"));
        when(packageStream.metadata()).thenReturn(md);
        when(md.name()).thenReturn("MyPackage.txt");
        when(md.spec()).thenReturn("http://purl.org/net/sword/package/SimpleZip");
        when(md.archive()).thenReturn(PackageStream.ARCHIVE.NONE);
        when(md.archived()).thenReturn(false);
        when(md.compression()).thenReturn(PackageStream.COMPRESSION.NONE);
        when(md.compressed()).thenReturn(false);
        when(md.sizeBytes()).thenReturn((long) PACKAGE_CONTENT.getBytes().length);
        when(md.mimeType()).thenReturn("text/plain");
        when(md.checksum()).thenAnswer(inv -> new PackageStream.Checksum() {
            @Override
            public PackageStream.Algo algorithm() {
                return PackageStream.Algo.MD5;
            }

            @Override
            public byte[] value() {
                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    return DigestUtils.digest(digest, IOUtils.toInputStream(PACKAGE_CONTENT, "UFT-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

            @Override
            public String asBase64() {
                return encodeBase64String(value());
            }
        });

        Sword2TransportSession underTest = new Sword2TransportSession(swordClient, serviceDoc, authCreds);

//        underTest.send(packageStream, )

    }
}
