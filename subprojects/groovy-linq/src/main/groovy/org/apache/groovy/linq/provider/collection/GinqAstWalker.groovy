/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.groovy.linq.provider.collection

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.groovy.linq.dsl.GinqSyntaxError
import org.apache.groovy.linq.dsl.GinqVisitor
import org.apache.groovy.linq.dsl.SyntaxErrorReportable
import org.apache.groovy.linq.dsl.expression.DataSourceExpression
import org.apache.groovy.linq.dsl.expression.FilterExpression
import org.apache.groovy.linq.dsl.expression.FromExpression
import org.apache.groovy.linq.dsl.expression.GinqExpression
import org.apache.groovy.linq.dsl.expression.JoinExpression
import org.apache.groovy.linq.dsl.expression.OnExpression
import org.apache.groovy.linq.dsl.expression.SelectExpression
import org.apache.groovy.linq.dsl.expression.SimpleGinqExpression
import org.apache.groovy.linq.dsl.expression.WhereExpression
import org.codehaus.groovy.GroovyBugError
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.CastExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.ExpressionTransformer
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.SourceUnit

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.lambdaX
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GeneralUtils.params
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt

/**
 * Visit AST of GINQ and generate target method calls for GINQ
 *
 * @since 4.0.0
 */
@CompileStatic
class GinqAstWalker implements GinqVisitor<Object>, SyntaxErrorReportable {
    private final SourceUnit sourceUnit

    GinqAstWalker(SourceUnit sourceUnit) {
        this.sourceUnit = sourceUnit
    }

    @Override
    MethodCallExpression visitSimpleGinqExpression(SimpleGinqExpression simpleGinqExpression) {
        FromExpression fromExpression = simpleGinqExpression.getFromExpression()
        MethodCallExpression fromMethodCallExpression = this.visitFromExpression(fromExpression)

        MethodCallExpression selectMethodReceiver = fromMethodCallExpression

        JoinExpression lastJoinExpression = null
        MethodCallExpression lastJoinMethodCallExpression = null
        for (JoinExpression joinExpression : simpleGinqExpression.getJoinExpressionList()) {
            joinExpression.putNodeMetaData(__METHOD_CALL_RECEIVER, lastJoinMethodCallExpression ?: fromMethodCallExpression)
            joinExpression.putNodeMetaData(__DATA_SOURCE_EXPRESSION, lastJoinExpression ?: fromExpression)

            lastJoinExpression = joinExpression
            lastJoinMethodCallExpression = this.visitJoinExpression(lastJoinExpression)
        }

        if (lastJoinMethodCallExpression) {
            selectMethodReceiver = lastJoinMethodCallExpression
        }

        SelectExpression selectExpression = simpleGinqExpression.getSelectExpression()
        selectExpression.putNodeMetaData(__METHOD_CALL_RECEIVER, selectMethodReceiver)
        selectExpression.putNodeMetaData(__DATA_SOURCE_EXPRESSION, lastJoinExpression ?: fromExpression)

        MethodCallExpression selectMethodCallExpression = this.visitSelectExpression(selectExpression)


        return selectMethodCallExpression
    }

    @Override
    MethodCallExpression visitFromExpression(FromExpression fromExpression) {
        MethodCallExpression fromMethodCallExpression = constructFromMethodCallExpression(fromExpression.dataSourceExpr)

        List<FilterExpression> filterExpressionList = fromExpression.getFilterExpressionList()
        if (filterExpressionList) {
            WhereExpression whereExpression = (WhereExpression) filterExpressionList.get(0)
            whereExpression.putNodeMetaData(__DATA_SOURCE_EXPRESSION, fromExpression)
            whereExpression.putNodeMetaData(__METHOD_CALL_RECEIVER, fromMethodCallExpression)

            return visitWhereExpression(whereExpression)
        }

        return fromMethodCallExpression
    }

