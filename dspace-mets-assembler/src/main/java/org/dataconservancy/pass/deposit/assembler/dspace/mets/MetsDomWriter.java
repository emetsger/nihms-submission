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

import au.edu.apsr.mtk.base.Constants;
import au.edu.apsr.mtk.base.FLocat;
import au.edu.apsr.mtk.base.File;
import au.edu.apsr.mtk.base.FileGrp;
import au.edu.apsr.mtk.base.FileSec;
import au.edu.apsr.mtk.base.METS;
import au.edu.apsr.mtk.base.METSException;
import au.edu.apsr.mtk.base.METSWrapper;
import au.edu.apsr.mtk.base.Stream;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class MetsDomWriter {

    static final String METS_ID = "DSPACE-METS-SWORD";

    static final String METS_OBJ_ID = "DSPACE-METS-SWORD-OBJ";

    static final String METS_DSPACE_LABEL = "DSpace SWORD Item";

    static final String METS_DSPACE_PROFILE = "DSpace METS SIP Profile 1.0";

    static final String CONTENT_USE = "CONTENT";

    static final String LOCTYPE_URL = "URL";

    private DocumentBuilderFactory dbf;

    private Document metsDocument;

    private METS mets;

    MetsDomWriter(DocumentBuilderFactory dbf) {
        try {
            this.dbf = dbf;
            this.metsDocument = dbf.newDocumentBuilder().newDocument();
            Element root = metsDocument.createElementNS(Constants.NS_METS, Constants.ELEMENT_METS);
            metsDocument.appendChild(root);
            this.mets = new METS(metsDocument);
            this.mets.setID(UUID.randomUUID().toString());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    void write(OutputStream out) {
        METSWrapper wrapper = null;
        try {
            wrapper = new METSWrapper(metsDocument);
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        wrapper.write(out);
    }

    void addResource(PackageStream.Resource resource) {
        File resourceFile = createFile(CONTENT_USE);

        if (resource.checksum() != null) {
            resourceFile.setChecksum(resource.checksum().asHex());
            resourceFile.setChecksumType(resource.checksum().algorithm().name());
        }

        if (resource.sizeBytes() > -1) {
            resourceFile.setSize(resource.sizeBytes());
        }

        if (resource.mimeType() != null && resource.mimeType().trim().length() > 0) {
            resourceFile.setMIMEType(resource.mimeType());
        }

        FLocat locat = null;
        try {
            locat = resourceFile.newFLocat();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        locat.setID(UUID.randomUUID().toString());
        locat.setHref(resource.name());
        locat.setLocType(LOCTYPE_URL);
    }

    private File createFile(String use) {
        File file = null;
        try {
            file = getFileGrpByUse(use).newFile();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        file.setID(UUID.randomUUID().toString());
        return file;
    }

    /**
     * Obtains the {@code <fileGrp>} element with a {@code USE} equal to the supplied {@code use} value.  If the element
     * does not exist, it is created and assigned an identifier.
     *
     * @param use the content use for the {@code FileGrp}
     * @return the {@code FileGrp} with a {@code USE} equal to {@code use}
     */
    private FileGrp getFileGrpByUse(String use) {
        List<FileGrp> fileGroups = null;
        try {
            fileGroups = getFileSec().getFileGrpByUse(use);
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (fileGroups == null || fileGroups.isEmpty()) {
            return createFileGrp(use);
        }

        return fileGroups.get(0);
    }

    /**
     * Creates the {@code <fileGrp>} element with a {@code USE} equal to the supplied {@code use} value, and assigns
     * it an identifier.
     *
     * @param use the content use for the {@code FileGrp}
     * @return the {@code FileGrp} with a {@code USE} equal to {@code use}
     */
    private FileGrp createFileGrp(String use) {
        FileGrp fileGrp = null;
        try {
            fileGrp = getFileSec().newFileGrp();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        fileGrp.setID(UUID.randomUUID().toString());
        fileGrp.setUse(use);
        return fileGrp;
    }

    /**
     * Obtains the only {@code <fileSec>} element from the METS document.  If the element does not exist, it is created
     * and assigned an identifier.
     *
     * @return the {@code FileSec} for the current METS document
     */
    private FileSec getFileSec() {
        FileSec fileSec = null;
        try {
            fileSec = mets.getFileSec();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (fileSec == null) {
            return createFileSec();
        }

        return fileSec;
    }

    /**
     * Creates a new {@code <fileSec>} element from the METS document and assigns it an identifier.
     *
     * @return the newly created {@code FileSec} for the current METS document
     */
    private FileSec createFileSec() {
        FileSec fs = null;
        try {
            fs = mets.newFileSec();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        fs.setID(UUID.randomUUID().toString());
        return fs;
    }
}
