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

import org.dataconservancy.nihms.transport.TransportResponse;
import org.swordapp.client.DepositReceipt;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Sword2DepositReceiptResponse implements TransportResponse {

    private DepositReceipt receipt;

    public Sword2DepositReceiptResponse(DepositReceipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("Deposit receipt must not be null.");
        }
        this.receipt = receipt;
    }

    // TODO: return true if *accepted* but will need to be polled for success
    @Override
    public boolean success() {
        return receipt.getStatusCode() > 199 && receipt.getStatusCode() < 300;
    }

    @Override
    public Throwable error() {
        return null;
    }

}
