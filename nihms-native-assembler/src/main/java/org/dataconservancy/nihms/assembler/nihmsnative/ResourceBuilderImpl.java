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

import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.assembler.ResourceBuilder;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class ResourceBuilderImpl implements ResourceBuilder {

    private ResourceImpl resource;

    @Override
    public ResourceBuilder checksum(PackageStream.Checksum checksum) {
        checkState();
        resource.addChecksum(checksum);
        return this;
    }

    @Override
    public ResourceBuilder mimeType(String mimeType) {
        checkState();
        resource.setMimeType(mimeType);
        return this;
    }

    @Override
    public ResourceBuilder name(String name) {
        checkState();
        resource.setName(name);
        return this;
    }

    @Override
    public ResourceBuilder sizeBytes(long sizeBytes) {
        checkState();
        resource.setSizeBytes(sizeBytes);
        return this;
    }

    @Override
    public PackageStream.Resource build() {
        checkState();
        ResourceImpl toReturn = this.resource;
        this.resource = null;

        return toReturn;
    }

    private void checkState() {
        if (resource == null) {
            this.resource = new ResourceImpl();
        }
    }

    public void reset() {
        this.resource = null;
    }
}
