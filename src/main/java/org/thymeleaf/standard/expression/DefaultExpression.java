/*
 * =============================================================================
 * 
 *   Copyright (c) 2011-2012, The THYMELEAF team (http://www.thymeleaf.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.thymeleaf.standard.expression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IProcessingContext;
import org.thymeleaf.util.StringUtils;
import org.thymeleaf.util.Validate;



/**
 * 
 * @author Daniel Fern&aacute;ndez
 * 
 * @since 1.1
 *
 */
public final class DefaultExpression extends ComplexExpression {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultExpression.class);


    private static final long serialVersionUID = 1830867943963082362L;


    private static final String OPERATOR = "?:";
    // Future proof, just in case in the future we add other tokens as operators
    static final String[] OPERATORS = new String[] {String.valueOf(OPERATOR)};

    
    private final Expression queriedExpression;
    private final Expression defaultExpression;

    
    public DefaultExpression(final Expression queriedExpression, final Expression defaultExpression) {
        super();
        Validate.notNull(queriedExpression, "Queried expression cannot be null");
        Validate.notNull(defaultExpression, "Default expression cannot be null");
        this.queriedExpression = queriedExpression;
        this.defaultExpression = defaultExpression;
    }
    
    public Expression getQueriedExpression() {
        return this.queriedExpression;
    }

    public Expression getDefaultExpression() {
        return this.defaultExpression;
    }

    
    @Override
    public String getStringRepresentation() {
        final StringBuilder sb = new StringBuilder();
        if (this.queriedExpression instanceof ComplexExpression) {
            sb.append(Expression.NESTING_START_CHAR);
            sb.append(this.queriedExpression);
            sb.append(Expression.NESTING_END_CHAR);
        } else {
            sb.append(this.queriedExpression);
        }
        sb.append(' ');
        sb.append(OPERATOR);
        sb.append(' ');
        if (this.defaultExpression instanceof ComplexExpression) {
            sb.append(Expression.NESTING_START_CHAR);
            sb.append(this.defaultExpression);
            sb.append(Expression.NESTING_END_CHAR);
        } else {
            sb.append(this.defaultExpression);
        }
        return sb.toString();
    }
    
    
    
    
    static ExpressionParsingState composeDefaultExpression(
            final ExpressionParsingState state, int nodeIndex) {

        // Returning "state" means "try next in chain" or "success"
        // Returning "null" means parsing error

        final String input = state.get(nodeIndex).getInput();

        if (StringUtils.isEmptyOrWhitespace(input)) {
            return null;
        }

        // Trying to fail quickly...
        int defaultOperatorPos = input.indexOf(OPERATOR);
        if (defaultOperatorPos == -1) {
            return state;
        }
        
        final String queriedStr = input.substring(0, defaultOperatorPos);
        final String defaultStr = input.substring(defaultOperatorPos + 2);
        
        if (defaultStr.contains(OPERATOR)) {
            // There are two "?:" operators
            return null;
        }


        final Expression queriedExpr = ExpressionParsingUtil.parseAndCompose(state, queriedStr);
        if (queriedExpr == null) {
            return null;
        }

        final Expression defaultExpr = ExpressionParsingUtil.parseAndCompose(state, defaultStr);
        if (defaultExpr == null) {
            return null;
        }


        final DefaultExpression defaultExpressionResult = new DefaultExpression(queriedExpr, defaultExpr);
        state.setNode(nodeIndex, defaultExpressionResult);
        
        return state;
        
    }
    

    
    
    static Object executeDefault(final Configuration configuration, final IProcessingContext processingContext, 
            final DefaultExpression expression, final IStandardVariableExpressionEvaluator expressionEvaluator,
            final StandardExpressionExecutionContext expContext) {

        if (logger.isTraceEnabled()) {
            logger.trace("[THYMELEAF][{}] Evaluating default expression: \"{}\"", TemplateEngine.threadIndex(), expression.getStringRepresentation());
        }
        
        final Object queriedValue = 
            Expression.execute(configuration, processingContext, expression.getQueriedExpression(), expressionEvaluator, expContext);
        
        if (queriedValue == null) {
            return Expression.execute(configuration, processingContext, expression.getDefaultExpression(), expressionEvaluator, expContext);
        }
        return queriedValue;
        
    }

    
}
