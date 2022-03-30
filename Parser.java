import java.util.*;

public class Parser {
    // Recursive descent parser that inputs a C++Lite program and
    // generates its abstract syntax. Each method corresponds to
    // a concrete syntax grammar rule, which appears as a comment
    // at the beginning of the method.

    Token token; // current token from the input stream
    Lexer lexer;
    Variable currentfnct;

    public Parser(Lexer ts) { // Open the C++Lite source program
        lexer = ts; // as a token stream, and
        token = lexer.next(); // retrieve its first Token
    }

    private String match(TokenType t) {
        String value = token.value();
        if (token.type().equals(t))
            token = lexer.next();
        else
            error(t);
        return value;
    }

    private void error(TokenType tok) {
        System.err.println("Syntax error: expecting: " + tok + "; saw: " + token);
        System.exit(1);
    }

    private void error(String tok) {
        System.err.println("Syntax error: expecting: " + tok + "; saw: " + token);
        System.exit(1);
    }

    public Program program() {
        Declarations globals = new Declarations();
        Functions functions = new Functions();

        while (isType()) {
            FunctionOrGlobal(globals, functions);
        }

        Function mainFunction = MainFunction();
        functions.add(mainFunction);

        return new Program(globals, functions);
    }

    private void FunctionOrGlobal(Declarations globals, Functions functions) {
        Type t = type();
        if (t.equals(Type.INT) && token.type().equals(TokenType.Main)) {
            return;
        }
        Variable v = new Variable(match(TokenType.Identifier));
        if (token.type().equals(TokenType.LeftParen)) {
            currentfnct = v;
            token = lexer.next();
            Declarations parameters = new Declarations();
            Type paramsType = type();
            Variable parameter = new Variable(match(TokenType.Identifier));
            parameters.add(new Declaration(parameter, paramsType));
            while (token.type().equals(TokenType.Comma)) {
                token = lexer.next();
                paramsType = type();
                parameter = new Variable(match(TokenType.Identifier));
                parameters.add(new Declaration(parameter, paramsType));
            }
            match(TokenType.RightParen);
            match(TokenType.LeftBrace);
            Declarations locals = declarations();
            Block body = programstatements();
            match(TokenType.RightBrace);
            functions.add(new Function(t, v.toString(), parameters, locals, body));
        } else {
            globals.add(new Declaration(v, t));
            Global(t, globals);
        }
    }

    private void Global(Type globalType, Declarations globals) {

        while (token.type().equals(TokenType.Comma)) {

            token = lexer.next();

            // <<Identifier>>
            Variable v = new Variable(match(TokenType.Identifier));
            // <<Declaration>>
            globals.add(new Declaration(v, globalType));
        }

        match(TokenType.Semicolon);
    }

    private Function MainFunction() {
        // Program --> int main ( ) '{' Declarations Statements '}'
        match(TokenType.Main);
        match(TokenType.LeftParen);
        Declarations params = declarations();
        match(TokenType.RightParen);
        match(TokenType.LeftBrace);
        Declarations locals = declarations();
        Block body = programstatements();
        match(TokenType.RightBrace);

        return new Function(Type.INT, Token.mainTok.toString(), params, locals, body);
    }

    private Block programstatements() {
        Block b = new Block();
        Statement s;
        while (token.type().equals(TokenType.Semicolon) || token.type().equals(TokenType.LeftBrace)
                || token.type().equals(TokenType.If) || token.type().equals(TokenType.While)
                || token.type().equals(TokenType.Identifier)
                || token.type().equals(TokenType.Return)) {
            s = statement();
            b.members.add(s);
        }
        return b;
    }

    private Declarations declarations() {
        // Declarations --> { Declaration }
        Declarations ds = new Declarations();
        while (isType()) {
            declaration(ds);
        }
        return ds; // student exercise
    }

    private void declaration(Declarations ds) {
        // Declaration --> Type Identifier { , Identifier } ;
        Variable v;
        Declaration d;
        Type t = type();
        v = new Variable(match(TokenType.Identifier));
        d = new Declaration(v, t);
        ds.add(d);

        while (token.type().equals(TokenType.Comma)) {
            token = lexer.next();
            v = new Variable(match(TokenType.Identifier));
            d = new Declaration(v, t);
            // d = (v, t);
            ds.add(d);
        }
        match(TokenType.Semicolon);
        // student exercise
    }

