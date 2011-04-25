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
package com.github.restdriver.clientdriver;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.github.restdriver.clientdriver.exception.ClientDriverInternalException;
import org.apache.commons.io.IOUtils;

/**
 * Implementation of {@link RequestMatcher}. This implementation expects exact match in terms of the HTTP method, the
 * path &amp; query string, and any body of the request.
 */
public final class DefaultRequestMatcher implements RequestMatcher {

    /**
     * Checks for a match between an actual {@link HttpServletRequest} and an expected {@link ClientDriverRequest}. This
     * implementation is as strict as it can be with exact matching for Strings, but can also use regular expressions in
     * the form of Patterns.
     * 
     * @param actualRequest
     *            The actual request
     * @param expectedRequest
     *            The expected {@link ClientDriverRequest}
     * @return True if there is a match, falsetto otherwise.
     */
    @Override
    public boolean isMatch(HttpServletRequest actualRequest, ClientDriverRequest expectedRequest) {

        // TODO: Better diagnostics from this method.  See https://github.com/rest-driver/rest-driver/issues/7

        // same method?
        if (!actualRequest.getMethod().equals(expectedRequest.getMethod().toString())) {
            return false;
        }
        // same base path?
        if (!isStringOrPattternMatch(actualRequest.getPathInfo(), expectedRequest.getPath())) {
            return false;
        }

        // same number of query-string parameters?
        if (actualRequest.getParameterMap().size() != expectedRequest.getParams().size()) {
            return false;
        }

        // same keys/values in query-string parameter map?
        Map<String, Object> expectedParams = expectedRequest.getParams();
        for (String expectedKey : expectedParams.keySet()) {

            String actualParamValue = actualRequest.getParameter(expectedKey);

            if (actualParamValue == null) {
                return false;
            }

            Object expectedParamValue = expectedParams.get(expectedKey);

            if (!isStringOrPattternMatch(actualParamValue, expectedParamValue)) {
                return false;
            }

        }

        // same request body?
        if (expectedRequest.getBodyContent() != null) {

            // this is needed because clients have a habit of putting
            // "text/html; charset=UTF-8" when you only ask for "text/html".
            String actualContentType = actualRequest.getContentType();
            if (actualContentType.contains(";")) {
                actualContentType = actualContentType.substring(0, actualContentType.indexOf(';'));
            }

            // same type?
            if (!isStringOrPattternMatch(actualContentType, expectedRequest.getBodyContentType())) {
                return false;
            }

            // same content?
            try {
                if (!isStringOrPattternMatch(IOUtils.toString(actualRequest.getReader()), expectedRequest
                        .getBodyContent())) {
                    return false;
                }
            } catch (IOException ioException) {
                throw new ClientDriverInternalException("Internal error, IOException while reading from body content",
                        ioException);
            }
        }

        return true;

    }

    private boolean isStringOrPattternMatch(String actual, Object expected) {
        if (expected instanceof String) {

            return actual.equals(expected);

        } else if (expected instanceof Pattern) {

            Pattern pattern = (Pattern) expected;
            return pattern.matcher(actual).matches();

        } else {
            throw new ClientDriverInternalException("RequestMatcherImpl asked to match " + expected.getClass()
                    + ", but only knows String and Pattern.", null);
        }
    }

}
