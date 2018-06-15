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
package org.dataconservancy.pass.deposit.messaging.policy;

import org.dataconservancy.pass.deposit.messaging.status.DepositStatusEvaluator;
import org.dataconservancy.pass.deposit.messaging.status.StatusEvaluator;
import org.dataconservancy.pass.model.Deposit;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IntermediateDepositStatusPolicyTest {

    private StatusEvaluator<Deposit.DepositStatus> evaluator;

    private IntermediateDepositStatusPolicy underTest;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        evaluator = mock(StatusEvaluator.class);
        underTest = new IntermediateDepositStatusPolicy(evaluator);
    }

    @Test
    public void testNullStatus() throws Exception {
        assertTrue(underTest.accept(null));
    }

    @Test
    public void testTerminalStatus() throws Exception {
        Deposit.DepositStatus terminal = Deposit.DepositStatus.ACCEPTED;
        when(evaluator.isTerminal(terminal)).thenReturn(true);

        assertFalse(underTest.accept(terminal));
        verify(evaluator).isTerminal(terminal);
    }

    @Test
    public void testIntermediateStatus() throws Exception {
        Deposit.DepositStatus terminal = Deposit.DepositStatus.SUBMITTED;
        when(evaluator.isTerminal(terminal)).thenReturn(false);

        assertTrue(underTest.accept(terminal));
        verify(evaluator).isTerminal(terminal);
    }

    @Test
    public void testFailedStatus() throws Exception {
        Deposit.DepositStatus failed = Deposit.DepositStatus.FAILED;
        when(evaluator.isTerminal(failed)).thenReturn(false);

        assertTrue(underTest.accept(failed));
        verify(evaluator).isTerminal(failed);
    }
}