    private Type type() {
        // Type --> int | bool | float | char
        Type t = null;
        if (token.type().equals(TokenType.Int)) {
            t = Type.INT;
        } else if (token.type().equals(TokenType.Bool)) {
            t = Type.BOOL;
        } else if (token.type().equals(TokenType.Float)) {
            t = Type.FLOAT;
        } else if (token.type().equals(TokenType.Char)) {
            t = Type.CHAR;
        } else if (token.type().equals(TokenType.Void)) {
            t = Type.VOID;
        } else
            error("Error in Type construction");
        token = lexer.next();
        // student exercise
        return t;
    }

    private Statement statement() {
        // Statement --> ; | Block | Assignment | IfStatement | WhileStatement
        Statement s = null;
        if (token.type().equals(TokenType.Semicolon))
            s = new Skip();
        else if (token.type().equals(TokenType.LeftBrace))
            s = statements();
        else if (token.type().equals(TokenType.If))
            s = ifStatement();
        else if (token.type().equals(TokenType.While))
            s = whileStatement();
        else if (token.type().equals(TokenType.Identifier)) {
            Variable v = new Variable(match(TokenType.Identifier));
            if (token.type().equals(TokenType.Assign)) {
                s = assignment(v);
            } else if (token.type().equals(TokenType.LeftParen)) {
                Call c = callStatement(v);
                match(TokenType.Semicolon);
                s = c;
            }
        } else if (token.type().equals(TokenType.Return))
            s = returnStatement();
        else
            error("Error in Statement construction");
        return s;
    }

    private Call callStatement(Variable identifier) {
        match(TokenType.LeftParen);
        Expressions arguments = new Expressions();
        while (!(token.type().equals(TokenType.RightParen))) {
            arguments.add(expression());
            if (token.type().equals(TokenType.Comma)) {
                match(TokenType.Comma);
            }
        }
        match(TokenType.RightParen);
        return new Call(identifier.toString(), arguments);
    }

    private Return returnStatement() {
        match(TokenType.Return);
        Expression rtn = expression();
        match(TokenType.Semicolon);
        return new Return(currentfnct, rtn);
    }

    private Block statements() {
        // Block --> '{' Statements '}'
        Statement s;
        Block b = new Block();

        match(TokenType.LeftBrace);
        while (token.type().equals(TokenType.Semicolon) || token.type().equals(TokenType.LeftBrace)
                || token.type().equals(TokenType.If) || token.type().equals(TokenType.While)
                || token.type().equals(TokenType.Identifier) || token.type().equals(TokenType.Return)) {
            s = statement();
            b.members.add(s);
        }
        match(TokenType.RightBrace);
        return b;
    }

    private Assignment assignment(Variable target) {
        // Assignment --> Identifier = Expression ;
        Expression source;

        // target = new Variable(match(TokenType.Identifier));
        match(TokenType.Assign);
        source = expression();
        match(TokenType.Semicolon);
        return new Assignment(target, source);
    }

    private Conditional ifStatement() {
        Conditional condition;
        Statement s;
        Expression test;

        match(TokenType.If);
        match(TokenType.LeftParen);
        test = expression();
        match(TokenType.RightParen);
        s = statement();
        if (token.type().equals(TokenType.Else)) {
            Statement elsestate = statement();
            condition = new Conditional(test, s, elsestate);
        } else {
            condition = new Conditional(test, s);
        }
        return condition;
    }

    private Loop whileStatement() {
        // WhileStatement --> while ( Expression ) Statement
        Statement body;
        Expression test;

        match(TokenType.While);
        match(TokenType.LeftParen);
        test = expression();
        match(TokenType.RightParen);
        body = statement();
        return new Loop(test, body);
    }

    private Expression expression() {
        // Expression --> Conjunction { || Conjunction }
        Expression c = conjunction();
        while (token.type().equals(TokenType.Or)) {
            Operator op = new Operator(match(token.type()));
            Expression e = expression();
            c = new Binary(op, c, e);
        }
        return c; // student exercise
    }

    private CallExpression callExpression(Variable identifier) {
        match(TokenType.LeftParen);
        Expressions arguments = new Expressions();
        while (!(token.type().equals(TokenType.RightParen))) {
            arguments.add(expression());
            if (token.type().equals(TokenType.Comma)) {
                match(TokenType.Comma);
            }
        }
        match(TokenType.RightParen);
        return new CallExpression(identifier.toString(), arguments);
    }

    private Expression conjunction() {
        // Conjunction --> Equality { && Equality }
        Expression eq = equality();
        while (token.type().equals(TokenType.And)) {
            Operator op = new Operator(match(token.type()));
            Expression c = conjunction();
            eq = new Binary(op, eq, c);
        }
        return eq;
    }

