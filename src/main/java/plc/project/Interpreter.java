package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        return scope.lookupFunction("main", 0).invoke(List.of());
    }


    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        Environment.PlcObject value = ast.getValue().map(this::visit).orElse(Environment.NIL);
        scope.defineVariable(ast.getName(), ast.getMutable(), value);
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope previous = scope;
            try {
                scope = new Scope(previous);
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
                return Environment.NIL;
            } catch (Return returnValue) {
                return returnValue.value;
            } finally {
                scope = previous;
            }
        });
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        Environment.PlcObject value = ast.getValue().map(this::visit).orElse(Environment.NIL);
        scope.defineVariable(ast.getName(), true, value);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Environment.PlcObject value = visit(ast.getValue());
        if (ast.getReceiver() instanceof Ast.Expression.Access) {
            Ast.Expression.Access access = (Ast.Expression.Access) ast.getReceiver();
            if (access.getOffset().isPresent()) {
                Environment.PlcObject listPlcObject = scope.lookupVariable(access.getName()).getValue();
                List<Environment.PlcObject> list = requireType(List.class, listPlcObject);
                int index = requireType(BigInteger.class, visit(access.getOffset().get())).intValue();
                if (index >= 0 && index < list.size()) {
                    list.set(index, value);
                } else {
                    throw new RuntimeException("List index out of bounds");
                }
            } else {
                scope.lookupVariable(access.getName()).setValue(value);
            }
        } else {
            throw new RuntimeException("The receiver must be an Access expression.");
        }
        return Environment.NIL;
    }




    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getThenStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }
        } else if (!ast.getElseStatements().isEmpty()) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getElseStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Environment.PlcObject condition = visit(ast.getCondition());
        boolean matched = false;
        for (Ast.Statement.Case c : ast.getCases()) {
            if (!matched && (!c.getValue().isPresent() || Objects.equals(visit(c.getValue().get()), condition))) {
                try {
                    scope = new Scope(scope);
                    for (Ast.Statement statement : c.getStatements()) {
                        visit(statement);
                    }
                    matched = true; // Ensure default case or subsequent cases aren't executed once a match is found
                } finally {
                    scope = scope.getParent();
                }
            }
        }
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            Environment.PlcObject value = visit(ast.getValue().get());
        }
        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        return Environment.NIL; // Case statements do not return a value.
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }


    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        } else {
            return Environment.create(ast.getLiteral());
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());

        switch (ast.getOperator()) {
            case "&&":
                if (!requireType(Boolean.class, left)) {
                    return Environment.create(false);
                }
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));


            case "||":
                if (requireType(Boolean.class, left)) {
                    return Environment.create(true);
                }
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));

            case "<":
                if (left.getValue() instanceof Comparable && right.getValue().getClass().equals(left.getValue().getClass())) {
                    Comparable leftComparable = (Comparable) left.getValue();
                    Comparable rightComparable = (Comparable) right.getValue();
                    return Environment.create(leftComparable.compareTo(rightComparable) < 0);
                } else {
                    throw new RuntimeException("Invalid types for < operator.");
                }

            case ">":
                if (left.getValue() instanceof Comparable && right.getValue() instanceof Comparable) {
                    Comparable leftComp = requireType(Comparable.class, left);
                    Comparable rightComp = requireType(Comparable.class, right);
                    int comparison = leftComp.compareTo(rightComp);
                    if (ast.getOperator().equals("<")) {
                        return Environment.create(comparison < 0);
                    } else {
                        return Environment.create(comparison > 0);
                    }
                } else {
                    throw new RuntimeException("Invalid types for comparison operator.");
                }

            case "==":
                return Environment.create(Objects.equals(left.getValue(), right.getValue()));

            case "!=":
                return Environment.create(!Objects.equals(left.getValue(), right.getValue()));

            case "+":
                if (left.getValue() instanceof String || right.getValue() instanceof String) {
                    return Environment.create(left.getValue().toString() + right.getValue().toString());
                } else if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).add((BigInteger) right.getValue()));
                } else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).add((BigDecimal) right.getValue()));
                } else {
                    throw new RuntimeException("Invalid types for + operator.");
                }

            case "-":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).subtract((BigInteger) right.getValue()));
                } else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).subtract((BigDecimal) right.getValue()));
                } else {
                    throw new RuntimeException("Invalid types for - operator.");
                }

            case "*":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).multiply((BigInteger) right.getValue()));
                } else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).multiply((BigDecimal) right.getValue()));
                } else {
                    throw new RuntimeException("Invalid types for * operator.");
                }

            case "/":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    BigInteger rightInt = (BigInteger) right.getValue();
                    if (rightInt.equals(BigInteger.ZERO)) {
                        throw new RuntimeException("Division by zero.");
                    }
                    return Environment.create(((BigInteger) left.getValue()).divide(rightInt));
                } else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    BigDecimal rightDec = (BigDecimal) right.getValue();
                    if (rightDec.compareTo(BigDecimal.ZERO) == 0) {
                        throw new RuntimeException("Division by zero.");
                    }
                    return Environment.create(((BigDecimal) left.getValue()).divide(rightDec, RoundingMode.HALF_EVEN));
                } else {
                    throw new RuntimeException("Invalid types for / operator.");
                }

            case "^":
                if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    BigInteger leftInt = (BigInteger) left.getValue();
                    BigInteger rightInt = (BigInteger) right.getValue();
                    try {
                        return Environment.create(leftInt.pow(rightInt.intValueExact()));
                    } catch (ArithmeticException e) {
                        throw new RuntimeException("Exponent out of integer range.");
                    }
                } else {
                    throw new RuntimeException("Invalid types for ^ operator.");
                }

            default:
                throw new RuntimeException("Unsupported operator: " + ast.getOperator());
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if (!ast.getOffset().isPresent()) {
            return scope.lookupVariable(ast.getName()).getValue();
        } else {
            Environment.PlcObject list = scope.lookupVariable(ast.getName()).getValue();
            Environment.PlcObject offset = visit(ast.getOffset().get());

            if (offset.getValue() instanceof BigInteger) {
                BigInteger index = (BigInteger) offset.getValue();
                List<Environment.PlcObject> values = requireType(List.class, list);
                if (index.compareTo(BigInteger.ZERO) >= 0 && index.compareTo(BigInteger.valueOf(values.size())) < 0) {
                    return values.get(index.intValue());
                } else {
                    throw new RuntimeException("List index out of bounds");
                }
            } else {
                throw new RuntimeException("List index must be an integer");
            }
        }
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> args = ast.getArguments().stream()
                .map(this::visit)
                .collect(Collectors.toList());
        return scope.lookupFunction(ast.getName(), args.size()).invoke(args);
    }


    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Environment.PlcObject> values = ast.getValues().stream()
                .map(this::visit)
                .collect(Collectors.toList());
        List<Object> rawValues = values.stream()
                .map(Environment.PlcObject::getValue)
                .collect(Collectors.toList());
        return Environment.create(rawValues);
    }



    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
