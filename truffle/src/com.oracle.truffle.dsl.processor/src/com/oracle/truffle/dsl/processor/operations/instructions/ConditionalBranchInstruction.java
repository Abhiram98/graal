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
package com.oracle.truffle.dsl.processor.operations.instructions;

import javax.lang.model.type.DeclaredType;

import com.oracle.truffle.dsl.processor.ProcessorContext;
import com.oracle.truffle.dsl.processor.java.model.CodeTree;
import com.oracle.truffle.dsl.processor.java.model.CodeTreeBuilder;
import com.oracle.truffle.dsl.processor.operations.OperationGeneratorUtils;
import com.oracle.truffle.dsl.processor.operations.OperationsContext;

public class ConditionalBranchInstruction extends Instruction {

    private final ProcessorContext context = ProcessorContext.getInstance();
    private final DeclaredType typeConditionProfile = context.getDeclaredType("com.oracle.truffle.api.profiles.ConditionProfile");

    public ConditionalBranchInstruction(OperationsContext ctx, int id) {
        super(ctx, "branch.false", id, 0);
        addPopSimple("condition");
        addBranchTarget("target");
        addBranchProfile("profile");
    }

    @Override
    public boolean isBranchInstruction() {
        return true;
    }

    @Override
    public CodeTree createExecuteCode(ExecutionVariables vars) {
        CodeTreeBuilder b = CodeTreeBuilder.createBuilder();

        // TODO: we should do (un)boxing elim here (but only if booleans are boxing elim'd)
        b.declaration("boolean", "cond", "UFA.unsafeGetObject(" + vars.stackFrame.getName() + ", $sp - 1) == Boolean.TRUE");

        b.startAssign(vars.sp).variable(vars.sp).string(" - 1").end();

        b.startIf().startCall("do_profileCondition");

        b.string("$this");
        b.string("cond");
        b.string("$conditionProfiles");
        b.tree(createBranchProfileIndex(vars, 0, false));

        b.end(2).startBlock();

        b.startAssign(vars.bci).variable(vars.bci).string(" + ").tree(createLength()).end();
        b.tree(OperationGeneratorUtils.encodeExecuteReturn());
        b.end().startElseBlock();
        b.startAssign(vars.bci).tree(createBranchTargetIndex(vars, 0, false)).end();
        b.tree(OperationGeneratorUtils.encodeExecuteReturn());

        b.end();

        return b.build();
    }

    @Override
    public CodeTree createPrepareAOT(ExecutionVariables vars, CodeTree language, CodeTree root) {
        return null;
    }
}
