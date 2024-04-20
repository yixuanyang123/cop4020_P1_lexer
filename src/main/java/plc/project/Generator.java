package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object == null) {
                writer.write("");
            } else if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }


    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        indent++;
        if (!ast.getGlobals().isEmpty())
        {
            newline(--indent);

            indent++;
            for (Ast.Global global : ast.getGlobals()) {
                newline(indent);
                visit(global);
            }
        }

        newline(0);
        newline(indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(0);
        indent--;
        for (Ast.Function function : ast.getFunctions()) {
            newline(++indent);
            visit(function);
//            indent--;
        }
        newline(indent);
        print("}");
        return null;
    }


    @Override
    public Void visit(Ast.Global ast) {
        if (!ast.getMutable()) {
            print("final ");
        }
        boolean isList = ast.getValue().isPresent() && ast.getValue().get() instanceof Ast.Expression.PlcList;
        switch (ast.getTypeName()) {
            case "Integer":
                print(isList ? "int[]" : "int");
                break;
            case "Decimal":
                print(isList ? "double[]" : "double");
                break;
            case "Boolean":
                print(isList ? "boolean[]" : "boolean");
                break;
            case "Character":
                print(isList ? "char[]" : "char");
                break;
            case "String":
                print(isList ? "String[]" : "String");
                break;
            default:
                print(ast.getTypeName());
                break;
        }
        print(" ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            if (isList) {
                print("{");
                List<Ast.Expression> elements = ((Ast.Expression.PlcList) ast.getValue().get()).getValues();
                for (int i = 0; i < elements.size(); i++) {
                    visit(elements.get(i));
                    if (i < elements.size() - 1) {
                        print(", ");
                    }
                }
                print("}");
            } else {
                visit(ast.getValue().get());
            }
        }
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Function ast) {
        String returnType = ast.getReturnTypeName().orElse("Void");
        switch (returnType) {
            case "Integer":
                print("int");
                break;
            case "Decimal":
                print("double");
                break;
            case "Boolean":
                print("boolean");
                break;
            case "Character":
                print("char");
                break;
            case "String":
                print("String");
                break;
            default:
                print("void");
                break;
        }

        print(" ", ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            String parameter = ast.getParameters().get(i);
            print(parameter);
            print(" arg", i);
            if (i < ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        print(") {");
//        newline(++indent);
        indent = indent+1;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
//            newline(indent);
        }
        if(ast.getStatements().size()!=0)
            newline(--indent);
        else indent--;
        print("}");
        newline(--indent);
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        if (ast.getTypeName().isPresent()) {
            switch (ast.getTypeName().get()) {
                case "Integer":
                    print("int");
                    break;
                case "Decimal":
                    print("double");
                    break;
                case "Boolean":
                    print("boolean");
                    break;
                case "Character":
                    print("char");
                    break;
                case "String":
                    print("String");
                    break;
                default:
                    print(ast.getTypeName().get());
                    break;
            }
        } else if (ast.getValue().isPresent()) {
            Ast.Expression value = ast.getValue().get();
            if (value instanceof Ast.Expression.Literal) {
                Object literal = ((Ast.Expression.Literal)value).getLiteral();
                if (literal instanceof Integer) {
                    print("int");
                } else if (literal instanceof BigDecimal) {
                    print("double");
                } else if (literal instanceof Boolean) {
                    print("boolean");
                } else if (literal instanceof Character) {
                    print("char");
                } else if (literal instanceof String) {
                    print("String");
                }
            }
        }
        print(" ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(visit(ast.getReceiver()));
        print(" = ");
        print(visit(ast.getValue()));
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        for (Ast.Statement statement : ast.getThenStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;
        newline(indent);
        print("}");
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;
            for (Ast.Statement statement : ast.getElseStatements()) {
                newline(indent);
                visit(statement);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        newline(++indent);
        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            if (!caseStmt.getValue().isPresent()) {
                print("default:");
            } else {
                print("case ");
                visit(caseStmt.getValue().get());
                print(":");
            }
            indent = indent+1;
            for (Ast.Statement statement : caseStmt.getStatements()) {
                newline(indent);
                visit(statement);
            }
            if (caseStmt.getValue().isPresent()) {
                newline(indent);
                print("break;");
                newline(--indent);
            }
        }
        newline(indent-2);
        print("}");
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
            print(":");
        } else {
            print("default:");
        }
        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
            if (statement instanceof Ast.Statement.Expression ||
                    statement instanceof Ast.Statement.Assignment ||
                    statement instanceof Ast.Statement.Return) {
                print(";");
            }
            newline(indent);
        }
        if (ast.getValue().isPresent()) {
            newline(indent);
            print("break;");
        }
        indent--;
        return null;
    }


    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        if (ast.getStatements().isEmpty()) {
            print("}");
        } else {
            indent++;
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent);
                visit(statement);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }


    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal instanceof BigDecimal) {
            print(((BigDecimal) literal).toPlainString());
        }
        else if (literal instanceof Boolean) {
            print(literal.toString());
        }
        else if (literal instanceof BigInteger) {
            print(((BigInteger) literal).toString());
        }
        else if (literal instanceof Character) {
            print("'", literal, "'");
        }
        else if (literal instanceof String) {
            print("\"", literal, "\"");
        }
        else {
            print(literal.toString());
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if ("^".equals(ast.getOperator())) {
            print("Math.pow(");
            visit(ast.getLeft());
            print(", ");
            visit(ast.getRight());
            print(")");
        } else {
            visit(ast.getLeft());
            print(" ", ast.getOperator(), " ");
            visit(ast.getRight());
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getName() != null) {
            print(ast.getName());
        } else {
            print("/* Variable name was null! */");
        }
        if (ast.getOffset().isPresent()) {
            Ast.Expression offset = ast.getOffset().get();
            if (offset != null) {
                print("[");
                visit(offset);
                print("]");
            } else {
                print("[/* Offset was null! */]");
            }
        }
        return null;
    }


    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function function = ast.getFunction();
        print(function.getJvmName());
        print("(");
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i < ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }


    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        List<Ast.Expression> values = ast.getValues();
        for (int i = 0; i < values.size(); i++) {
            visit(values.get(i));
            if (i < values.size() - 1) {
                print(", ");
            }
        }
        print("}");
        return null;
    }
}
