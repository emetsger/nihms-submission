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

package org.dataconservancy.nihms.cli;

import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class NihmsSubmissionAppTest {

    /**
     * Insures that a property key can be resolved to a properties file on the classpath, and that a base64 encoded
     * properties file can be decoded and read as a Map of strings.
     *
     * @throws Exception
     */
    @Test
    public void resolveTransportHints() throws Exception {
        String transportKey = "encoded";
        String expectedKey = "foo";
        String expectedValue = "bar";

        NihmsSubmissionApp app = new NihmsSubmissionApp(mock(File.class), transportKey);
        Map<String, String> resolved = app.resolveTransportHints(transportKey);
        assertEquals(expectedValue, resolved.get(expectedKey));
    }

    @Test
    public void parseDockerHostAddressFromTcpUrl() throws Exception {
        assertEquals("foo", NihmsSubmissionApp.parseDockerHostAddress("tcp://foo:2345"));
    }

    @Test
    public void parseDockerHostAddressFromTcpUrlTrailingSlash() throws Exception {
        assertEquals("foo", NihmsSubmissionApp.parseDockerHostAddress("tcp://foo:2345/"));
    }

    @Test
    public void parseDockerHostAddressFromTcpUrlNoPort() throws Exception {
        assertEquals("foo", NihmsSubmissionApp.parseDockerHostAddress("tcp://foo"));
    }

    @Test
    public void parseDockerHostAddressFromTcpUrlNoPortTrailingSlash() throws Exception {
        assertEquals("foo", NihmsSubmissionApp.parseDockerHostAddress("tcp://foo/"));
    }

    @Test
    public void parseDockerHostAddressNotUrl() throws Exception {
        assertEquals("foo", NihmsSubmissionApp.parseDockerHostAddress("foo"));
    }
}