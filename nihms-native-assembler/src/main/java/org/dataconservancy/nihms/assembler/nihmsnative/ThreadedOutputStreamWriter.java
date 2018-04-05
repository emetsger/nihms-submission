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

package org.dataconservancy.nihms.assembler.nihmsnative;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ContentLengthObserver;
import org.apache.commons.io.input.DigestObserver;
import org.apache.commons.io.input.MessageDigestCalculatingInputStream;
import org.apache.commons.io.input.ObservableInputStream;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.assembler.ResourceStreamCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static org.dataconservancy.nihms.assembler.nihmsnative.NihmsPackageStream.ERR_PUT_RESOURCE;
import static org.dataconservancy.nihms.assembler.nihmsnative.NihmsPackageStream.MANIFEST_ENTRY_NAME;
import static org.dataconservancy.nihms.assembler.nihmsnative.NihmsPackageStream.METADATA_ENTRY_NAME;

class ThreadedOutputStreamWriter extends AbstractThreadedOutputStreamWriter {

    private static final int THIRTY_TWO_KIB = 32 * 2 ^ 10;

    private static final Logger LOG = LoggerFactory.getLogger(ThreadedOutputStreamWriter.class);

    private StreamingSerializer manifestSerializer;

    private StreamingSerializer metadataSerializer;

    public ThreadedOutputStreamWriter(String threadName, ArchiveOutputStream archiveOut, List<Resource> packageFiles,
                                      StreamingSerializer manifestSerializer, StreamingSerializer metadataSerializer) {
        super(threadName, archiveOut, packageFiles);
        this.manifestSerializer = manifestSerializer;
        this.metadataSerializer = metadataSerializer;
    }

    @Override
    public void assembleResources(List<PackageStream.Resource> resources) throws IOException {
        TarArchiveEntry manifestEntry = new TarArchiveEntry(MANIFEST_ENTRY_NAME);
        TarArchiveEntry metadataEntry = new TarArchiveEntry(METADATA_ENTRY_NAME);
        putResource(archiveOut, manifestEntry, updateLength(manifestEntry, manifestSerializer.serialize()));
        putResource(archiveOut, metadataEntry, updateLength(metadataEntry, metadataSerializer.serialize()));
    }

    private InputStream updateLength(TarArchiveEntry entry, InputStream toSize) throws IOException {
        org.apache.commons.io.output.ByteArrayOutputStream baos =
                new org.apache.commons.io.output.ByteArrayOutputStream(THIRTY_TWO_KIB);
        IOUtils.copy(toSize, baos);
        entry.setSize(baos.size());
        LOG.debug("Updating tar entry {} size to {}", entry.getName(), baos.size());
        return new ByteArrayInputStream(baos.toByteArray());
    }

}
