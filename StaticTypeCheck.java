
// StaticTypeCheck.java
// Static type checking for Clite is defined by the functions 
// V and the auxiliary functions typing and typeOf.  These
// functions use the classes in the Abstract Syntax of Clite.
import java.util.ArrayList;
import java.util.Iterator;

public class StaticTypeCheck {

    public static ArrayList<String> checkError = new ArrayList<>();
    public static boolean returncheck;

    public static TypeMap typing(Declarations d) {
        TypeMap map = new TypeMap();
        for (Declaration di : d)
            map.put(di.v, di.t);
        return map;
    }

    public static void check(boolean test, String msg) {
        if (test)
            return;
        // System.err.println(msg);
        // System.exit(1);
        checkError.add(msg);
    }

    public static void V(Declarations d) {
        for (int i = 0; i < d.size() - 1; i++)
            for (int j = i + 1; j < d.size(); j++) {
                Declaration di = d.get(i);
                Declaration dj = d.get(j);
                check(!(di.v.equals(dj.v)), "duplicate declaration: " + dj.v);
            }
    }

    public static void V(Program p) {
        // V(p.decpart);
        // V(p.body, typing(p.decpart));
        check(p.functions.NametFunction("main") != null, "Error! Main function not found!");
        Declarations globalAndFunctions = new Declarations();
        globalAndFunctions.addAll(p.globals);
        globalAndFunctions.addAll(p.functions.allFunctionsName());
        V(globalAndFunctions);
        V(p.functions, typing(p.globals));
    }

    public static void V(Functions functions, TypeMap tm) {
        for (Function f : functions) {
            TypeMap functionMap = new TypeMap();
            functionMap.putAll(tm);
            functionMap.putAll(typing(f.params));
            functionMap.putAll(typing(f.locals));

            Declarations localsAndParams = new Declarations();
            localsAndParams.addAll(f.locals);
            localsAndParams.addAll(f.params);
            V(localsAndParams);

            V(f, functionMap, functions);
        }
    }

    public static void V(Function function, TypeMap tm, Functions functions) {
        System.out.println("Function " + function.id + " = ");
        tm.display(functions);
        returncheck = false;
        Iterator<Statement> it = function.body.members.iterator();
        while (it.hasNext()) {
            Statement s = it.next();

            if (s instanceof Return) {

                check(!returncheck, "Function " + function.id + " has multiple return statements!");
                returncheck = true;
                V(s, tm, functions);
            } else {

                check(!returncheck,
                        "Return must be last statement in function block (in function " + function.id + ")");
                V(s, tm, functions);
            }
            if (!function.t.equals(Type.VOID) && !function.id.equals("main")) {
                check(returncheck, "Non-void function " + function.id + " missing return statement!");
            }

            else if (function.t.equals(Type.VOID)) {
                check(!returncheck, "Void function " + function.id + " has return statement when it shouldn't!");
            }
        }
    }

    public static void V(Call call, TypeMap tm, Functions functions) {

        Function function = functions.NametFunction(call.name);

        Iterator<Declaration> funcIt = function.params.iterator();
        Iterator<Expression> callIt = call.args.iterator();

        while (funcIt.hasNext()) {
            Declaration dec = funcIt.next();

            check(callIt.hasNext(), "Incorrect number of arguments for function call " + call.name);

            if (!callIt.hasNext()) {
                break;
            }

            Expression exp = callIt.next();

            Type expType = typeOf(exp, tm, functions);
            check(dec.t == expType, "Wrong type in parameter for " + dec.v + " of function " + call.name + " (got a "
                    + expType + ", expected a " + dec.t + ")");
        }

        check(!callIt.hasNext(), "Incorrect number of arguments for function call " + call.name);
    }

