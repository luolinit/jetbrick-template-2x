/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 *   Author: Guoqiang Chen
 *    Email: subchen@gmail.com
 *   WebURL: https://github.com/subchen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.template.parser.ast;

import jetbrick.template.Errors;
import jetbrick.template.runtime.InterpretContext;
import jetbrick.template.runtime.InterpretException;

/**
 *  <h2>Binary Operator</h2>
 *  <p>
 *  {@link http://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.6.2}<br/>
 *  <ul>
 *    <li>The multiplicative operators *, / and % </li>
 *    <li>The addition and subtraction operators for numeric types + and - </li>
 *    <li>The numerical comparison operators &lt;, &lt;=, &gt;, and &gt;= </li>
 *    <li>The numerical equality operators == and != </li>
 *    <li>The integer bitwise operators &, ^, and | </li>
 *    <li>In certain cases, the conditional operator ? : </li>
 *  </ul>
 *  </p>
 *
 *  <h2>String "+" Operator</h2>
 *  <p>
 *  {@link http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.18.1}<br/>
 *  </p>
 */
public final class AstOperatorBinary extends AstExpression {
    private final int operator;
    private final AstExpression lhs;
    private final AstExpression rhs;

    public AstOperatorBinary(int operator, AstExpression lhs, AstExpression rhs, Position position) {
        super(position);
        this.operator = operator;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public Object execute(InterpretContext ctx) throws InterpretException {
        Object o1 = lhs.execute(ctx);
        Object o2 = rhs.execute(ctx);
        if (o1 == null) {
            if (o2 instanceof String) {
                return o2;
            }
            throw new InterpretException(Errors.OP_LHS_IS_NULL).set(lhs.getPosition());
        }
        if (o2 == null) {
            if (o1 instanceof String) {
                return o1;
            }
            throw new InterpretException(Errors.OP_RHS_IS_NULL).set(lhs.getPosition());
        }

        try {
            Object value;
            switch (operator) {
            case Tokens.PLUS:
                value = ALU.plus(o1, o2);
                break;
            case Tokens.MINUS:
                value = ALU.minus(o1, o2);
                break;
            case Tokens.MUL:
                value = ALU.mul(o1, o2);
                break;
            case Tokens.DIV:
                value = ALU.div(o1, o2);
                break;
            case Tokens.MOD:
                value = ALU.mod(o1, o2);
                break;
            case Tokens.BIT_AND:
                value = ALU.bitAnd(o1, o2);
                break;
            case Tokens.BIT_OR:
                value = ALU.bitOr(o1, o2);
                break;
            case Tokens.BIT_XOR:
                value = ALU.bitXor(o1, o2);
                break;
            case Tokens.BIT_SHL:
                value = ALU.shl(o1, o2);
                break;
            case Tokens.BIT_SHR:
                value = ALU.shr(o1, o2);
                break;
            case Tokens.BIT_USHR:
                value = ALU.ushr(o1, o2);
                break;
            case Tokens.LT:
                value = ALU.lt(o1, o2);
                break;
            case Tokens.LE:
                value = ALU.le(o1, o2);
                break;
            case Tokens.GT:
                value = ALU.gt(o1, o2);
                break;
            case Tokens.GE:
                value = ALU.ge(o1, o2);
                break;
            default:
                throw new UnsupportedOperationException();
            }

            return value;

        } catch (InterpretException e) {
            throw e.set(position);
        } catch (IllegalStateException e) {
            throw new InterpretException(e).set(position);
        }
    }

}
