/*
 * Copyright 2017 Johns Hopkins University
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

package org.dataconservancy.pass.deposit.builder.fedora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.dataconservancy.nihms.builder.fs.FcrepoModelBuilder;
import org.dataconservancy.nihms.builder.fs.PassJsonFedoraAdapter;
import org.dataconservancy.nihms.integration.BaseIT;
import org.dataconservancy.nihms.model.DepositSubmission;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientDefault;
import org.dataconservancy.pass.model.File;
import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PassEntity;
import org.dataconservancy.pass.model.Publication;
import org.dataconservancy.pass.model.Submission;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;

public class FcrepoModelBuilderIT extends BaseIT {

    private DepositSubmission submission;
    private FcrepoModelBuilder underTest = new FcrepoModelBuilder();
    private String SAMPLE_SUBMISSION_RESOURCE = "SampleSubmissionData.json";

    @Before
    public void setup() throws Exception {
        PassClient passClient = new PassClientDefault();

        // Upload sample data to Fedora repository to get its URI.
        PassJsonFedoraAdapter reader = new PassJsonFedoraAdapter();
        URL sampleDataUrl = this.getClass().getResource(SAMPLE_SUBMISSION_RESOURCE);
        InputStream is = new FileInputStream(sampleDataUrl.getPath());
        URI submissionUri = reader.jsonToFcrepo(is);
        is.close();


        // wait for files to appear in the index
        ObjectMapper mapper = new ObjectMapper();
        long fileCount = mapper.readTree(new FileInputStream(sampleDataUrl.getPath()))
                .findValuesAsText("@type").stream().filter("File"::equals).count();
        assertTrue(fileCount > 0);
        OkHttpClient http = new OkHttpClient();
        Request esFileQuery = new Request.Builder().url(System.getProperty(PASS_ES_URL) + "_search?q=@type:File").build();

        attemptAndVerify(60, () -> {
            LOG.debug("Looking for {} files...", fileCount);
            try (Response res = http.newCall(esFileQuery).execute()) {
                LOG.debug("Executing request {}", esFileQuery.url().toString());
                return mapper.readTree(res.body().bytes());
            }
        }, (jsonNode) -> {
            LOG.debug("Evaluating JSON {}", (jsonNode != null) ? jsonNode.asText() : null);

            if (jsonNode == null) {
                return null;
            }

            JsonNode hits = jsonNode.findValue("hits");
            if (hits == null) {
                LOG.debug("No hits");
                return null;
            }

            if (hits.get("total").asInt() == fileCount) {
                LOG.debug("{} hits, returning true", fileCount);
                return true;
            } else {
                LOG.debug("{} hits, expected {}", hits.get("total").asInt(), fileCount);
                return null;
            }
        } );

        submission = underTest.build(submissionUri.toString()); // this invokes the search
    }

    @Test
    public void testElementValues() {
        // Load the PassEntity version of the sample data file
        Submission submissionEntity = null;
        HashMap<URI, PassEntity> entities = new HashMap<>();
        try {
            URL sampleDataUrl = this.getClass().getResource(SAMPLE_SUBMISSION_RESOURCE);
            InputStream is = new FileInputStream(sampleDataUrl.getPath());
            PassJsonFedoraAdapter reader = new PassJsonFedoraAdapter();
            submissionEntity = reader.jsonToPass(is, entities);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("Could not load sample data file");
        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not close the sample data file");
        }

        // Until the data model is finalized, just check that some basic things are in order
        assertNotNull(submission.getManifest());
        assertNotNull(submission.getMetadata());
        assertNotNull(submission.getMetadata().getManuscriptMetadata());
        assertNotNull(submission.getMetadata().getJournalMetadata());
        assertNotNull(submission.getMetadata().getArticleMetadata());
        assertNotNull(submission.getMetadata().getPersons());

        // Cannot compare ID strings, as they change when uploading to a Fedora server.
        Publication publication = (Publication)entities.get(submissionEntity.getPublication());
        assertEquals(submission.getMetadata().getManuscriptMetadata().getTitle(), publication.getTitle());
        assertEquals(submission.getMetadata().getArticleMetadata().getDoi().toString(), publication.getDoi());
        Journal journal = (Journal)entities.get(publication.getJournal());
        assertEquals(submission.getMetadata().getJournalMetadata().getJournalTitle(), journal.getName());

        assertNotNull(submission.getFiles());
        assertEquals(2, submission.getFiles().size());
    }

}