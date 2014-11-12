/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.marmotta.kiwi.sparql.builder.eval;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.marmotta.commons.util.DateUtils;
import org.apache.marmotta.kiwi.model.rdf.KiWiNode;
import org.apache.marmotta.kiwi.sparql.builder.SQLBuilder;
import org.apache.marmotta.kiwi.sparql.builder.ValueType;
import org.apache.marmotta.kiwi.sparql.builder.collect.OPTypeFinder;
import org.apache.marmotta.kiwi.sparql.builder.model.SQLVariable;
import org.apache.marmotta.kiwi.sparql.function.NativeFunction;
import org.apache.marmotta.kiwi.sparql.function.NativeFunctionRegistry;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.FN;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.algebra.*;
import org.openrdf.query.algebra.helpers.QueryModelVisitorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Evaluate a SPARQL ValueExpr by translating it into a SQL expression.
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
public class ValueExpressionEvaluator extends QueryModelVisitorBase<RuntimeException> {

    private static Logger log = LoggerFactory.getLogger(ValueExpressionEvaluator.class);

    /**
     * Date format used for SQL timestamps.
     */
    private static final DateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    /**
     * Reference to the registry of natively supported functions with parameter and return types as well as SQL translation
     */
    private static NativeFunctionRegistry functionRegistry = NativeFunctionRegistry.getInstance();

    // used by BNodeGenerator
    private static Random anonIdGenerator = new Random();


    private StringBuilder builder = new StringBuilder();

    private Deque<ValueType> optypes = new ArrayDeque<>();

    private SQLBuilder parent;

    public ValueExpressionEvaluator(ValueExpr expr, SQLBuilder parent) {
        this(expr,parent, ValueType.NODE);
    }

    public ValueExpressionEvaluator(ValueExpr expr, SQLBuilder parent, ValueType optype) {
        this.parent = parent;

        optypes.push(optype);

        if(log.isTraceEnabled()) {
            long start = System.currentTimeMillis();
            expr.visit(this);
            log.trace("expression evaluated in {} ms", (System.currentTimeMillis()-start));
        } else {
            expr.visit(this);
        }
    }


    /**
     * Create the actual SQL string generated by this evaluator.
     *
     * @return
     */
    public String build() {
        return builder.toString();
    }

    @Override
    public void meet(And node) throws RuntimeException {
        builder.append("(");
        node.getLeftArg().visit(this);
        builder.append(" AND ");
        node.getRightArg().visit(this);
        builder.append(")");
    }

    @Override
    public void meet(Or node) throws RuntimeException {
        builder.append("(");
        node.getLeftArg().visit(this);
        builder.append(" OR ");
        node.getRightArg().visit(this);
        builder.append(")");
    }

    @Override
    public void meet(Not node) throws RuntimeException {
        builder.append("NOT (");
        node.getArg().visit(this);
        builder.append(")");
    }

    @Override
    public void meet(Exists node) throws RuntimeException {
        // TODO: need to make sure that variables of the parent are visible in the subquery
        //       - pattern names need to be unique even in subqueries
        //       - variable lookup for expressions in the subquery need to refer to the parent
        SQLBuilder sq_builder = new SQLBuilder(node.getSubQuery(), parent.getBindings(), parent.getDataset(), parent.getConverter(), parent.getDialect(), "_", Collections.EMPTY_SET, copyVariables(parent.getVariables()));

        builder.append("EXISTS (").append(sq_builder.build()).append(")");
    }

