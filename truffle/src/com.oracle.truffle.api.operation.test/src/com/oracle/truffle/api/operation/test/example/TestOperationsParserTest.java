/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.api.operation.test.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.operation.ContinuationResult;
import com.oracle.truffle.api.operation.OperationConfig;
import com.oracle.truffle.api.operation.OperationLabel;
import com.oracle.truffle.api.operation.OperationLocal;
import com.oracle.truffle.api.operation.OperationNodes;
import com.oracle.truffle.api.operation.OperationRootNode;
import com.oracle.truffle.api.operation.OperationParser;

public class TestOperationsParserTest {
    // @formatter:off

    private static final TestLanguage LANGUAGE = null;

    private static RootCallTarget parse(OperationParser<TestOperationsGen.Builder> builder) {
        OperationRootNode operationsNode = parseNode(builder);
        return ((RootNode) operationsNode).getCallTarget();
    }

    private static OperationRootNode parseNode(OperationParser<TestOperationsGen.Builder> builder) {
        OperationNodes<TestOperations> nodes = TestOperationsGen.create(OperationConfig.DEFAULT, builder);
        TestOperations op = nodes.getNodes().get(nodes.getNodes().size() - 1);
        System.out.println(op.dump());
        return op;
    }

    @Test
    public void testAdd() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            b.beginReturn();
            b.beginAddOperation();
            b.emitLoadArgument(0);
            b.emitLoadArgument(1);
            b.endAddOperation();
            b.endReturn();

