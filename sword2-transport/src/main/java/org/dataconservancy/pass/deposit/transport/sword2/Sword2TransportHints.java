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
package org.dataconservancy.pass.deposit.transport.sword2;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface Sword2TransportHints {

    /**
     * Property identifying the SWORD service document URL
     */
    String SWORD_SERVICE_DOC_URL = "deposit.transport.sword.service-doc";

    /**
     * Property identifying the Atom Publishing Protocol Collection to deposit the package to
     */
    String SWORD_COLLECTION_URL = "deposit.transport.sword.target-collection";

    /**
     * Property identifying the On-Behalf-Of user
     */
    String SWORD_ON_BEHALF_OF_USER = "deposit.transport.sword.on-behalf-of";

}