    private Expression equality() {
        // Equality --> Relation [ EquOp Relation ]
        Expression rel = relation();
        while (isEqualityOp()) {
            Operator op = new Operator(match(token.type()));
            Expression rel2 = relation();
            rel = new Binary(op, rel, rel2);
        }
        return rel; // student exercise
    }

    private Expression relation() {
        // Relation --> Addition [RelOp Addition]
        Expression e1 = addition();
        while (isRelationalOp()) {
            Operator op = new Operator(match(token.type()));
            Expression e2 = addition();
            e1 = new Binary(op, e1, e2);
        }
        return e1; // student exercise
    }

    private Expression addition() {
        // Addition --> Term { AddOp Term }
        Expression e = term();
        while (isAddOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = term();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression term() {
        // Term --> Factor { MultiplyOp Factor }
        Expression e = factor();
        while (isMultiplyOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term2 = factor();
            e = new Binary(op, e, term2);
        }
        return e;
    }

    private Expression factor() {
        // Factor --> [ UnaryOp ] Primary
        if (isUnaryOp()) {
            Operator op = new Operator(match(token.type()));
            Expression term = primary();
            return new Unary(op, term);
        } else
            return primary();
    }

    private Expression primary() {
        // Primary --> Identifier | Literal | ( Expression )
        // | Type ( Expression )
        Expression e = null;
        if (token.type().equals(TokenType.Identifier)) {
            Variable v = new Variable(match(TokenType.Identifier));
            if (token.type().equals(TokenType.LeftParen)) {
                e = callExpression(v);
            } else {
                e = v;
            }
        } else if (isLiteral()) {
            e = literal();
        } else if (token.type().equals(TokenType.LeftParen)) {
            token = lexer.next();
            e = expression();
            match(TokenType.RightParen);
        } else if (isType()) {
            Operator op = new Operator(match(token.type()));
            match(TokenType.LeftParen);
            Expression term = expression();
            match(TokenType.RightParen);
            e = new Unary(op, term);
        } else
            error("Identifier | Literal | ( | Type");
        return e;
    }

    private Value literal() {
        Value value = null;
        String stval = token.value();
        if (token.type().equals(TokenType.IntLiteral)) {
            value = new IntValue(Integer.parseInt(stval));
            token = lexer.next();
            // System.out.println("found int literal");
        } else if (token.type().equals(TokenType.FloatLiteral)) {
            value = new FloatValue(Float.parseFloat(stval));
            token = lexer.next();
        } else if (token.type().equals(TokenType.CharLiteral)) {
            value = new CharValue(stval.charAt(0));
            token = lexer.next();
        } else if (token.type().equals(TokenType.True)) {
            value = new BoolValue(true);
            token = lexer.next();
        } else if (token.type().equals(TokenType.False)) {
            value = new BoolValue(false);
            token = lexer.next();
        } else
            error("Error in literal value contruction");
        return value;
    }

    private boolean isAddOp() {
        return token.type().equals(TokenType.Plus) || token.type().equals(TokenType.Minus);
    }

    private boolean isMultiplyOp() {
        return token.type().equals(TokenType.Multiply) || token.type().equals(TokenType.Divide);
    }

    private boolean isUnaryOp() {
        return token.type().equals(TokenType.Not) || token.type().equals(TokenType.Minus);
    }

    private boolean isEqualityOp() {
        return token.type().equals(TokenType.Equals) || token.type().equals(TokenType.NotEqual);
    }

    private boolean isRelationalOp() {
        return token.type().equals(TokenType.Less) || token.type().equals(TokenType.LessEqual)
                || token.type().equals(TokenType.Greater) || token.type().equals(TokenType.GreaterEqual);
    }

    private boolean isType() {
        return token.type().equals(TokenType.Int) || token.type().equals(TokenType.Bool)
                || token.type().equals(TokenType.Float) || token.type().equals(TokenType.Char)
                || token.type().equals(TokenType.Void);
    }

    private boolean isLiteral() {
        return token.type().equals(TokenType.IntLiteral) || isBooleanLiteral()
                || token.type().equals(TokenType.FloatLiteral) || token.type().equals(TokenType.CharLiteral);
    }

    private boolean isBooleanLiteral() {
        return token.type().equals(TokenType.True) || token.type().equals(TokenType.False);
    }

    public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0); // display abstract syntax tree
    } // main

} // Parser