            b.endRoot();
        });

        Assert.assertEquals(42L, root.call(20L, 22L));
        Assert.assertEquals("foobar", root.call("foo", "bar"));
        Assert.assertEquals(100L, root.call(120L, -20L));
    }

    @Test
    public void testMax() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);
            b.beginIfThenElse();

            b.beginLessThanOperation();
            b.emitLoadArgument(0);
            b.emitLoadArgument(1);
            b.endLessThanOperation();

            b.beginReturn();
            b.emitLoadArgument(1);
            b.endReturn();

            b.beginReturn();
            b.emitLoadArgument(0);
            b.endReturn();

            b.endIfThenElse();

            b.endRoot();
        });

        Assert.assertEquals(42L, root.call(42L, 13L));
        Assert.assertEquals(42L, root.call(42L, 13L));
        Assert.assertEquals(42L, root.call(42L, 13L));
        Assert.assertEquals(42L, root.call(13L, 42L));
    }

    @Test
    public void testIfThen() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);
            b.beginIfThen();

            b.beginLessThanOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(0L);
            b.endLessThanOperation();

            b.beginReturn();
            b.emitLoadConstant(0L);
            b.endReturn();

            b.endIfThen();

            b.beginReturn();
            b.emitLoadArgument(0);
            b.endReturn();

            b.endRoot();
        });

        Assert.assertEquals(0L, root.call(-2L));
        Assert.assertEquals(0L, root.call(-1L));
        Assert.assertEquals(0L, root.call(0L));
        Assert.assertEquals(1L, root.call(1L));
        Assert.assertEquals(2L, root.call(2L));
    }

    @Test
    public void testSumLoop() {

        // i = 0;j = 0;
        // while ( i < arg0 ) { j = j + i;i = i + 1;}
        // return j;

        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);
            OperationLocal locI = b.createLocal();
            OperationLocal locJ = b.createLocal();

            b.beginStoreLocal(locI);
            b.emitLoadConstant(0L);
            b.endStoreLocal();

            b.beginStoreLocal(locJ);
            b.emitLoadConstant(0L);
            b.endStoreLocal();

            b.beginWhile();
                b.beginLessThanOperation();
                b.emitLoadLocal(locI);
                b.emitLoadArgument(0);
                b.endLessThanOperation();

                b.beginBlock();
                    b.beginStoreLocal(locJ);
                        b.beginAddOperation();
                        b.emitLoadLocal(locJ);
                        b.emitLoadLocal(locI);
                        b.endAddOperation();
                    b.endStoreLocal();

                    b.beginStoreLocal(locI);
                        b.beginAddOperation();
                        b.emitLoadLocal(locI);
                        b.emitLoadConstant(1L);
                        b.endAddOperation();
                    b.endStoreLocal();
                b.endBlock();
            b.endWhile();

            b.beginReturn();
            b.emitLoadLocal(locJ);
            b.endReturn();


            b.endRoot();
        });

        Assert.assertEquals(45L, root.call(10L));
    }

    @Test
    public void testTryCatch() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            OperationLocal local = b.createLocal();
            b.beginTryCatch(local);

            b.beginIfThen();
            b.beginLessThanOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(0L);
            b.endLessThanOperation();

            b.emitThrowOperation();

            b.endIfThen();

            b.beginReturn();
            b.emitLoadConstant(1L);
            b.endReturn();

            b.endTryCatch();

            b.beginReturn();
            b.emitLoadConstant(0L);
            b.endReturn();

            b.endRoot();
        });

        Assert.assertEquals(1L, root.call(-1L));
        Assert.assertEquals(0L, root.call(1L));
    }

    @Test
    public void testVariableBoxingElim() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            OperationLocal local0 = b.createLocal();
            OperationLocal local1 = b.createLocal();

            b.beginStoreLocal(local0);
            b.emitLoadConstant(0L);
            b.endStoreLocal();

            b.beginStoreLocal(local1);
            b.emitLoadConstant(0L);
            b.endStoreLocal();

            b.beginWhile();

            b.beginLessThanOperation();
            b.emitLoadLocal(local0);
            b.emitLoadConstant(100L);
            b.endLessThanOperation();

            b.beginBlock();

            b.beginStoreLocal(local1);
            b.beginAddOperation();
            b.beginAlwaysBoxOperation();
            b.emitLoadLocal(local1);
            b.endAlwaysBoxOperation();
            b.emitLoadLocal(local0);
            b.endAddOperation();
            b.endStoreLocal();

            b.beginStoreLocal(local0);
            b.beginAddOperation();
            b.emitLoadLocal(local0);
            b.emitLoadConstant(1L);
            b.endAddOperation();
            b.endStoreLocal();

            b.endBlock();

            b.endWhile();

            b.beginReturn();
            b.emitLoadLocal(local1);
            b.endReturn();


            b.endRoot();
        });

        Assert.assertEquals(4950L, root.call());
    }

    private static void testOrdering(boolean expectException, RootCallTarget root, Long... order) {
        List<Object> result = new ArrayList<>();

        try {
            root.call(result);
            if (expectException) {
                Assert.fail();
            }
        } catch (AbstractTruffleException ex) {
            if (!expectException) {
                throw new AssertionError("unexpected", ex);
            }
        }

        Assert.assertArrayEquals("expected " + Arrays.toString(order) + " got " + result, order, result.toArray());
    }

    @Test
    public void testFinallyTryBasic() {

        // try { 1;} finally { 2;}
        // expected 1, 2

        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);
            b.beginFinallyTry();
                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(2L);
                b.endAppenderOperation();

                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(1L);
                b.endAppenderOperation();
            b.endFinallyTry();

            b.beginReturn();
            b.emitLoadConstant(0L);
            b.endReturn();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 2L);
    }

    @Test
    public void testFinallyTryException() {

        // try { 1;throw;2;} finally { 3;}
        // expected: 1, 3

        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);
            b.beginFinallyTry();
                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(3L);
                b.endAppenderOperation();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.emitThrowOperation();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTry();

            b.beginReturn();
            b.emitLoadConstant(0L);
            b.endReturn();


            b.endRoot();
        });

        testOrdering(true, root, 1L, 3L);
    }

    @Test
    public void testFinallyTryReturn() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);
            b.beginFinallyTry();
                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(1L);
                b.endAppenderOperation();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();

                    b.beginReturn();
                    b.emitLoadConstant(0L);
                    b.endReturn();
                b.endBlock();
            b.endFinallyTry();

            b.beginAppenderOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(3L);
            b.endAppenderOperation();


            b.endRoot();
        });

        testOrdering(false, root, 2L, 1L);
    }

    @Test
    public void testFinallyTryBranchOut() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { 1;goto lbl;2;} finally { 3;} 4;lbl: 5;
            // expected: 1, 3, 5

            OperationLabel lbl = b.createLabel();

            b.beginFinallyTry();
                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(3L);
                b.endAppenderOperation();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.emitBranch(lbl);

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTry();

            b.beginAppenderOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(4L);
            b.endAppenderOperation();

            b.emitLabel(lbl);

            b.beginAppenderOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(5L);
            b.endAppenderOperation();

            b.beginReturn();
            b.emitLoadConstant(0L);
            b.endReturn();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 3L, 5L);
    }

    @Test
    public void testFinallyTryCancel() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { 1;return;} finally { 2;goto lbl;} 3;lbl: 4;
            // expected: 1, 2, 4

            OperationLabel lbl = b.createLabel();

            b.beginFinallyTry();
                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();

                    b.emitBranch(lbl);
                b.endBlock();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.beginReturn();
                    b.emitLoadConstant(0L);
                    b.endReturn();
                b.endBlock();
            b.endFinallyTry();

            b.beginAppenderOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(3L);
            b.endAppenderOperation();

            b.emitLabel(lbl);

            b.beginAppenderOperation();
            b.emitLoadArgument(0);
            b.emitLoadConstant(4L);
            b.endAppenderOperation();

            b.beginReturn();
            b.emitLoadConstant(0L);
            b.endReturn();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 2L, 4L);
    }

    @Test
    public void testFinallyTryInnerCf() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { 1;return;2 } finally { 3;goto lbl;4;lbl: 5;}
            // expected: 1, 3, 5

            b.beginFinallyTry();
                b.beginBlock();
                    OperationLabel lbl = b.createLabel();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(3L);
                    b.endAppenderOperation();

                    b.emitBranch(lbl);

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(4L);
                    b.endAppenderOperation();

                    b.emitLabel(lbl);

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(5L);
                    b.endAppenderOperation();
                b.endBlock();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.beginReturn();
                    b.emitLoadConstant(0L);
                    b.endReturn();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTry();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 3L, 5L);
    }

    @Test
    public void testFinallyTryNestedTry() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { try { 1;return;2;} finally { 3;} } finally { 4;}
            // expected: 1, 3, 4

            b.beginFinallyTry();
                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(4L);
                    b.endAppenderOperation();
                b.endBlock();

                b.beginFinallyTry();
                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(3L);
                        b.endAppenderOperation();
                    b.endBlock();

                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(1L);
                        b.endAppenderOperation();

                        b.beginReturn();
                        b.emitLoadConstant(0L);
                        b.endReturn();

                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(2L);
                        b.endAppenderOperation();
                    b.endBlock();
                b.endFinallyTry();
            b.endFinallyTry();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 3L, 4L);
    }

    @Test
    public void testFinallyTryNestedFinally() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { 1;return;2;} finally { try { 3;return;4;} finally { 5;} }
            // expected: 1, 3, 5

            b.beginFinallyTry();
                b.beginFinallyTry();
                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(5L);
                        b.endAppenderOperation();
                    b.endBlock();

                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(3L);
                        b.endAppenderOperation();

                        b.beginReturn();
                        b.emitLoadConstant(0L);
                        b.endReturn();

                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(4L);
                        b.endAppenderOperation();
                    b.endBlock();
                b.endFinallyTry();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.beginReturn();
                    b.emitLoadConstant(0L);
                    b.endReturn();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTry();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 3L, 5L);
    }

    @Test
    public void testFinallyTryNestedTryThrow() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { try { 1;throw;2;} finally { 3;} } finally { 4;}
            // expected: 1, 3, 4

            b.beginFinallyTry();
                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(4L);
                    b.endAppenderOperation();
                b.endBlock();

                b.beginFinallyTry();
                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(3L);
                        b.endAppenderOperation();
                    b.endBlock();

                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(1L);
                        b.endAppenderOperation();

                        b.emitThrowOperation();

                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(2L);
                        b.endAppenderOperation();
                    b.endBlock();
                b.endFinallyTry();
            b.endFinallyTry();


            b.endRoot();
        });

        testOrdering(true, root, 1L, 3L, 4L);
    }

    @Test
    public void testFinallyTryNestedFinallyThrow() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // try { 1;throw;2;} finally { try { 3;throw;4;} finally { 5;} }
            // expected: 1, 3, 5

            b.beginFinallyTry();
                b.beginFinallyTry();
                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(5L);
                        b.endAppenderOperation();
                    b.endBlock();

                    b.beginBlock();
                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(3L);
                        b.endAppenderOperation();

                        b.emitThrowOperation();

                        b.beginAppenderOperation();
                        b.emitLoadArgument(0);
                        b.emitLoadConstant(4L);
                        b.endAppenderOperation();
                    b.endBlock();
                b.endFinallyTry();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.emitThrowOperation();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTry();


            b.endRoot();
        });

        testOrdering(true, root, 1L, 3L, 5L);
    }

    @Test
    public void testFinallyTryNoExceptReturn() {

        // try { 1;return;2;} finally noexcept { 3;}
        // expected: 1, 3

        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            b.beginFinallyTryNoExcept();
                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(3L);
                b.endAppenderOperation();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.beginReturn();
                    b.emitLoadConstant(0L);
                    b.endReturn();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTryNoExcept();


            b.endRoot();
        });

        testOrdering(false, root, 1L, 3L);
    }

    @Test
    public void testFinallyTryNoExceptException() {

        // try { 1;throw;2;} finally noexcept { 3;}
        // expected: 1

        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            b.beginFinallyTryNoExcept();
                b.beginAppenderOperation();
                b.emitLoadArgument(0);
                b.emitLoadConstant(3L);
                b.endAppenderOperation();

                b.beginBlock();
                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(1L);
                    b.endAppenderOperation();

                    b.emitThrowOperation();

                    b.beginAppenderOperation();
                    b.emitLoadArgument(0);
                    b.emitLoadConstant(2L);
                    b.endAppenderOperation();
                b.endBlock();
            b.endFinallyTryNoExcept();


            b.endRoot();
        });

        testOrdering(true, root, 1L);
    }


    @Test
    public void testTeeLocal() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            OperationLocal local = b.createLocal();

            b.beginTeeLocal(local);
            b.emitLoadConstant(1L);
            b.endTeeLocal();

            b.beginReturn();
            b.emitLoadLocal(local);
            b.endReturn();


            b.endRoot();
        });

        Assert.assertEquals(1L, root.call());
    }

    @Test
    public void testYield() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            b.beginYield();
            b.emitLoadConstant(1L);
            b.endYield();

            b.beginYield();
            b.emitLoadConstant(2L);
            b.endYield();

            b.beginReturn();
            b.emitLoadConstant(3L);
            b.endReturn();

            b.endRoot();
        });

        ContinuationResult r1 = (ContinuationResult) root.call();
        Assert.assertEquals(1L, r1.getResult());

        ContinuationResult r2 = (ContinuationResult) r1.continueWith(null);
        Assert.assertEquals(2L, r2.getResult());

        Assert.assertEquals(3L, r2.continueWith(null));
    }


    @Test
    public void testYieldLocal() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            OperationLocal loc = b.createLocal();

            // loc = 0
            // yield loc
            // loc = loc + 1
            // yield loc
            // loc = loc + 1
            // return loc

            b.beginStoreLocal(loc);
            b.emitLoadConstant(0L);
            b.endStoreLocal();

            b.beginYield();
            b.emitLoadLocal(loc);
            b.endYield();

            b.beginStoreLocal(loc);
            b.beginAddOperation();
            b.emitLoadLocal(loc);
            b.emitLoadConstant(1L);
            b.endAddOperation();
            b.endStoreLocal();

            b.beginYield();
            b.emitLoadLocal(loc);
            b.endYield();

            b.beginStoreLocal(loc);
            b.beginAddOperation();
            b.emitLoadLocal(loc);
            b.emitLoadConstant(1L);
            b.endAddOperation();
            b.endStoreLocal();

            b.beginReturn();
            b.emitLoadLocal(loc);
            b.endReturn();

            b.endRoot();
        });

        ContinuationResult r1 = (ContinuationResult) root.call();
        Assert.assertEquals(0L, r1.getResult());

        ContinuationResult r2 = (ContinuationResult) r1.continueWith(null);
        Assert.assertEquals(1L, r2.getResult());

        Assert.assertEquals(2L, r2.continueWith(null));
    }
    @Test
    public void testYieldStack() {
        RootCallTarget root = parse(b -> {
            b.beginRoot(LANGUAGE);

            // return (yield 1) + (yield 2)
            b.beginReturn();
            b.beginAddOperation();

            b.beginYield();
            b.emitLoadConstant(1L);
            b.endYield();

            b.beginYield();
            b.emitLoadConstant(2L);
            b.endYield();

            b.endAddOperation();
            b.endReturn();


            b.endRoot();
        });

        ContinuationResult r1 = (ContinuationResult) root.call();
        Assert.assertEquals(1L, r1.getResult());

        ContinuationResult r2 = (ContinuationResult) r1.continueWith(3L);
        Assert.assertEquals(2L, r2.getResult());

        Assert.assertEquals(7L, r2.continueWith(4L));
    }

    @Test
    public void testNestedFunctions() {
        RootCallTarget root = parse(b -> {
            // this simulates following in python:
            // return (lambda: 1)()
            b.beginRoot(LANGUAGE);

            b.beginReturn();

            b.beginInvoke();

                b.beginRoot(LANGUAGE);

                b.beginReturn();
                b.emitLoadConstant(1L);
                b.endReturn();

                TestOperations innerRoot = b.endRoot();

            b.emitLoadConstant(innerRoot);
            b.endInvoke();

            b.endReturn();

            b.endRoot();
        });

        Assert.assertEquals(1L, root.call());
    }

    @Test
    public void testNonlocalRead() {
        RootCallTarget root = parse(b -> {
            // x = 1
            // return (lambda: x)()
            b.beginRoot(LANGUAGE);

            OperationLocal xLoc = b.createLocal();

            b.beginStoreLocal(xLoc);
            b.emitLoadConstant(1L);
            b.endStoreLocal();

            b.beginReturn();

            b.beginInvoke();

                b.beginRoot(LANGUAGE);
                b.beginReturn();
                b.beginLoadLocalMaterialized(xLoc);
                b.emitLoadArgument(0);
                b.endLoadLocalMaterialized();
                b.endReturn();
                TestOperations inner = b.endRoot();

            b.beginCreateClosure();
            b.emitLoadConstant(inner);
            b.endCreateClosure();

            b.endInvoke();
            b.endReturn();

            b.endRoot();
        });

        Assert.assertEquals(1L, root.call());
    }

    @Test
    public void testNonlocalWrite() {
        RootCallTarget root = parse(b -> {
            // x = 1
            // (lambda: x = 2)()
            // return x
            b.beginRoot(LANGUAGE);

            OperationLocal xLoc = b.createLocal();

            b.beginStoreLocal(xLoc);
            b.emitLoadConstant(1L);
            b.endStoreLocal();


            b.beginInvoke();

                b.beginRoot(LANGUAGE);

                b.beginStoreLocalMaterialized(xLoc);
                b.emitLoadArgument(0);
                b.emitLoadConstant(2L);
                b.endStoreLocalMaterialized();

                b.beginReturn();
                b.emitLoadConstant(null);
                b.endReturn();

                TestOperations inner = b.endRoot();

            b.beginCreateClosure();
            b.emitLoadConstant(inner);
            b.endCreateClosure();

            b.endInvoke();

            b.beginReturn();
            b.emitLoadLocal(xLoc);
            b.endReturn();

            b.endRoot();
        });

        Assert.assertEquals(2L, root.call());
    }
}
