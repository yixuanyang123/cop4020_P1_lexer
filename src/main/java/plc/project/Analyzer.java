package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        boolean mainExists = false;
        for (Ast.Function function : ast.getFunctions()) {
            if ("main".equals(function.getName()) && function.getParameters().isEmpty()) {
                mainExists = true;
                break;
            }
        }
        if (!mainExists) {
            throw new RuntimeException("A main() function with zero parameters does not exist.");
        }
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        return null;
    }


    @Override
    public Void visit(Ast.Global ast) {
        Environment.Type declaredType = Environment.getType(ast.getTypeName());
        if (ast.getValue().isPresent()) {
            Ast.Expression expression = ast.getValue().get();
            visit(expression);
            if (expression instanceof Ast.Expression.PlcList) {
                Ast.Expression.PlcList list = (Ast.Expression.PlcList) expression;
                for (Ast.Expression listElement : list.getValues()) {
                    visit(listElement);
                    if (!declaredType.equals(listElement.getType())) {
                        throw new RuntimeException("List element type mismatch: expected " + declaredType.getName() + ", but found " + listElement.getType().getName());
                    }
                }
            } else {
                if (!declaredType.equals(expression.getType())) {
                    throw new RuntimeException("Global variable type mismatch: declared type is " + declaredType.getName() + ", but found " + expression.getType().getName());
                }
            }
        }
        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), declaredType, ast.getMutable(), Environment.NIL);
        ast.setVariable(variable);
        return null;
    }


    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> parameterTypes = ast.getParameterTypeNames().stream()
                .map(Environment::getType)
                .collect(Collectors.toList());
        Environment.Type returnType = ast.getReturnTypeName()
                .map(Environment::getType)
                .orElse(Environment.Type.NIL);
        if ("main".equals(ast.getName()) && ast.getParameters().isEmpty()) {
            if (!returnType.equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("The main() function must have an Integer return type.");
            }
        }
        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL);
        Scope functionScope = new Scope(scope);
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String parameter = ast.getParameters().get(i);
            Environment.Type parameterType = parameterTypes.get(i);
            functionScope.defineVariable(parameter, parameter, parameterType, true, Environment.NIL);
        }
        Environment.Variable returnVariable = functionScope.defineVariable("__RETURN__", "__RETURN__", returnType, true, Environment.NIL);

        Scope originalScope = scope;
        scope = functionScope;
        try {
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
                if (statement instanceof Ast.Statement.Return) {
                    Ast.Statement.Return returnStatement = (Ast.Statement.Return) statement;
                    if (returnStatement.getValue() != null && !returnType.equals(returnStatement.getValue().getType())) {
                        throw new RuntimeException("Return value type does not match function's declared return type.");
                    }
                }
            }
        } finally {
            scope = originalScope;
        }
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("The expression is not a function call, which is required for an expression statement.");
        }
        visit((Ast.Expression.Function) ast.getExpression());
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type;
        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        } else if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
            if (type == null) {
                throw new RuntimeException("Type of the declaration value could not be determined.");
            }
        } else {
            throw new RuntimeException("Declaration must have a type or an initial value.");
        }
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.Access) {
            Ast.Expression.Access access = (Ast.Expression.Access) ast.getValue().get();
            if (access.getName().equals(ast.getName())) {
                throw new RuntimeException("Recursive reference: Variable '" + ast.getName() + "' cannot be initialized with itself.");
            }
        }
        Environment.Variable variable = scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        if (ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.Literal) {
            ((Ast.Expression.Literal) ast.getValue().get()).setType(type);
            if (!isTypeCompatible(type, ((Ast.Expression.Literal) ast.getValue().get()).getLiteral())) {
                throw new RuntimeException("Type mismatch: cannot assign " + ((Ast.Expression.Literal) ast.getValue().get()).getLiteral() + " to " + type.getName());
            }
            variable.setValue(Environment.create(((Ast.Expression.Literal) ast.getValue().get()).getLiteral()));
        }
        ast.setVariable(variable);
        return null;
    }

    private boolean isTypeCompatible(Environment.Type type, Object literal) {
        switch (type.getName()) {
            case "Integer":
                return literal instanceof BigInteger;
            case "Decimal":
                return literal instanceof BigDecimal;
            case "String":
                return literal instanceof String;
            case "Boolean":
                return literal instanceof Boolean;
            case "Character":
                return literal instanceof Character;
            default:
                return false;
        }
    }


    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("The receiver is not an access expression.");
        }
        Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
        Environment.Variable variable = scope.lookupVariable(access.getName());
        if (variable == null) {
            throw new RuntimeException("Variable '" + access.getName() + "' not found.");
        }
        access.setVariable(variable);
        visit(ast.getValue());
        if (ast.getValue() instanceof Ast.Expression.Literal) {
            Ast.Expression.Literal literal = (Ast.Expression.Literal) ast.getValue();
            Object value = literal.getLiteral();
            if (value instanceof Boolean) {
                literal.setType(Environment.Type.BOOLEAN);
            } else if (value instanceof Integer || value instanceof BigInteger) {
                literal.setType(Environment.Type.INTEGER);
            } else if (value instanceof BigDecimal) {
                literal.setType(Environment.Type.DECIMAL);
            } else if (value instanceof String) {
                literal.setType(Environment.Type.STRING);
            } else if (value instanceof Character) {
                literal.setType(Environment.Type.CHARACTER);
            } else {
                throw new RuntimeException("Unsupported type for literal value.");
            }
        }
        requireAssignable(variable.getType(), ast.getValue().getType());
        return null;
    }


    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            throw new RuntimeException("Condition of if statement must be of type BOOLEAN.");
        }
        if (ast.getThenStatements().isEmpty() && ast.getElseStatements().isEmpty()) {
            throw new RuntimeException("If statement must have at least one 'then' or 'else' statement.");
        }
        Scope originalScope = scope;
        scope = new Scope(scope);
        try {
            for (Ast.Statement statement : ast.getThenStatements()) {
                visit(statement);
            }
        } finally {
            scope = originalScope;
        }
        if (!ast.getElseStatements().isEmpty()) {
            scope = new Scope(scope);
            try {
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            } finally {
                scope = originalScope;
            }
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        if (!(ast.getCondition().getType().equals(Environment.Type.CHARACTER) ||
                ast.getCondition().getType().equals(Environment.Type.INTEGER) ||
                ast.getCondition().getType().equals(Environment.Type.STRING))) {
            throw new RuntimeException("Switch condition must be of type CHARACTER, INTEGER, or STRING.");
        }
        boolean defaultCaseEncountered = false;

        for (Ast.Statement.Case caseStatement : ast.getCases()) {
            if (!caseStatement.getValue().isPresent()) {
                if (defaultCaseEncountered) {
                    throw new RuntimeException("Multiple default cases encountered in switch statement.");
                }
                defaultCaseEncountered = true;
            } else {
                visit(caseStatement.getValue().get());
                if (!caseStatement.getValue().get().getType().equals(ast.getCondition().getType())) {
                    throw new RuntimeException("Case value type does not match the type of the switch condition.");
                }
            }
            Scope originalScope = scope;
            scope = new Scope(scope);
            try {
                for (Ast.Statement statement : caseStatement.getStatements()) {
                    visit(statement);
                }
            } finally {
                scope = originalScope;
            }
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Case ast) {
        Scope caseScope = new Scope(scope);
        scope = caseScope;
        try {
            for (Ast.Statement statement : ast.getStatements()) {
                if (statement instanceof Ast.Statement.Expression) {
                    visit((Ast.Statement.Expression) statement);
                } else if (statement instanceof Ast.Statement.Declaration) {
                    visit((Ast.Statement.Declaration) statement);
                } else if (statement instanceof Ast.Statement.Assignment) {
                    visit((Ast.Statement.Assignment) statement);
                } else if (statement instanceof Ast.Statement.If) {
                    visit((Ast.Statement.If) statement);
                } else if (statement instanceof Ast.Statement.Switch) {
                    visit((Ast.Statement.Switch) statement);
                } else if (statement instanceof Ast.Statement.While) {
                    visit((Ast.Statement.While) statement);
                } else if (statement instanceof Ast.Statement.Return) {
                    visit((Ast.Statement.Return) statement);
                }
            }
        } finally {
            scope = caseScope.getParent();
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            throw new RuntimeException("While condition must be BOOLEAN.");
        }
        Scope originalScope = scope;
        scope = new Scope(scope);
        try {
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        } finally {
            scope = originalScope;
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        Environment.Variable returnVariable = scope.lookupVariable("__RETURN__");
        Environment.Type expectedReturnType = returnVariable.getType();
        Environment.Type returnType = ast.getValue().getType();
        if (!expectedReturnType.equals(returnType)) {
            throw new RuntimeException("Return value type " + returnType + " does not match expected type " + expectedReturnType);
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object value = ast.getLiteral();
        if (value == null) {
            ast.setType(Environment.Type.NIL);
        } else if (value instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (value instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (value instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (value instanceof BigInteger) {
            BigInteger bigInteger = (BigInteger) value;
            try {
                bigInteger.intValueExact();
                ast.setType(Environment.Type.INTEGER);
            } catch (ArithmeticException e) {
                throw new RuntimeException("The integer literal is out of range: " + bigInteger);
            }
        } else if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            double doubleValue = bigDecimal.doubleValue();
            if (doubleValue == Double.POSITIVE_INFINITY || doubleValue == Double.NEGATIVE_INFINITY) {
                throw new RuntimeException("The decimal literal is out of range: " + bigDecimal);
            }
            ast.setType(Environment.Type.DECIMAL);
        } else {
            throw new UnsupportedOperationException("Unsupported type of literal value.");
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Grouped expression must be binary.");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        switch (ast.getOperator()) {
            case "&&":
            case "||":
                if (!leftType.equals(Environment.Type.BOOLEAN) || !rightType.equals(Environment.Type.BOOLEAN)) {
                    throw new RuntimeException("Operands for logical operators must be of type BOOLEAN.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<":
            case ">":
            case "==":
            case "!=":
                if (!(leftType.equals(rightType) && (leftType.equals(Environment.Type.INTEGER) || leftType.equals(Environment.Type.DECIMAL) || leftType.equals(Environment.Type.STRING)))) {
                    throw new RuntimeException("Operands for comparison operators must be of the same type and either INTEGER, DECIMAL, or STRING.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                } else if ((leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) || (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL))) {
                    ast.setType(leftType);
                } else {
                    throw new RuntimeException("Operands for '+' operator must both be arithmetic types or one operand must be a STRING.");
                }
                break;
            case "-":
            case "*":
            case "/":
                if ((leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) || (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL))) {
                    ast.setType(leftType);
                } else {
                    throw new RuntimeException("Operands for '-' or '*' or '/' operators must be of type INTEGER or DECIMAL and must match.");
                }
                break;
            case "^":
                if (!leftType.equals(Environment.Type.INTEGER) || !rightType.equals(Environment.Type.INTEGER)) {
                    throw new RuntimeException("Operands for '^' operator must be of type INTEGER.");
                }
                ast.setType(Environment.Type.INTEGER);
                break;
            default:
                throw new RuntimeException("Unsupported operator: " + ast.getOperator());
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (function != null && ast.getName().equals(function.getName()) && !ast.getOffset().isPresent()) {
            throw new RuntimeException("Variable '" + ast.getName() + "' cannot be initialized with itself");
        }
        Environment.Variable variable = scope.lookupVariable(ast.getName());
        if (variable == null) {
            throw new RuntimeException("Variable '" + ast.getName() + "' not found.");
        } else {
            ast.setVariable(variable);
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        if (function == null) {
            throw new RuntimeException("Function '" + ast.getName() + "' with " + ast.getArguments().size() + " arguments is not defined.");
        } else {
            ast.setFunction(function);
        }
        for (int i = 0; i < ast.getArguments().size(); i++) {
            Ast.Expression arg = ast.getArguments().get(i);
            visit(arg);
            requireAssignable(function.getParameterTypes().get(i), arg.getType());
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        if (ast.getValues().isEmpty()) {
            return null;
        }
        visit(ast.getValues().get(0));
        Environment.Type expectedType = ast.getValues().get(0).getType();
        for (Ast.Expression expression : ast.getValues()) {
            visit(expression);
            requireAssignable(expectedType, expression.getType());
        }
        ast.setType(expectedType);

        return null;
    }


    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (!target.equals(type)) {
            Scope targetScope = target.getScope();
            Scope typeScope = type.getScope();
            boolean isAssignable = false;

            while (typeScope != null) {
                if (typeScope.equals(targetScope)) {
                    isAssignable = true;
                    break;
                }
                typeScope = typeScope.getParent();
            }
            if (!isAssignable) {
                throw new RuntimeException("Type " + type.getName() + " is not assignable to " + target.getName());
            }
        }
    }
}