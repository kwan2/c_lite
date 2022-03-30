import java.util.ArrayList;
import java.util.Iterator;

public class Semantics
{
	ActivationRecords M(Program p)
	{
		ActivationRecords stateRecords = new ActivationRecords();
		try
		{
			stateRecords.pushState(initialState(p.globals));
			stateRecords = M(new Call("main", new Expressions()), stateRecords, p.functions);
			stateRecords.popState();
			return stateRecords;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return stateRecords;
	}
	
	State initialState(Declarations d)
	{
		State state = new State();
		Value intUndef = new IntValue();
		for (Declaration decl : d)
		{
			state.put(decl.v, Value.mkValue(decl.t));
		}
		return state;
	}
	
	ActivationRecords M(Call c, ActivationRecords stateRecords, Functions functions)
	{
		
		Function function = functions.NametFunction(c.name);
		State newState = new State();
		
		Iterator<Expression> argIt = c.args.iterator();
		Iterator<Declaration> paramIt = function.params.iterator();
		while (argIt.hasNext())
		{
			Expression e = argIt.next();
			Declaration d = paramIt.next();
			Value value = M(e, stateRecords, functions);
			newState.put(d.v, value);
		}

		stateRecords.pushState(newState);
		for (Declaration di : function.locals)
		{
			newState.put(di.v, Value.mkValue(di.t));
		}
		if (!c.name.equals(Token.mainTok.value()))
		{
			stateRecords.put(new Variable(c.name), Value.mkValue(functions.NametFunction(c.name).t));
			System.out.println( "Calling " + c.name);
			stateRecords.display();
		}
		else{
			System.out.println( "Entering " + c.name);
			stateRecords.display();
		}
		
		
		Iterator<Statement> members = function.body.members.iterator();
		while (members.hasNext())
		{
			Statement statement = members.next();
			
			if(stateRecords.get(new Variable(c.name)) != null && !stateRecords.get(new Variable(c.name)).isUndef())
			{
				System.out.println("Returning " + c.name);
				stateRecords.display();
				
				return stateRecords;
			}
			
			if (statement instanceof Return)
			{
				Return r = (Return) statement;
				Value returnValue = M(r.result, stateRecords, functions);
				stateRecords.put(r.target, returnValue);
				
				System.out.println("Returning " + c.name);
				stateRecords.display();
				
				return stateRecords;
			}
			else
			{
				stateRecords = M(statement, stateRecords, functions);
			}
		}

		System.out.println("Returning " + c.name);
		stateRecords.display();
		
		return stateRecords;
	}
	
	ActivationRecords M(Statement s, ActivationRecords stateRecords, Functions functions)
	{
		if (s instanceof Skip)
		{
			return M((Skip) s, stateRecords, functions);
		}
		if (s instanceof Assignment)
		{
			return M((Assignment) s, stateRecords, functions);
		}
		if (s instanceof Conditional)
		{
			return M((Conditional) s, stateRecords, functions);
		}
		if (s instanceof Loop)
		{
			return M((Loop) s, stateRecords, functions);
		}
		if (s instanceof Block)
		{
			return M((Block) s, stateRecords, functions);
		}
	if (s instanceof Call)
		{
			Call call = (Call) s;
			stateRecords = M(call, stateRecords, functions);
			stateRecords.popState();
			return stateRecords;
		}
		if (s instanceof Return)
		{
			Return r = (Return) s;
			
			if(stateRecords.get(r.target) != null && !stateRecords.get(r.target).isUndef())
			{
				return stateRecords;
			}
			Value returnValue = M(r.result, stateRecords, functions);
			stateRecords.put(r.target, returnValue);
			
			return stateRecords;
		}
		throw new IllegalArgumentException("should never reach here");
	}
	
	ActivationRecords M(Skip s, ActivationRecords stateRecords, Functions functions)
	{
		return stateRecords;
	}
	
	ActivationRecords M(Assignment a, ActivationRecords state, Functions functions)
	{
		return state.onion(a.target, M(a.source, state, functions));
	}
	
	ActivationRecords M(Block b, ActivationRecords state, Functions functions)
	{
		for (Statement s : b.members)
		{
			state = M(s, state, functions);
		}
		return state;
	}
	
	ActivationRecords M(Conditional c, ActivationRecords stateRecords, Functions functions)
	{
		if (M(c.test, stateRecords, functions).boolValue())
		{
			return M(c.thenbranch, stateRecords, functions);
		}
		else
		{
			return M(c.elsebranch, stateRecords, functions);
		}
	}
	
	ActivationRecords M(Loop l, ActivationRecords stateRecords, Functions functions)
	{
		if (M(l.test, stateRecords, functions).boolValue())
		{
			return M(l, M(l.body, stateRecords, functions), functions);
		}
		else
		{
			return stateRecords;
		}
	}
	
	Value applyBinary(Operator op, Value v1, Value v2) {
        StaticTypeCheck.check(!v1.isUndef() && !v2.isUndef(),
                "reference to undef value");
        if (op.val.equals(Operator.INT_PLUS))
            return new IntValue(v1.intValue() + v2.intValue());
        if (op.val.equals(Operator.INT_MINUS))
            return new IntValue(v1.intValue() - v2.intValue());
        if (op.val.equals(Operator.INT_TIMES))
            return new IntValue(v1.intValue() * v2.intValue());
        if (op.val.equals(Operator.INT_DIV))
            return new IntValue(v1.intValue() / v2.intValue());

        if (op.val.equals(Operator.INT_LT))
            return new BoolValue(v1.intValue() < v2.intValue());
        if (op.val.equals(Operator.INT_LE))
            return new BoolValue(v1.intValue() <= v2.intValue());
        if (op.val.equals(Operator.INT_EQ))
            return new BoolValue(v1.intValue() == v2.intValue());
        if (op.val.equals(Operator.INT_LE))
            return new BoolValue(v1.intValue() != v2.intValue());
        if (op.val.equals(Operator.INT_GT))
            return new BoolValue(v1.intValue() > v2.intValue());
        if (op.val.equals(Operator.INT_GE))
            return new BoolValue(v1.intValue() >= v2.intValue());

        if (op.val.equals(Operator.FLOAT_PLUS))
            return new FloatValue(v1.floatValue() + v2.floatValue());
        if (op.val.equals(Operator.FLOAT_MINUS))
            return new FloatValue(v1.floatValue() - v2.floatValue());
        if (op.val.equals(Operator.FLOAT_TIMES))
            return new FloatValue(v1.floatValue() * v2.floatValue());
        if (op.val.equals(Operator.FLOAT_DIV))
            return new FloatValue(v1.floatValue() / v2.floatValue());

        if (op.val.equals(Operator.FLOAT_LT))
            return new BoolValue(v1.floatValue() < v2.floatValue());
        if (op.val.equals(Operator.FLOAT_LE))
            return new BoolValue(v1.floatValue() <= v2.floatValue());
        if (op.val.equals(Operator.FLOAT_EQ))
            return new BoolValue(v1.floatValue() == v2.floatValue());
        if (op.val.equals(Operator.FLOAT_LE))
            return new BoolValue(v1.floatValue() != v2.floatValue());
        if (op.val.equals(Operator.FLOAT_GT))
            return new BoolValue(v1.floatValue() > v2.floatValue());
        if (op.val.equals(Operator.FLOAT_GE))
            return new BoolValue(v1.floatValue() >= v2.floatValue());

        if (op.val.equals(Operator.CHAR_LT))
            return new BoolValue(v1.charValue() < v2.charValue());
        if (op.val.equals(Operator.CHAR_LE))
            return new BoolValue(v1.charValue() <= v2.charValue());
        if (op.val.equals(Operator.CHAR_EQ))
            return new BoolValue(v1.charValue() == v2.charValue());
        if (op.val.equals(Operator.CHAR_LE))
            return new BoolValue(v1.charValue() != v2.charValue());
        if (op.val.equals(Operator.CHAR_GT))
            return new BoolValue(v1.charValue() > v2.charValue());
        if (op.val.equals(Operator.CHAR_GE))
            return new BoolValue(v1.charValue() >= v2.charValue());

        if (op.val.equals(Operator.BOOL_EQ))
            return new BoolValue(v1.boolValue() == v2.boolValue());
        if (op.val.equals(Operator.BOOL_LE))
            return new BoolValue(v1.boolValue() != v2.boolValue());
        /*
         * if (op.val.equals(Operator.BOOL_LT))
         * return new BoolValue(v1.boolValue( ) < v2.boolValue( ));
         * if (op.val.equals(Operator.BOOL_LE))
         * return new BoolValue(v1.boolValue( ) <= v2.boolValue( ));
         * if (op.val.equals(Operator.BOOL_GT))
         * return new BoolValue(v1.boolValue( ) > v2.boolValue( ));
         * if (op.val.equals(Operator.BOOL_GE))
         * return new BoolValue(v1.boolValue( ) >= v2.boolValue( ));
         */

        throw new IllegalArgumentException("should never reach here");
    }

    Value applyUnary(Operator op, Value v) {
        StaticTypeCheck.check(!v.isUndef(),
                "reference to undef value");
        if (op.val.equals(Operator.NOT))
            return new BoolValue(!v.boolValue());
        else if (op.val.equals(Operator.INT_NEG))
            return new IntValue(-v.intValue());
        else if (op.val.equals(Operator.FLOAT_NEG))
            return new FloatValue(-v.floatValue());
        else if (op.val.equals(Operator.I2F))
            return new FloatValue((float) (v.intValue()));
        else if (op.val.equals(Operator.F2I))
            return new IntValue((int) (v.floatValue()));
        else if (op.val.equals(Operator.C2I))
            return new IntValue((int) (v.charValue()));
        else if (op.val.equals(Operator.I2C))
            return new CharValue((char) (v.intValue()));
        throw new IllegalArgumentException("should never reach here");
    }

	Value M(Expression e, ActivationRecords stateRecords,Functions functions)
	{
		if (e instanceof Value)
		{
			return (Value) e;
		}
		if (e instanceof Variable)
		{
			return (Value) (stateRecords.get((Variable) e));
		}
		if (e instanceof Binary)
		{
			Binary b = (Binary) e;
			return applyBinary(b.op,
					M(b.term1, stateRecords,functions), M(b.term2, stateRecords,functions));
		}
		if (e instanceof Unary)
		{
			Unary u = (Unary) e;
			return applyUnary(u.op, M(u.term, stateRecords,functions));
		}
		// Call
		if (e instanceof Call)
		{
			Call c = (Call) e;
			stateRecords = M(c, stateRecords, functions);
			Value returnValue = stateRecords.get(new Variable(c.name));
			stateRecords.popState();
			return returnValue;
		}
		throw new IllegalArgumentException("should never reach here");
	}
	
	public static void main(String args[]) {
        Parser parser = new Parser(new Lexer(args[0]));
        Program prog = parser.program();
        prog.display(0); // student exercise
        System.out.println("\nBegin type checking...programs/" + args[0] + "\n");
        System.out.println("Type map:");
        TypeMap map = StaticTypeCheck.typing(prog.globals);
        map.display(); // student exercise
        StaticTypeCheck.V(prog);
        Program out = TypeTransformer.T(prog, map);
        // System.out.println("Output AST");
        // out.display(0); // student exercise
        Semantics semantics = new Semantics();
        ActivationRecords state = semantics.M(out);
        System.out.println("\nBegin interpreting...programs/" + args[0] + "\n");
        System.out.println("Final State");
        state.display(); // student exercise
    }
}