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
package org.dataconservancy.pass.deposit.builder.fs;

import org.dataconservancy.pass.deposit.builder.InvalidModel;
import org.dataconservancy.pass.deposit.model.DepositSubmission;

import java.net.URI;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static submissions.SubmissionResourceUtil.lookupUri;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SharedSubmissionUtil {

    private FilesystemModelBuilder fsModelBuilder = new FilesystemModelBuilder();

    public SharedSubmissionUtil() {

    }

    public SharedSubmissionUtil(FilesystemModelBuilder fsModelBuilder) {
        this.fsModelBuilder = fsModelBuilder;
    }

    public DepositSubmission asDepositSubmission(URI submissionUri) throws InvalidModel {
        URI submissionJsonUri = lookupUri(submissionUri);

        if (submissionJsonUri == null) {
            throw new RuntimeException("Unable to look up test resource URI for submission '" + submissionUri + "'");
        }

        return fsModelBuilder.build(submissionJsonUri.toString());
    }

}
