/**
 * Copyright © 2010-2011 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.restdriver.clientdriver.clientdriver.integration;

import java.util.regex.Pattern;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Assert;
import org.junit.Test;

import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import com.github.restdriver.clientdriver.ClientDriverRequest;
import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverResponse;
import com.github.restdriver.clientdriver.exception.ClientDriverFailedExpectationException;

public class BenchFailTest {

    private ClientDriver bServer;

    @Test
    public void testUnexpectedCall() throws Exception {
        bServer = new ClientDriverFactory().createClientDriver();

        // No expectations defined

        HttpClient client = new DefaultHttpClient();
        HttpGet getter = new HttpGet(bServer.getBaseUrl() + "/blah?foo=bar");

        client.execute(getter);

        try {
            bServer.shutdown();
            Assert.fail();
        } catch (ClientDriverFailedExpectationException bre) {
            Assert.assertEquals("Unexpected request: /blah?foo=bar", bre.getMessage());
        }

    }

    @Test
    public void testUnmatchedExpectation() throws Exception {
        bServer = new ClientDriverFactory().createClientDriver();

        bServer.addExpectation(new ClientDriverRequest("/blah"), new ClientDriverResponse("OUCH!!").withStatus(200));
        bServer.addExpectation(new ClientDriverRequest("/blah"), new ClientDriverResponse("OUCH!!").withStatus(404));

        // no requests made

        try {
            bServer.shutdown();
            Assert.fail();
        } catch (ClientDriverFailedExpectationException bre) {
            Assert.assertEquals("2 unmatched expectation(s), first is: BenchRequest: GET /blah; ", bre.getMessage());
        }

    }

    @Test
    public void testJettyWorkingWithMethodButIncorrectParams() throws Exception {
        bServer = new ClientDriverFactory().createClientDriver();

        bServer.addExpectation(new ClientDriverRequest("/blah").withMethod(Method.POST).withParam("gang", "green"),
                new ClientDriverResponse("OUCH!!").withStatus(200).withContentType("text/plain").withHeader("Server",
                        "TestServer"));

        HttpClient client = new DefaultHttpClient();

        String baseUrl = bServer.getBaseUrl();
        HttpPost poster = new HttpPost(baseUrl + "/blah?gang=groon");

        client.execute(poster);

        try {
            bServer.shutdown();
            Assert.fail();
        } catch (ClientDriverFailedExpectationException bre) {
            Assert.assertEquals("Unexpected request: /blah?gang=groon", bre.getMessage());
        }

    }

    @Test
    public void testJettyWorkingWithMethodButIncorrectParamsPattern() throws Exception {
        bServer = new ClientDriverFactory().createClientDriver();

        bServer.addExpectation(new ClientDriverRequest(Pattern.compile("/b[a-z]{3}")).withMethod(Method.POST).withParam(
                "gang", Pattern.compile("r")), new ClientDriverResponse("OUCH!!").withStatus(200)
                .withContentType("text/plain").withHeader("Server", "TestServer"));

        HttpClient client = new DefaultHttpClient();

        String baseUrl = bServer.getBaseUrl();
        HttpPost poster = new HttpPost(baseUrl + "/blah?gang=goon");

        client.execute(poster);

        try {
            bServer.shutdown();
            Assert.fail();
        } catch (ClientDriverFailedExpectationException bre) {
            Assert.assertEquals("Unexpected request: /blah?gang=goon", bre.getMessage());
        }

    }

}
