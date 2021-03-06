/*
 * Copyright 2018 Otavio R. Piske <angusyoung@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maestro.common;

import org.apache.commons.lang3.StringUtils;
import org.maestro.common.exceptions.MaestroException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Maestro URL utilities
 */
public class URLUtils {

    private URLUtils() {}

    /**
     * The client uses the mqtt://{host} url format so it's the same as the C client. This
     * method ensures that the URLs follows this format.
     * @param url the URL to sanitize
     * @return the sanitized URL
     */
    public static String sanitizeURL(final String url) {
        return StringUtils.replace(url, "mqtt", "tcp");
    }


    /**
     * Get the host part from a URL
     * @param string the URL (ie.: http://hostname:port)
     * @return the host part of the URL
     */
    public static String getHostnameFromURL(final String string) {
        try {
            URL url = new URL(string);

            return url.getHost();
        } catch (MalformedURLException e) {
            throw new MaestroException("Invalid URL: " + string);
        }
    }
}