    @Override
    public void meet(FunctionCall fc) throws RuntimeException {
        // special optimizations for frequent cases with variables
        if((XMLSchema.DOUBLE.toString().equals(fc.getURI()) || XMLSchema.FLOAT.toString().equals(fc.getURI()) ) && fc.getArgs().size() == 1) {
            optypes.push(ValueType.DOUBLE);
            fc.getArgs().get(0).visit(this);
            optypes.pop();
        } else if((XMLSchema.INTEGER.toString().equals(fc.getURI()) || XMLSchema.INT.toString().equals(fc.getURI())) && fc.getArgs().size() == 1) {
            optypes.push(ValueType.INT);
            fc.getArgs().get(0).visit(this);
            optypes.pop();
        } else if(XMLSchema.BOOLEAN.toString().equals(fc.getURI()) && fc.getArgs().size() == 1) {
            optypes.push(ValueType.BOOL);
            fc.getArgs().get(0).visit(this);
            optypes.pop();
        } else if(XMLSchema.DATE.toString().equals(fc.getURI()) && fc.getArgs().size() == 1) {
            optypes.push(ValueType.DATE);
            fc.getArgs().get(0).visit(this);
            optypes.pop();
        } else {

            String fnUri = fc.getURI();

            String[] args = new String[fc.getArgs().size()];

            NativeFunction nf = functionRegistry.get(fnUri);

            if (nf != null && nf.isSupported(parent.getDialect())) {

                for (int i = 0; i < args.length; i++) {
                    args[i] = new ValueExpressionEvaluator(fc.getArgs().get(i), parent, nf.getArgumentType(i)).build();
                }

                if (optypes.peek() != nf.getReturnType()) {
                    builder.append(castExpression(nf.getNative(parent.getDialect(), args), optypes.peek()));
                } else {
                    builder.append(nf.getNative(parent.getDialect(), args));
                }
            } else {
                throw new IllegalArgumentException("the function " + fnUri + " is not supported by the SQL translation");
            }
        }

    }

    @Override
    public void meet(Avg node) throws RuntimeException {
        builder.append("AVG(");
        optypes.push(ValueType.DOUBLE);
        node.getArg().visit(this);
        optypes.pop();
        builder.append(")");
    }

    @Override
    public void meet(BNodeGenerator gen) throws RuntimeException {
        if(gen.getNodeIdExpr() != null) {
            // get value of argument and express it as string
            optypes.push(ValueType.STRING);
            gen.getNodeIdExpr().visit(this);
            optypes.pop();
        } else {
            builder.append("'").append(Long.toHexString(System.currentTimeMillis())+Integer.toHexString(anonIdGenerator.nextInt(1000))).append("'");
        }
    }

    @Override
    public void meet(Bound node) throws RuntimeException {
        ValueExpr arg = node.getArg();

        if(arg instanceof ValueConstant) {
            builder.append(Boolean.toString(true));
        } else if(arg instanceof Var) {
            builder.append("(");
            optypes.push(ValueType.NODE);
            arg.visit(this);
            optypes.pop();
            builder.append(" IS NOT NULL)");
        }
    }