    @Override
    MethodCallExpression visitJoinExpression(JoinExpression joinExpression) {
        Expression receiver = joinExpression.getNodeMetaData(__METHOD_CALL_RECEIVER)
        DataSourceExpression dataSourceExpression = joinExpression.getNodeMetaData(__DATA_SOURCE_EXPRESSION)
        Expression receiverAliasExpr = dataSourceExpression.aliasExpr
        List<FilterExpression> filterExpressionList = joinExpression.getFilterExpressionList()
        int filterExpressionListSize = filterExpressionList.size()

        if (0 == filterExpressionListSize && !joinExpression.crossJoin) {
            this.collectSyntaxError(
                    new GinqSyntaxError(
                            "`on` clause is expected for `" + joinExpression.joinName + "`",
                            joinExpression.getLineNumber(), joinExpression.getColumnNumber()
                    )
            )
        }

        OnExpression onExpression
        int whereExpressionPos
        if (joinExpression.isCrossJoin()) {
            onExpression = null
            whereExpressionPos = 0
        } else {
            onExpression = (OnExpression) filterExpressionList.get(0)
            whereExpressionPos = 1
        }
        WhereExpression whereExpression = filterExpressionList.size() < (whereExpressionPos + 1) ? null : (WhereExpression) filterExpressionList.get(whereExpressionPos)

        MethodCallExpression joinMethodCallExpression = constructJoinMethodCallExpression(receiver, receiverAliasExpr, joinExpression, onExpression, whereExpression)

        return joinMethodCallExpression
    }

    @Override
    MethodCallExpression visitOnExpression(OnExpression onExpression) {
        return null // do nothing
    }

    @CompileDynamic
    private MethodCallExpression constructFromMethodCallExpression(Expression dataSourceExpr) {
        MethodCallExpression fromMethodCallExpression = macro {
            $v{ makeQueryableCollectionClassExpression() }.from($v {
                if (dataSourceExpr instanceof SimpleGinqExpression) {
                    return this.visitSimpleGinqExpression((SimpleGinqExpression) dataSourceExpr)
                } else {
                    return dataSourceExpr
                }
            })
        }

        return fromMethodCallExpression
    }

    private MethodCallExpression constructJoinMethodCallExpression(
            Expression receiver, Expression receiverAliasExpr, JoinExpression joinExpression,
            OnExpression onExpression, WhereExpression whereExpression) {

        MethodCallExpression joinMethodCallExpression = callX(receiver, joinExpression.joinName,
                args(
                        constructFromMethodCallExpression(joinExpression.dataSourceExpr),
                        null == onExpression ? EmptyExpression.INSTANCE : lambdaX(
                                params(
                                        param(ClassHelper.DYNAMIC_TYPE, receiverAliasExpr.text),
                                        param(ClassHelper.DYNAMIC_TYPE, joinExpression.aliasExpr.text)
                                ),
                                stmt(onExpression.getFilterExpr())
                        )
                )
        )

        if (joinExpression.crossJoin) {
            // cross join does not need `on` clause
            Expression lastArgumentExpression = ((ArgumentListExpression) joinMethodCallExpression.arguments).getExpressions().removeLast()
            if (EmptyExpression.INSTANCE !== lastArgumentExpression) {
                throw new GroovyBugError("Wrong argument removed")
            }
        }

        if (whereExpression) {
            whereExpression.putNodeMetaData(__DATA_SOURCE_EXPRESSION, joinExpression)
            whereExpression.putNodeMetaData(__METHOD_CALL_RECEIVER, joinMethodCallExpression)
            return visitWhereExpression(whereExpression)
        }

        return joinMethodCallExpression
    }

    @Override
    MethodCallExpression visitWhereExpression(WhereExpression whereExpression) {
        DataSourceExpression dataSourceExpression = whereExpression.getNodeMetaData(__DATA_SOURCE_EXPRESSION)
        Expression fromMethodCallExpression = whereExpression.getNodeMetaData(__METHOD_CALL_RECEIVER)
        Expression filterExpr = whereExpression.getFilterExpr()

        return callXWithLambda(fromMethodCallExpression, "where", dataSourceExpression, filterExpr)
    }

    @Override
    MethodCallExpression visitSelectExpression(SelectExpression selectExpression) {
        Expression selectMethodReceiver = selectExpression.getNodeMetaData(__METHOD_CALL_RECEIVER)
        DataSourceExpression dataSourceExpression = selectExpression.getNodeMetaData(__DATA_SOURCE_EXPRESSION)
        Expression projectionExpr = selectExpression.getProjectionExpr()

        List<Expression> expressionList = ((TupleExpression) projectionExpr).getExpressions()
        Expression lambdaCode
        if (expressionList.size() > 1) {
            ConstructorCallExpression namedListCtorCallExpression = constructNamedListCtorCallExpression(expressionList)
            lambdaCode = namedListCtorCallExpression
        } else {
            lambdaCode = expressionList.get(0)
        }

        return callXWithLambda(selectMethodReceiver, "select", dataSourceExpression, lambdaCode)
    }

