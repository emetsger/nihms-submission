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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ContentLengthObserver;
import org.apache.commons.io.input.DigestObserver;
import org.apache.commons.io.input.ObservableInputStream;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.dataconservancy.nihms.assembler.nihmsnative.NihmsPackageStream.ERR_PUT_RESOURCE;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public abstract class AbstractThreadedOutputStreamWriter extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractThreadedOutputStreamWriter.class);

    private List<Resource> packageFiles;

    private AbstractThreadedOutputStreamWriter.CloseOutputstreamCallback closeStreamHandler;

    protected static final int THIRTY_TWO_KIB = 32 * 2 ^ 10;

    protected ArchiveOutputStream archiveOut;

    public AbstractThreadedOutputStreamWriter(String threadName, ArchiveOutputStream archiveOut, List<Resource> packageFiles) {
        super(threadName);
        this.archiveOut = archiveOut;
        this.packageFiles = packageFiles;
    }

    @Override
    public void run() {
        List<PackageStream.Resource> assembledResources = new ArrayList<>();

        try {
            // prepare a tar entry for each file in the archive

            // (1) need to know the name of each file going into the tar
            // (2) the size of each file going into the tar?
            ResourceBuilderImpl rb = new ResourceBuilderImpl();
            packageFiles.forEach(resource -> {
                try (InputStream resourceIn = resource.getInputStream();
                     ObservableInputStream observableIn = new ObservableInputStream(resourceIn)) {

                    DefaultDetector detector = new DefaultDetector();
                    MediaType mimeType = detector.detect(resourceIn, new Metadata());
                    rb.mimeType(mimeType.toString());

                    ContentLengthObserver clObs = new ContentLengthObserver(rb);
                    DigestObserver md5Obs = new DigestObserver(rb, PackageStream.Algo.MD5);
                    DigestObserver sha256Obs = new DigestObserver(rb, PackageStream.Algo.SHA_256);
                    observableIn.add(clObs);
                    observableIn.add(md5Obs);
                    observableIn.add(sha256Obs);

                    final TarArchiveEntry archiveEntry = new TarArchiveEntry(resource.getFilename());
                    archiveEntry.setSize(resource.contentLength());
                    putResource(archiveOut, archiveEntry, observableIn);
                } catch (IOException e) {
                    throw new RuntimeException(format(ERR_PUT_RESOURCE, resource.getFilename(), e.getMessage()), e);
                }
                rb.name(resource.getFilename());
                assembledResources.add(rb.build());
            });

            // TODO: manifests, etc are built and serialized to the archiveOut stream
            // (must create TarArchiveEntry for each manifest)
            // build METS manifest from assembledResources
            // build NIHMS manifest from assembledResources and NihmsManifest (needed b/c it has the classifier info: manuscript, figure, table, etc.
            // build NIHMS bulk metadata from NihmsMetadata, Manuscript metadata, Journal metadata, Person metadata, Article metadata

            assembleResources(assembledResources);

            // TODO: archiveOut is closed and finished by the parent thread?
            archiveOut.finish();
            archiveOut.close();
        } catch (Exception e) {
            LOG.info("Exception encountered streaming package, cleaning up by closing the archive output stream: {}",
                    e.getMessage(), e);

            // special care needs to be taken when exceptions are encountered.  it is essential that the underlying
            // pipedoutputstream be closed. (1) the archive output stream prevents the underlying output streams from
            // being closed (2) this class isn't aware of the underlying output streams; there may be multiple of them
            // so the creator of this instance also supplies a callback which is invoked to close each of the underlying
            // streams, insuring that the pipedoutputstream is closed.
            closeStreamHandler.closeAll();
        }
    }

    public abstract void assembleResources(List<PackageStream.Resource> resources) throws IOException;

    /**
     * Called when an exception occurs writing to the piped output stream, or after all resources have been successfully
     * streamed to the piped output stream.
     *
     * @return the handler invoked to close output streams when an exception is encountered writing to the piped output
     * stream
     */
    public AbstractThreadedOutputStreamWriter.CloseOutputstreamCallback getCloseStreamHandler() {
        return closeStreamHandler;
    }

    /**
     * Called when an exception occurs writing to the piped output stream, or after all resources have been successfully
     * streamed to the piped output stream.
     *
     * @param callback the handler invoked to close output streams when an exception is encountered writing to the piped
     * output stream
     */
    public void setCloseStreamHandler(AbstractThreadedOutputStreamWriter.CloseOutputstreamCallback callback) {
        this.closeStreamHandler = callback;
    }

    protected void putResource(ArchiveOutputStream archiveOut, ArchiveEntry archiveEntry, InputStream inputStream)
            throws IOException {
        archiveOut.putArchiveEntry(archiveEntry);
        IOUtils.copy(inputStream, archiveOut);
        archiveOut.closeArchiveEntry();
    }

    protected InputStream updateLength(TarArchiveEntry entry, InputStream toSize) throws IOException {
        org.apache.commons.io.output.ByteArrayOutputStream baos =
                new org.apache.commons.io.output.ByteArrayOutputStream(THIRTY_TWO_KIB);
        IOUtils.copy(toSize, baos);
        entry.setSize(baos.size());
        LOG.debug("Updating tar entry {} size to {}", entry.getName(), baos.size());
        return new ByteArrayInputStream(baos.toByteArray());
    }

    interface CloseOutputstreamCallback {
        void closeAll();
    }
}
