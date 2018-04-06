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

package org.dataconservancy.nihms.assembler.nihmsnative;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.dataconservancy.nihms.assembler.MetadataBuilder;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.compress.archivers.ArchiveStreamFactory.TAR;

public abstract class AbstractPackageStream implements PackageStream {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPackageStream.class);

    private static final int ONE_MIB = 2 ^ 20;

    protected static final String ERR_CREATING_ARCHIVE_STREAM = "Error creating a %s archive output stream: %s";

    protected List<org.springframework.core.io.Resource> custodialContent;

    private MetadataBuilder metadataBuilder;

    private ResourceBuilderFactory rbf;

    public AbstractPackageStream(List<org.springframework.core.io.Resource> custodialContent,
                                 MetadataBuilder metadataBuilder, ResourceBuilderFactory rbf) {
        this.custodialContent = custodialContent;
        this.metadataBuilder = metadataBuilder;
        this.rbf = rbf;
    }

    @Override
    public InputStream open() {

        PipedInputStream pipedIn = new PipedInputStream(ONE_MIB);
        PipedOutputStream pipedOut;
        try {
            pipedOut = new PipedOutputStream(pipedIn);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        TarArchiveOutputStream archiveOut;
        GZIPOutputStream gzipOut;

        try {
            gzipOut = new GZIPOutputStream(pipedOut, true);
            archiveOut = new TarArchiveOutputStream(gzipOut);
        } catch (Exception e) {
            throw new RuntimeException(String.format(ERR_CREATING_ARCHIVE_STREAM, TAR, e.getMessage()), e);
        }


        // put below in a thread, and start
        // then return pipedIn

        AbstractThreadedOutputStreamWriter streamWriter = getStreamWriter(archiveOut, rbf);
        streamWriter.setCloseStreamHandler(() -> {
                    try {
                        pipedOut.close();
                    } catch (IOException e) {
                        LOG.info("Error closing piped output stream: {}", e.getMessage(), e);
                    }

                    try {
                        gzipOut.close();
                    } catch (IOException e) {
                        LOG.info("Error closing the gzip output stream: {}", e.getMessage(), e);
                    }

                    try {
                        archiveOut.close();
                    } catch (IOException e) {
                        try {
                            archiveOut.closeArchiveEntry();
                        } catch (IOException e1) {
                            LOG.info("Error closing archive entry: {}", e.getMessage(), e);
                        }

                        try {
                            archiveOut.close();
                        } catch (IOException e1) {
                            // too bad
                            LOG.info("Error closing the archive output stream: {}", e.getMessage(), e);
                        }
                    }
                }
        );

        streamWriter.start();

        return pipedIn;

    }

    public abstract AbstractThreadedOutputStreamWriter getStreamWriter(TarArchiveOutputStream tarArchiveOutputStream, ResourceBuilderFactory rbf);

    @Override
    public PackageStream.Metadata metadata() {
        return metadataBuilder.build();
    }

    @Override
    public InputStream open(String packageResource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<PackageStream.Resource> resources() {
        throw new UnsupportedOperationException();
    }

}
