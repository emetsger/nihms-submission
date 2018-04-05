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

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.dataconservancy.nihms.assembler.MetadataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NihmsPackageStream extends AbstractPackageStream {

    static final String MANIFEST_ENTRY_NAME = "manifest.txt";

    static final String METADATA_ENTRY_NAME = "bulk_meta.xml";

    static final String ERR_CREATING_ARCHIVE_STREAM = "Error creating a %s archive output stream: %s";

    static final String ERR_PUT_RESOURCE = "Error putting resource '%s' into archive output stream: %s";

    private static final Logger LOG = LoggerFactory.getLogger(NihmsPackageStream.class);

    private StreamingSerializer manifestSerializer;

    private StreamingSerializer metadataSerializer;

    public NihmsPackageStream(StreamingSerializer manifestSerializer, StreamingSerializer metadataSerializer,
                              List<org.springframework.core.io.Resource> custodialResources, MetadataBuilder metadata, ResourceBuilderFactory rbf) {
        super(custodialResources, metadata, rbf);
        this.manifestSerializer = manifestSerializer;
        this.metadataSerializer = metadataSerializer;
    }

    @Override
    public AbstractThreadedOutputStreamWriter getStreamWriter(TarArchiveOutputStream archiveOut, ResourceBuilderFactory rbf) {
        ThreadedOutputStreamWriter threadedWriter = new ThreadedOutputStreamWriter("Archive Piped Writer", archiveOut, custodialContent, manifestSerializer, metadataSerializer);

        return threadedWriter;
    }
}