    private static ConstructorCallExpression constructNamedListCtorCallExpression(List<Expression> expressionList) {
        int expressionListSize = expressionList.size()
        List<Expression> elementExpressionList = new ArrayList<>(expressionListSize)
        List<Expression> nameExpressionList = new ArrayList<>(expressionListSize)
        for (Expression e : expressionList) {
            Expression elementExpression = e
            Expression nameExpression = new ConstantExpression(e.text)

            if (e instanceof CastExpression) {
                elementExpression = e.expression
                nameExpression = new ConstantExpression(e.type.text)
            } else if (e instanceof PropertyExpression) {
                if (e.property instanceof ConstantExpression) {
                    elementExpression = e
                    nameExpression = new ConstantExpression(e.property.text)
                } else if (e.property instanceof GStringExpression) {
                    elementExpression = e
                    nameExpression = e.property
                }
            }
            elementExpressionList << elementExpression
            nameExpressionList << nameExpression
        }

        ConstructorCallExpression namedListCtorCallExpression = GeneralUtils.ctorX(ClassHelper.make(NamedList.class), args(new ListExpression(elementExpressionList), new ListExpression(nameExpressionList)))
        namedListCtorCallExpression
    }

    private static Expression correctVariablesOfGinqExpression(JoinExpression joinExpression, Expression expr) {
        DataSourceExpression dataSourceExpression = joinExpression.getNodeMetaData(__DATA_SOURCE_EXPRESSION)
        final Expression firstAliasExpr = dataSourceExpression.aliasExpr
        final Expression secondAliasExpr = joinExpression.aliasExpr

        // The synthetic lambda parameter `__t` represents the element from the result datasource of joining, e.g. `n1` innerJoin `n2`
        // The element from first datasource(`n1`) is referenced via `_t.v1`
        // and the element from second datasource(`n2`) is referenced via `_t.v2`
        expr = expr.transformExpression(new ExpressionTransformer() {
            @Override
            Expression transform(Expression expression) {
                if (expression instanceof VariableExpression) {
                    Expression transformedExpression = null
                    if (firstAliasExpr.text == expression.text) {
                        // replace `n1` with `__t.v1`
                        transformedExpression = constructFirstAliasVariableAccess()
                    } else if (secondAliasExpr.text == expression.text) {
                        // replace `n2` with `__t.v2`
                        transformedExpression = constructSecondAliasVariableAccess()
                    }

                    if (null != transformedExpression) {
                        return transformedExpression
                    }
                }

                return expression.transformExpression(this)
            }
        })
        return expr
    }

    @Override
    Object visit(GinqExpression expression) {
        return expression.accept(this)
    }

    private static MethodCallExpression callXWithLambda(Expression receiver, String methodName, DataSourceExpression dataSourceExpression, Expression lambdaCode) {
        String lambdaParamName
        if (dataSourceExpression instanceof JoinExpression) {
            lambdaParamName = __T
            lambdaCode = correctVariablesOfGinqExpression((JoinExpression) dataSourceExpression, lambdaCode)
        } else {
            lambdaParamName = dataSourceExpression.aliasExpr.text
        }

        callXWithLambda(receiver, methodName, lambdaParamName, lambdaCode)
    }

    private static MethodCallExpression callXWithLambda(Expression receiver, String methodName, String lambdaParamName, Expression lambdaCode) {
        callX(
                receiver,
                methodName,
                lambdaX(
                        params(param(ClassHelper.DYNAMIC_TYPE, lambdaParamName)),
                        stmt(lambdaCode)
                )
        )
    }

    private static Expression constructFirstAliasVariableAccess() {
        constructAliasVariableAccess('v1')
    }

    private static Expression constructSecondAliasVariableAccess() {
        constructAliasVariableAccess('v2')
    }

    private static Expression constructAliasVariableAccess(String name) {
        propX(new VariableExpression(__T), name)
    }

    private static makeQueryableCollectionClassExpression() {
        new ClassExpression(ClassHelper.make(Queryable.class))
    }

    @Override
    SourceUnit getSourceUnit() {
        sourceUnit
    }

    private static final String __DATA_SOURCE_EXPRESSION = "__dataSourceExpression"
    private static final String __METHOD_CALL_RECEIVER = "__methodCallReceiver"
    private static final String __T = "__t"
}
