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

import org.dataconservancy.nihms.builder.fs.PassJsonFedoraAdapter;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.PassEntity;
import org.dataconservancy.pass.model.Repository;
import org.dataconservancy.pass.model.Submission;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.dataconservancy.pass.deposit.messaging.service.SubmissionTestUtil.getDepositUris;
import static org.dataconservancy.pass.model.Submission.AggregatedDepositStatus.NOT_STARTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.jms.listener.auto-startup=false" })
public abstract class AbstractSubmissionIT {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected static final String EXPECTED_REPO_NAME = "JScholarship";

    protected Submission submission;

    protected Map<URI, PassEntity> submissionResources;

    @Autowired
    @Qualifier("submissionProcessor")
    protected SubmissionProcessor underTest;

    @Autowired
    protected PassClient passClient;

    /**
     * Populates Fedora with a Submission, as if it was submitted interactively by a user of the PASS UI.
     *
     * @throws Exception
     */
    @Before
    public void createSubmission() throws Exception {
        PassJsonFedoraAdapter passAdapter = new PassJsonFedoraAdapter();

        // Upload sample data to Fedora repository to get its Submission URI.
        InputStream is = getSubmissionResources();

        HashMap<URI, PassEntity> uriMap = new HashMap<>();
        URI submissionUri = passAdapter.jsonToFcrepo(is, uriMap);
        is.close();

        // Find the Submission entity that was uploaded
        for (URI key : uriMap.keySet()) {
            PassEntity entity = uriMap.get(key);
            if (entity.getId() == submissionUri) {
                submission = (Submission)entity;
                break;
            }
        }

        assertNotNull("Missing expected Submission; it was not added to the repository.", submission);

        // verify state of the initial Submission
        assertEquals(Submission.Source.PASS, submission.getSource());
        assertEquals(Boolean.TRUE, submission.getSubmitted());
        assertEquals(NOT_STARTED, submission.getAggregatedDepositStatus());

        // no Deposits pointing to the Submission
        assertTrue("Unexpected incoming links to " + submissionUri,
                getDepositUris(submission, passClient).isEmpty());

        // JScholarship repository ought to exist
        assertNotNull(submission.getRepositories());
        assertTrue(submission.getRepositories().stream()
                .map(uri -> (Repository)uriMap.get(uri))
                .anyMatch(repo -> repo.getName().equals(EXPECTED_REPO_NAME)));


        submissionResources = uriMap;

    }

    protected abstract InputStream getSubmissionResources();

}
