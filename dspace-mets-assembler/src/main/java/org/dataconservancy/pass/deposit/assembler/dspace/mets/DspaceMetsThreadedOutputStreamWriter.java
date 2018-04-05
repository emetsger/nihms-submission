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
package org.dataconservancy.pass.deposit.assembler.dspace.mets;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.assembler.nihmsnative.AbstractThreadedOutputStreamWriter;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DspaceMetsThreadedOutputStreamWriter extends AbstractThreadedOutputStreamWriter {

    private static final String METS_XML = "mets.xml";

    private MetsDomWriter metsWriter;

    public DspaceMetsThreadedOutputStreamWriter(String threadName, ArchiveOutputStream archiveOut,
                                                List<Resource> packageFiles, MetsDomWriter metsWriter) {
        super(threadName, archiveOut, packageFiles);
        if (metsWriter == null) {
            throw new IllegalArgumentException("MetsDomWriter must not be null.");
        }
        this.metsWriter = metsWriter;
    }

    @Override
    public void assembleResources(List<PackageStream.Resource> resources) throws IOException {
        resources.forEach(r -> System.err.println(">>>> Got resource: " + r));

        // this is where we compose and write the METS xml to the ArchiveOutputStream

        resources.forEach(r -> metsWriter.addResource(r));

        ByteArrayOutputStream metsOut = new ByteArrayOutputStream();
        metsWriter.write(metsOut);
        ByteArrayInputStream metsIn = new ByteArrayInputStream(metsOut.toByteArray());

        TarArchiveEntry metsEntry = new TarArchiveEntry(METS_XML);
        metsEntry.setSize(metsOut.size());

        putResource(archiveOut, metsEntry, metsIn);
    }

}
