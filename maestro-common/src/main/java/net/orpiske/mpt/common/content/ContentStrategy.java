/*
 *  Copyright 2017 Otavio R. Piske <angusyoung@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.orpiske.mpt.common.content;

/**
 * The message content strategy for sending data
 */
public interface ContentStrategy {
    /**
     * Set the message size
     * @param size the message size
     */
    void setSize(int size);

    /**
     * Sets the size based on a size specification string
     * @param sizeSpec the size specification string as formatted by ({@link MessageSize})
     */
    void setSize(String sizeSpec);


    /**
     * Gets the message content to send
     * @return the message content to send
     */
    String getContent();
}
