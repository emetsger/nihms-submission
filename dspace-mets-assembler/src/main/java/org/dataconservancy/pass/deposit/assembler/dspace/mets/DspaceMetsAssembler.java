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

import org.dataconservancy.nihms.assembler.MetadataBuilder;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.assembler.nihmsnative.AbstractAssembler;
import org.dataconservancy.nihms.assembler.nihmsnative.MetadataBuilderFactory;
import org.dataconservancy.nihms.assembler.nihmsnative.ResourceBuilderFactory;
import org.dataconservancy.nihms.model.NihmsSubmission;
import org.springframework.core.io.Resource;

import java.util.List;

public class DspaceMetsAssembler extends AbstractAssembler {

    private MetsDomWriter metsWriter;

    public DspaceMetsAssembler(MetadataBuilderFactory mbf, ResourceBuilderFactory rbf, MetsDomWriter metsWriter) {
        super(mbf, rbf);
        this.metsWriter = metsWriter;
    }

    @Override
    protected PackageStream createPackageStream(NihmsSubmission submission, List<Resource> custodialResources, MetadataBuilder mb, ResourceBuilderFactory rbf) {
        return new DspaceMetsPackageStream(custodialResources, mb, rbf, metsWriter);
    }

}
