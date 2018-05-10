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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.dataconservancy.nihms.builder.fs.PassJsonFedoraAdapter;
import org.dataconservancy.nihms.integration.BaseIT;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PassJsonFedoraAdapterIT extends BaseIT {

    private String SAMPLE_DATA_FILE = "SampleSubmissionData.json";
    private URL sampleDataUrl;
    private PassJsonFedoraAdapter reader;

    @Before
    public void setup() {
        sampleDataUrl = this.getClass().getResource(SAMPLE_DATA_FILE);
        reader = new PassJsonFedoraAdapter();
    }

    @Test
    public void roundTrip() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            long testFileCount = mapper.readTree(new FileInputStream(sampleDataUrl.getPath()))
                    .findValuesAsText("@type").stream().filter("File"::equals).count();
            assertTrue(testFileCount > 0);

            OkHttpClient http = new OkHttpClient();
            Request esFileQuery = new Request.Builder().url(System.getProperty(PASS_ES_URL) + "_search?q=@type:File").build();
            long existingFileCount = 0;

            try (Response res = http.newCall(esFileQuery).execute()) {
                LOG.debug("Executing request {}", esFileQuery.url().toString());
                JsonNode jsonNode = mapper.readTree(res.body().bytes());
                LOG.debug("Evaluating JSON {}", (jsonNode != null) ? jsonNode.asText() : null);

                int hits;
                if (jsonNode == null || jsonNode.findValue("hits") == null) {
                    existingFileCount = 0;
                } else {
                    existingFileCount = jsonNode.findValue("hits").get("total").asInt();
                }
            }

            long expectedFileCount = existingFileCount + testFileCount;



            // Upload the sample data to the Fedora repo.
            InputStream is = new FileInputStream(sampleDataUrl.getPath());
            URI submissionUri = reader.jsonToFcrepo(is);
            is.close();

            // wait for files to show up in index
            attemptAndVerify(60, () -> {
                LOG.debug("Looking for {} files...", expectedFileCount);
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

                if (hits.get("total").asInt() == expectedFileCount) {
                    LOG.debug("{} hits, returning true", expectedFileCount);
                    return true;
                } else {
                    LOG.debug("{} hits, expected {}", hits.get("total").asInt(), expectedFileCount);
                    return null;
                }
            } );

            // Download the data from the server to a temporary JSON file
            File tempFile = File.createTempFile("fcrepo", ".json");
            tempFile.deleteOnExit();
            String tempFilePath = tempFile.getCanonicalPath();
            FileOutputStream fos = new FileOutputStream(tempFile);
            reader.fcrepoToJson(submissionUri, fos);
            fos.close();

            // Read the two files into JSON models
            is = new FileInputStream(sampleDataUrl.getPath());
            String origString = IOUtils.toString(is, Charset.defaultCharset());
            LOG.debug(">>> Original: ");
            LOG.debug(origString);
            JsonArray origJson = new JsonParser().parse(origString).getAsJsonArray();
            is.close();
            is = new FileInputStream(tempFilePath);
            String resultString = IOUtils.toString(is, Charset.defaultCharset());
            JsonArray resultJson = new JsonParser().parse(resultString).getAsJsonArray();
            is.close();

            LOG.debug(">>> Server: ");
            LOG.debug(resultString);

            // Compare the two files.  Array contents may be in a different order, and URIs have changed,
            // so find objects with same @type field and compare the values of their first properties.
//            assertEquals(origJson.size(), resultJson.size());
            for (JsonElement origElement : origJson) {
                boolean found = false;
                JsonObject origObj = origElement.getAsJsonObject();
                String origType = origObj.get("@type").getAsString();
                String firstPropName = origObj.keySet().iterator().next();
                for (JsonElement resultElement : resultJson) {
                    JsonObject resObj = resultElement.getAsJsonObject();
                    if (origType.equals(resObj.get("@type").getAsString()) &&
                        origObj.get(firstPropName).getAsString().equals(resObj.get(firstPropName).getAsString())) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Could not find source object with '" + firstPropName + "' equal to '" + origObj.get(firstPropName) + "' of type '" + origType + "' in result array.", found);
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not close the sample data file");
        }
    }

}