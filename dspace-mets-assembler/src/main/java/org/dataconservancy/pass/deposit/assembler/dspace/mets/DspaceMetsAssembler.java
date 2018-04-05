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

import org.dataconservancy.nihms.assembler.Assembler;
import org.dataconservancy.nihms.assembler.PackageStream;
import org.dataconservancy.nihms.model.NihmsFile;
import org.dataconservancy.nihms.model.NihmsSubmission;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

public class DspaceMetsAssembler implements Assembler {

    private static final String ERR_MAPPING_LOCATION = "Unable to resolve the location of a submitted file ('%s') to a Spring Resource type.";

    private static final String FILE_PREFIX = "file:";

    private static final String CLASSPATH_PREFIX = "classpath:";

    private static final String WILDCARD_CLASSPATH_PREFIX = "classpath*:";

    private static final String HTTP_PREFIX = "http:";

    private static final String HTTPS_PREFIX = "https:";

    @Override
    public PackageStream assemble(NihmsSubmission submission) {
        List<Resource> fileResources = submission.getFiles()
                .stream()
                .map(NihmsFile::getLocation)
                .map(location -> {
                    if (location.startsWith(FILE_PREFIX)) {
                        return new FileSystemResource(location);
                    }
                    if (location.startsWith(CLASSPATH_PREFIX) ||
                            location.startsWith(WILDCARD_CLASSPATH_PREFIX)) {
                        if (location.startsWith(WILDCARD_CLASSPATH_PREFIX)) {
                            return new ClassPathResource(location.substring(WILDCARD_CLASSPATH_PREFIX.length()));
                        }
                        return new ClassPathResource(location.substring(CLASSPATH_PREFIX.length()));
                    }
                    if (location.startsWith(HTTP_PREFIX) || location.startsWith(HTTPS_PREFIX)) {
                        try {
                            return new UrlResource(location);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                    if (location.contains("/") || location.contains("\\")) {
                        // assume it is a file
                        return new FileSystemResource(location);
                    }

                    throw new RuntimeException(String.format(ERR_MAPPING_LOCATION, location));
                })
                .collect(Collectors.toList());
    }

}
