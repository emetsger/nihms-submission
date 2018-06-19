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
package org.dataconservancy.pass.deposit.messaging.service;

import org.dataconservancy.pass.deposit.messaging.DepositServiceRuntimeException;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.isA;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class EmptySubmissionIT extends AbstractSubmissionIT {

    private static final String SUBMISSION_RESOURCES = "SubmissionProcessorIT-no-files.json";

    @Override
    protected InputStream getSubmissionResources() {
        return SubmissionTestUtil.getSubmissionResources(SUBMISSION_RESOURCES);
    }

    @Test
    public void submissionWithNoFiles() throws Exception {

        thrown.expect(DepositServiceRuntimeException.class);
        thrown.expectCause(isA(IllegalStateException.class));
        thrown.expectMessage("no files attached");

        // This submission should fail off the bat because there's no files in the submission.
        underTest.accept(submission);
    }

}