    @Override
    public void meet(Coalesce node) throws RuntimeException {
        builder.append("COALESCE(");
        for(Iterator<ValueExpr> it = node.getArguments().iterator(); it.hasNext(); ) {
            it.next().visit(this);
            if(it.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append(")");
    }

    @Override
    public void meet(Compare cmp) throws RuntimeException {
        optypes.push(new OPTypeFinder(cmp).coerce());
        cmp.getLeftArg().visit(this);
        builder.append(getSQLOperator(cmp.getOperator()));
        cmp.getRightArg().visit(this);
        optypes.pop();
    }

    @Override
    public void meet(Count node) throws RuntimeException {
        builder.append("COUNT(");

        if(node.isDistinct()) {
            builder.append("DISTINCT ");
        }

        if(node.getArg() == null) {
            // this is a weird special case where we need to expand to all variables selected in the query wrapped
            // by the group; we cannot simply use "*" because the concept of variables is a different one in SQL,
            // so instead we construct an ARRAY of the bindings of all variables

            List<String> countVariables = new ArrayList<>();
            for(SQLVariable v : parent.getVariables().values()) {
                if(v.getProjectionType() == ValueType.NONE) {
                    Preconditions.checkState(v.getExpressions().size() > 0, "no expressions available for variable");

                    countVariables.add(v.getExpressions().get(0));
                }
            }
            builder.append("ARRAY[");
            Joiner.on(',').appendTo(builder, countVariables);
            builder.append("]");

        } else {
            optypes.push(ValueType.NODE);
            node.getArg().visit(this);
            optypes.pop();
        }
        builder.append(")");
    }


    @Override
    public void meet(GroupConcat node) throws RuntimeException {
        if(node.getSeparator() == null) {
            builder.append(parent.getDialect().getGroupConcat(new ValueExpressionEvaluator(node.getArg(), parent, ValueType.STRING).build(), null, node.isDistinct()));
        } else {
            builder.append(parent.getDialect().getGroupConcat(
                    new ValueExpressionEvaluator(node.getArg(), parent, ValueType.STRING).build(),
                    new ValueExpressionEvaluator(node.getSeparator(), parent, ValueType.STRING).build(),
                    node.isDistinct()
            ));
        }
    }


    @Override
    public void meet(If node) throws RuntimeException {
        builder.append("CASE WHEN ");

        optypes.push(ValueType.BOOL);
        node.getCondition().visit(this);
        optypes.pop();

        optypes.push(new OPTypeFinder(node).coerce());
        builder.append(" THEN ");
        node.getResult().visit(this);
        builder.append(" ELSE ");
        node.getAlternative().visit(this);
        builder.append(" END");
        optypes.pop();
    }


    @Override
    public void meet(IsBNode node) throws RuntimeException {
        ValueExpr arg = node.getArg();

        // operator must be a variable or a constant
        if(arg instanceof ValueConstant) {
            builder.append(Boolean.toString(((ValueConstant) arg).getValue() instanceof BNode));
        } else if(arg instanceof Var) {
            String var = getVariableAlias((Var) arg);

            builder.append(var).append(".ntype = 'bnode'");
        }
    }

    @Override
    public void meet(IsLiteral node) throws RuntimeException {
        ValueExpr arg = node.getArg();

        // operator must be a variable or a constant
        if (arg instanceof ValueConstant) {
            builder.append(Boolean.toString(((ValueConstant) arg).getValue() instanceof Literal));
        } else if(arg instanceof Var) {
            String var = getVariableAlias((Var) arg);

            Preconditions.checkState(var != null, "no alias available for variable");

            builder.append("(")
                    .append(var)
                    .append(".ntype = 'string' OR ")
                    .append(var)
                    .append(".ntype = 'int' OR ")
                    .append(var)
                    .append(".ntype = 'double' OR ")
                    .append(var)
                    .append(".ntype = 'date' OR ")
                    .append(var)
                    .append(".ntype = 'boolean')");
        }
    }

    @Override
    public void meet(IsNumeric node) throws RuntimeException {
        ValueExpr arg = node.getArg();

        // operator must be a variable or a constant
        if (arg instanceof ValueConstant) {
            try {
                Double.parseDouble(((ValueConstant) arg).getValue().stringValue());
                builder.append(Boolean.toString(true));
            } catch (NumberFormatException ex) {
                builder.append(Boolean.toString(false));
            }
        } else if(arg instanceof Var) {
            String var = getVariableAlias((Var) arg);

            Preconditions.checkState(var != null, "no alias available for variable");

            builder.append("(")
                    .append(var)
                    .append(".ntype = 'int' OR ")
                    .append(var)
                    .append(".ntype = 'double')");
        }
    }

    @Override
    public void meet(IsResource node) throws RuntimeException {
        ValueExpr arg = node.getArg();

        // operator must be a variable or a constant
        if(arg instanceof ValueConstant) {
            builder.append(Boolean.toString(((ValueConstant) arg).getValue() instanceof URI || ((ValueConstant) arg).getValue() instanceof BNode));
        } else if(arg instanceof Var) {
            String var = getVariableAlias((Var) arg);

            Preconditions.checkState(var != null, "no alias available for variable");

            builder .append("(")
                    .append(var)
                    .append(".ntype = 'uri' OR ")
                    .append(var)
                    .append(".ntype = 'bnode')");
        }
    }

    @Override
    public void meet(IsURI node) throws RuntimeException {
        ValueExpr arg = node.getArg();

        // operator must be a variable or a constant
        if(arg instanceof ValueConstant) {
            builder.append(Boolean.toString(((ValueConstant) arg).getValue() instanceof URI));
        } else if(arg instanceof Var) {
            String var = getVariableAlias((Var) arg);

            Preconditions.checkState(var != null, "no alias available for variable");

            builder.append(var).append(".ntype = 'uri'");
        }
    }

    @Override
    public void meet(IRIFunction fun) throws RuntimeException {
        if(fun.getBaseURI() != null) {

            String ex = new ValueExpressionEvaluator(fun.getArg(), parent, ValueType.STRING).build();

            builder
                    .append("CASE WHEN position(':' IN ").append(ex).append(") > 0 THEN ").append(ex)
                    .append(" ELSE ").append(functionRegistry.get(FN.CONCAT.stringValue()).getNative(parent.getDialect(), "'" + fun.getBaseURI() + "'", ex))
                    .append(" END ");
        } else {
            // get value of argument and express it as string
            optypes.push(ValueType.STRING);
            fun.getArg().visit(this);
            optypes.pop();
        }
    }

    @Override
    public void meet(Label node) throws RuntimeException {
        optypes.push(ValueType.STRING);
        node.getArg().visit(this);
        optypes.pop();
    }

    @Override
    public void meet(Lang lang) throws RuntimeException {
        if(lang.getArg() instanceof Var) {
            String var = getVariableAlias((Var) lang.getArg());
            Preconditions.checkState(var != null, "no alias available for variable");

            builder.append(var);
            builder.append(".lang");
        }
    }

    @Override
    public void meet(LangMatches lm) throws RuntimeException {
        ValueConstant pattern = (ValueConstant) lm.getRightArg();

        if(pattern.getValue().stringValue().equals("*")) {
            lm.getLeftArg().visit(this);
            builder.append(" LIKE '%'");
        } else if(pattern.getValue().stringValue().equals("")) {
            lm.getLeftArg().visit(this);
            builder.append(" IS NULL");
        } else {
            builder.append("(");
            lm.getLeftArg().visit(this);
            builder.append(" = '");
            builder.append(pattern.getValue().stringValue().toLowerCase());
            builder.append("' OR ");
            lm.getLeftArg().visit(this);
            builder.append(" LIKE '");
            builder.append(pattern.getValue().stringValue().toLowerCase());
            builder.append("-%' )");
        }
    }

    @Override
    public void meet(Like node) throws RuntimeException {
        if(node.isCaseSensitive()) {
            optypes.push(ValueType.STRING);
            node.getArg().visit(this);
            optypes.pop();

            builder.append(" LIKE ");
            node.getPattern();
        } else {
            builder.append(parent.getDialect().getILike(new ValueExpressionEvaluator(node.getArg(),parent, ValueType.STRING).build(), node.getOpPattern()));
        }

    }


    @Override
    public void meet(LocalName node) throws RuntimeException {
        super.meet(node);
    }

    @Override
    public void meet(MathExpr expr) throws RuntimeException {
        ValueType ot = new OPTypeFinder(expr).coerce();

        if(ot == ValueType.STRING) {
            if(expr.getOperator() == MathExpr.MathOp.PLUS) {
                builder.append(functionRegistry.get(FN.CONCAT.stringValue()).getNative(parent.getDialect(),new ValueExpressionEvaluator(expr.getLeftArg(), parent, ot).build(), new ValueExpressionEvaluator(expr.getRightArg(), parent, ot).build()));
            } else {
                throw new IllegalArgumentException("operation "+expr.getOperator()+" is not supported on strings");
            }
        } else {
            if(ot == ValueType.NODE || ot == ValueType.TERM) {
                ot = ValueType.DOUBLE;
            }

            optypes.push(ot);
            expr.getLeftArg().visit(this);
            builder.append(getSQLOperator(expr.getOperator()));
            expr.getRightArg().visit(this);
            optypes.pop();
        }
    }

    @Override
    public void meet(Max node) throws RuntimeException {
        builder.append("MAX(");
        optypes.push(ValueType.DOUBLE);
        node.getArg().visit(this);
        optypes.pop();
        builder.append(")");
    }

    @Override
    public void meet(Min node) throws RuntimeException {
        builder.append("MIN(");
        optypes.push(ValueType.DOUBLE);
        node.getArg().visit(this);
        optypes.pop();
        builder.append(")");
    }

    @Override
    public void meet(Regex re) throws RuntimeException {
        builder.append(optimizeRegexp(
                new ValueExpressionEvaluator(re.getArg(), parent, ValueType.STRING).build(),
                new ValueExpressionEvaluator(re.getPatternArg(), parent, ValueType.STRING).build(),
                re.getFlagsArg()
        ));
    }

    @Override
    public void meet(SameTerm cmp) throws RuntimeException {
        // covered by value binding in variables
        optypes.push(ValueType.TERM);
        cmp.getLeftArg().visit(this);
        builder.append(" = ");
        cmp.getRightArg().visit(this);
        optypes.pop();
    }

    @Override
    public void meet(Str node) throws RuntimeException {
        optypes.push(ValueType.STRING);
        node.getArg().visit(this);
        optypes.pop();
    }

    @Override
    public void meet(Sum node) throws RuntimeException {
        builder.append("SUM(");
        optypes.push(ValueType.DOUBLE);
        node.getArg().visit(this);
        optypes.pop();
        builder.append(")");
    }

    @Override
    public void meet(Var node) throws RuntimeException {
        // distinguish between the case where the variable is plain and the variable is bound
        SQLVariable sv = parent.getVariables().get(node.getName());

        if(sv == null) {
            builder.append("NULL");
        } else if(sv.getBindings().size() > 0) {
            // in case the variable is actually an alias for an expression, we evaluate that expression instead, effectively replacing the
            // variable occurrence with its value
            sv.getBindings().get(0).visit(this);
        } else {
            String var = sv.getAlias();

            if(sv.getProjectionType() != ValueType.NODE && sv.getProjectionType() != ValueType.NONE) {
                // in case the variable represents a constructed or bound value instead of a node, we need to
                // use the SQL expression as value; SQL should take care of proper casting...
                // TODO: explicit casting needed?
                builder.append(sv.getExpressions().get(0));
            } else {
                // in case the variable represents an entry from the NODES table (i.e. has been bound to a node
                // in the database, we take the NODES alias and resolve to the correct column according to the
                // operator type
                switch (optypes.peek()) {
                    case STRING:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(var).append(".svalue");
                        break;
                    case INT:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(var).append(".ivalue");
                        break;
                    case DECIMAL:
                    case DOUBLE:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(var).append(".dvalue");
                        break;
                    case BOOL:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(var).append(".bvalue");
                        break;
                    case DATE:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(var).append(".tvalue");
                        break;
                    case TZDATE:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(parent.getDialect().getDateTimeTZ(var));
                        break;
                    case URI:
                        Preconditions.checkState(var != null, "no alias available for variable");
                        builder.append(var).append(".svalue");
                        break;
                    case TERM:
                    case NODE:
                        if(sv.getExpressions().size() > 0) {
                            // this allows us to avoid joins with the nodes table for simple expressions that only need the ID
                            builder.append(sv.getExpressions().get(0));
                        } else {
                            Preconditions.checkState(var != null, "no alias available for variable");
                            builder.append(var).append(".id");
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void meet(ValueConstant node) throws RuntimeException {
        String val = node.getValue().stringValue();

            switch (optypes.peek()) {
                case STRING:
                case URI:
                    builder.append("'").append(val).append("'");
                    break;
                case INT:
                    builder.append(Integer.parseInt(val));
                    break;
                case DECIMAL:
                case DOUBLE:
                    builder.append(Double.parseDouble(val));
                    break;
                case BOOL:
                    builder.append(Boolean.parseBoolean(val));
                    break;
                case DATE:
                    builder.append("'").append(sqlDateFormat.format(DateUtils.parseDate(val))).append("'");
                    break;

                // in this case we should return a node ID and also need to make sure it actually exists
                case TERM:
                case NODE:
                    KiWiNode n = parent.getConverter().convert(node.getValue());
                    builder.append(n.getId());
                    break;

                default: throw new IllegalArgumentException("unsupported value type: " + optypes.peek());
            }
    }

    private String getVariableAlias(Var var) {
        return parent.getVariables().get(var.getName()).getAlias();
    }


    private String getVariableAlias(String varName) {
        return parent.getVariables().get(varName).getAlias();
    }

    /**
     * Copy variables from the set to a new set suitable for a subquery; this allows passing over variable expressions
     * from parent queries to subqueries without the subquery adding expressions that are then not visible outside
     * @param variables
     * @return
     */
    private static Map<String, SQLVariable> copyVariables(Map<String, SQLVariable> variables) {
        Map<String,SQLVariable> copy = new HashMap<>();
        try {
            for(Map.Entry<String,SQLVariable> entry : variables.entrySet()) {
                copy.put(entry.getKey(), (SQLVariable) entry.getValue().clone());
            }
        } catch (CloneNotSupportedException e) {
            log.error("could not clone SQL variable:",e);
        }

        return copy;
    }

    private String castExpression(String arg, ValueType type) {
        if(type == null) {
            return arg;
        }

        switch (type) {
            case DECIMAL:
                return functionRegistry.get(XMLSchema.DECIMAL).getNative(parent.getDialect(), arg);
            case DOUBLE:
                return functionRegistry.get(XMLSchema.DOUBLE).getNative(parent.getDialect(), arg);
            case INT:
                return functionRegistry.get(XMLSchema.INTEGER).getNative(parent.getDialect(), arg);
            case BOOL:
                return functionRegistry.get(XMLSchema.BOOLEAN).getNative(parent.getDialect(), arg);
            case DATE:
                return functionRegistry.get(XMLSchema.DATETIME).getNative(parent.getDialect(), arg);
            case STRING:
                return arg;
            case NODE:
                return arg;
            default:
                return arg;
        }
    }

    private static String getSQLOperator(Compare.CompareOp op) {
        switch (op) {
            case EQ: return " = ";
            case GE: return " >= ";
            case GT: return " > ";
            case LE: return " <= ";
            case LT: return " < ";
            case NE: return " <> ";
        }
        throw new IllegalArgumentException("unsupported operator type for comparison: "+op);
    }


    private static String getSQLOperator(MathExpr.MathOp op) {
        switch (op) {
            case PLUS: return " + ";
            case MINUS: return " - ";
            case DIVIDE: return " / ";
            case MULTIPLY: return " / ";
        }
        throw new IllegalArgumentException("unsupported operator type for math expression: "+op);
    }

    /**
     * Test if the regular expression given in the pattern can be simplified to a LIKE SQL statement; these are
     * considerably more efficient to evaluate in most databases, so in case we can simplify, we return a LIKE.
     *
     * @param value
     * @param pattern
     * @return
     */
    private String optimizeRegexp(String value, String pattern, ValueExpr flags) {
        String _flags = flags != null && flags instanceof ValueConstant ? ((ValueConstant)flags).getValue().stringValue() : null;

        String simplified = pattern;

        // apply simplifications

        // remove SQL quotes at beginning and end
        simplified = simplified.replaceFirst("^'","");
        simplified = simplified.replaceFirst("'$","");


        // remove .* at beginning and end, they are the default anyways
        simplified = simplified.replaceFirst("^\\.\\*","");
        simplified = simplified.replaceFirst("\\.\\*$","");

        // replace all occurrences of % with \% and _ with \_, as they are special characters in SQL
        simplified = simplified.replaceAll("%","\\%");
        simplified = simplified.replaceAll("_","\\_");

        // if pattern now does not start with a ^, we put a "%" in front
        if(!simplified.startsWith("^")) {
            simplified = "%" + simplified;
        } else {
            simplified = simplified.substring(1);
        }

        // if pattern does not end with a "$", we put a "%" at the end
        if(!simplified.endsWith("$")) {
            simplified = simplified + "%";
        } else {
            simplified = simplified.substring(0,simplified.length()-1);
        }

        // replace all non-escaped occurrences of .* with %
        simplified = simplified.replaceAll("(?<!\\\\)\\.\\*","%");

        // replace all non-escaped occurrences of .+ with _%
        simplified = simplified.replaceAll("(?<!\\\\)\\.\\+","_%");

        // the pattern is not simplifiable if the simplification still contains unescaped regular expression constructs
        Pattern notSimplifiable = Pattern.compile("(?<!\\\\)[\\.\\*\\+\\{\\}\\[\\]\\|]");

        if(notSimplifiable.matcher(simplified).find()) {
            return parent.getDialect().getRegexp(value, pattern, _flags);
        } else {
            if(!simplified.startsWith("%") && !simplified.endsWith("%")) {
                if(StringUtils.containsIgnoreCase(_flags, "i")) {
                    return String.format("lower(%s) = lower('%s')", value, simplified);
                } else {
                    return String.format("%s = '%s'", value, simplified);
                }
            } else {
                if(StringUtils.containsIgnoreCase(_flags,"i")) {
                    return parent.getDialect().getILike(value, "'" + simplified + "'");
                } else {
                    return value + " LIKE '"+simplified+"'";
                }
            }
        }

    }

}