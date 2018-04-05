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

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.dataconservancy.nihms.assembler.nihmsnative.AbstractPackageStream;
import org.dataconservancy.nihms.assembler.nihmsnative.AbstractThreadedOutputStreamWriter;

import java.util.List;

public class DspaceMetsPackageStream extends AbstractPackageStream {

    private MetsDomWriter metsWriter;

    public DspaceMetsPackageStream(List<org.springframework.core.io.Resource> packageFiles, Metadata metadata,
                                   MetsDomWriter metsWriter) {
        super(packageFiles, metadata);
        if (metsWriter == null) {
            throw new IllegalArgumentException("METS writer must not be null.");
        }
        this.metsWriter = metsWriter;
    }

    @Override
    public AbstractThreadedOutputStreamWriter getStreamWriter(TarArchiveOutputStream tarArchiveOutputStream) {
        return new DspaceMetsThreadedOutputStreamWriter("DSpace Archive Writer", tarArchiveOutputStream,
                packageFiles, metsWriter);
    }
}
