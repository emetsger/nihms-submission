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

package org.dataconservancy.nihms.integration;

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public abstract class BaseIT {

    protected static final Logger LOG = LoggerFactory.getLogger(BaseIT.class);

    protected static final String DOCKER_HOST_PROPERTY = "docker.host.address";

    protected static final String PASS_FEDORA_USER = "pass.fedora.user";

    protected static final String PASS_FEDORA_PASSWORD = "pass.fedora.password";

    protected static final String PASS_FEDORA_BASEURL = "pass.fedora.baseurl";

    protected static final String PASS_ES_URL = "pass.elasticsearch.url";

    @Before
    public void verifyDockerHostProperty() throws Exception {
        assertNotNull("Expected required system property 'docker.host.address' to be set.",
                System.getProperty("docker.host.address"));
    }

    @Before
    public void verifyPassClientProperties() {
        assertTrue("Missing expected system property " + PASS_FEDORA_USER,
                System.getProperties().containsKey(PASS_FEDORA_USER));
        assertTrue("Missing expected system property " + PASS_FEDORA_PASSWORD,
                System.getProperties().containsKey(PASS_FEDORA_PASSWORD));
        assertTrue("Missing expected system property " + PASS_FEDORA_BASEURL,
                System.getProperties().containsKey(PASS_FEDORA_BASEURL));
        assertTrue("Missing expected system property " + PASS_ES_URL,
                System.getProperties().containsKey(PASS_ES_URL));
    }

    public static <T> void attemptAndVerify(int times, Callable<T> callable, Function<T, Boolean> verification) {
        if (times < 1) {
            throw new IllegalArgumentException("times must be a positive integer");
        }

        long sleepMs = 2000;

        Throwable throwable = null;

        for (int i = 0; i < times; i++) {

            T result = null;
            try {
                result = callable.call();
            } catch (Exception e) {
                throwable = e;
            }

            if (result != null) {
                StringBuilder msg = new StringBuilder("Attempt failed");
                if (throwable != null) {
                    msg.append(" with: ").append(throwable.getMessage());
                } else {
                    msg.append("!");
                }
                Boolean verificationResult = verification.apply(result);
                if (verificationResult == null) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ie) {
                        Thread.interrupted();
                        break;
                    }
                    continue;
                }
                assertTrue(msg.toString(), verificationResult);
                break;
            } else {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                    break;
                }
            }

        }
    }

}