    public static Type typeOf(Expression e, TypeMap tm, Functions functions) {
        if (e instanceof Value)
            return ((Value) e).type;
        if (e instanceof Variable) {
            Variable v = (Variable) e;
            check(tm.containsKey(v), "undefined variable: " + v);
            return (Type) tm.get(v);
        }
        if (e instanceof Call) {
            Call c = (Call) e;
            Function f = functions.NametFunction(c.name);
            if (f == null) {
                check(f != null, "undefined variable: " + c.name);
                return Type.VOID;
            }
            return f.t;
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            if (b.op.ArithmeticOp())
                if (typeOf(b.term1, tm, functions) == Type.FLOAT)
                    return (Type.FLOAT);
                else
                    return (Type.INT);
            if (b.op.RelationalOp() || b.op.BooleanOp())
                return (Type.BOOL);
        }
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            if (u.op.NotOp())
                return (Type.BOOL);
            else if (u.op.NegateOp())
                return typeOf(u.term, tm, functions);
            else if (u.op.intOp())
                return (Type.INT);
            else if (u.op.floatOp())
                return (Type.FLOAT);
            else if (u.op.charOp())
                return (Type.CHAR);
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V(Expression e, TypeMap tm, Functions functions) {
        if (e instanceof Value)
            return;
        if (e instanceof Variable) {
            Variable v = (Variable) e;
            check(tm.containsKey(v), "undeclared variable: " + v);
            return;
        }
        if (e instanceof Call) {
            CallExpression c = (CallExpression) e;
            Function function = functions.NametFunction(c.name);
            if (function == null)
                check(function != null, "undeclared function: " + c.name);
            else
                check(!function.t.equals(Type.VOID), "This Function is void function");
            V((CallExpression) e, tm, functions);
            return;
        }
        if (e instanceof Binary) {
            Binary b = (Binary) e;
            Type typ1 = typeOf(b.term1, tm, functions);
            Type typ2 = typeOf(b.term2, tm, functions);
            V(b.term1, tm, functions);
            V(b.term2, tm, functions);
            if (b.op.ArithmeticOp())
                check(typ1 == typ2 && (typ1 == Type.INT || typ1 == Type.FLOAT), "type error for " + b.op);
            else if (b.op.RelationalOp())
                check(typ1 == typ2, "type error for " + b.op);
            else if (b.op.BooleanOp())
                check(typ1 == Type.BOOL && typ2 == Type.BOOL, b.op + ": non-bool operand");
            else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        // student exercise
        if (e instanceof Unary) {
            Unary u = (Unary) e;
            Type typU = typeOf(u.term, tm, functions);
            V(u.term, tm, functions); // valid
            if (u.op.NotOp()) {
                check((typU == Type.BOOL), "type error for" + u.op);
            } else if (u.op.NegateOp()) {
                check((typU == Type.FLOAT) || (typU == Type.INT), "type error for" + u.op);
            } else if (u.op.floatOp()) {
                check((typU == Type.INT), "type error for" + u.op);
            } else if (u.op.charOp()) {
                check((typU == Type.INT), "type error for" + u.op);
            } else if (u.op.intOp()) {
                check((typU == Type.CHAR) || (typU == Type.FLOAT), "type error for" + u.op);
            } else
                throw new IllegalArgumentException("should never reach here");
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void V(Statement s, TypeMap tm, Functions functions) {
        if (s == null)
            throw new IllegalArgumentException("AST error: null statement");
        if (s instanceof Skip)
            return;
        if (s instanceof Assignment) {
            Assignment a = (Assignment) s;
            check(tm.containsKey(a.target), " undefined target in assignment: " + a.target);
            V(a.source, tm, functions);
            Type ttype = (Type) tm.get(a.target);
            Type srctype = typeOf(a.source, tm, functions);
            if (ttype != srctype) {
                if (ttype == Type.FLOAT)
                    check(srctype == Type.INT, "mixed mode assignment to " + a.target);
                else if (ttype == Type.INT)
                    check(srctype == Type.CHAR, "mixed mode assignment to " + a.target);
                else
                    check(false, "mixed mode assignment to " + a.target);
            }
            return;
        }
        // student exercise
        if (s instanceof Conditional) {
            Conditional c = (Conditional) s;
            boolean ifReturn = false, elseReturn = false;
            V(c.test, tm, functions); // valid test Expression
            Type ttype = typeOf(c.test, tm, functions);
            if (ttype == Type.BOOL) {
                V(c.thenbranch, tm, functions);
                ifReturn = returncheck;
                returncheck = false;
                V(c.elsebranch, tm, functions);
                elseReturn = returncheck;
                returncheck = ifReturn && elseReturn;
                return;
            } else {
                check(false, "Conditional's test Expression Type NOT BOOL" + c.test);
            }
        }
        if (s instanceof Loop) {
            Loop l = (Loop) s;
            V(l.test, tm, functions);
            Type ttype = typeOf(l.test, tm, functions);
            if (ttype == Type.BOOL) {
                V(l.body, tm, functions);
                returncheck = false;
                return;
            } else
                check(false, "Loop's test Expression Type NOT BOOL" + l.test);
            return;
        }
        if (s instanceof Block) {
            Block b = (Block) s;
            for (Statement bNum : b.members) {
                V(bNum, tm, functions);
            }
            return;
        }
        if (s instanceof Call) {
            Call call = (Call) s;
            Function function = functions.NametFunction(call.name);
            check(function.t.equals(Type.VOID), "Call Statement must be a void type function!");
            V((Call) s, tm, functions);
            return;
        }
        if (s instanceof Return) {
            returncheck = true;
            Return r = (Return) s;
            Function f = functions.NametFunction(r.target.toString());
            Type t = typeOf(r.result, tm, functions);
            check(t.equals(f.t), "Return expression doesn't match function's return type! (got a " + t + ", expected a "
                    + f.t + ")");
            return;
        }
        throw new IllegalArgumentException("should never reach here");
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        // prog.display(); // student exercise

        prog.display(0);

        System.out.println("\nBegin type checking...");
        System.out.println("Globals = ");
        TypeMap globalmap = typing(prog.globals);

        globalmap.display();
        V(prog);
        if (checkError.isEmpty()) {
            System.out.println(" Type system errors not detected");
        } else {
            System.out.println(checkError.size() + " type system errors detected");
            for (String s : checkError) {
                System.out.println(s);
            }
        }
    } // main

} // class StaticTypeCheck
