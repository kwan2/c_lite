import java.util.*;

class ActivationRecords {
    Stack<State> runtimeStack = new Stack<>();

    public ActivationRecords() {
    }

    public ActivationRecords pushState(State state) {
        runtimeStack.push(state);
        return this;
    }

    public ActivationRecords popState() {
        runtimeStack.pop();
        return this;
    }

    public State peekState() {
        return runtimeStack.peek();
    }

    public ActivationRecords put(Variable key, Value val) {
        State top = runtimeStack.peek();
        top.put(key, val);
        return this;
    }

    public ActivationRecords remove(Variable key) {
        State top = runtimeStack.peek();
        top.remove(key);
        return this;
    }
    

    public Value get(Variable key) {
        State top = runtimeStack.peek();
        Iterator<State> stateIterator = runtimeStack.listIterator();
        State global = stateIterator.next();

        if (top.get(key) != null) {
            return top.get(key);
        } else {
            return global.get(key);
        }
    }

    public ActivationRecords onion(Variable key, Value val) {
        State top = runtimeStack.peek();
        Iterator<State> stateIterator = runtimeStack.listIterator();
        State global = stateIterator.next();

        if (top.get(key) != null) {
            top.put(key, val);
        } else {
            global.put(key, val);
        }

        return this;
    }

    public ActivationRecords onion(State t) {
        State top = runtimeStack.peek();
        for (Variable key : t.keySet()) {
            top.put(key, t.get(key));
        }
        return this;
    }

    public void display() {
        System.out.println("Globals and top frame: ");
        System.out.println("-------------");
        Iterator<State> stateIterator = runtimeStack.iterator();

        State global = stateIterator.next();
        State top = runtimeStack.peek();

        if (global.equals(top)) {
            global.display();
        } else {
            global.display();
            top.display();
        }

        System.out.println("-------------");
    }
}

public class State extends HashMap<Variable, Value> {
    // Defines the set of variables and their associated values
    // that are active during interpretation

    public State() {
    }

    public State(Variable key, Value val) {
        put(key, val);
    }

    public State onion(Variable key, Value val) {
        put(key, val);
        return this;
    }

    public State onion(State t) {
        for (Variable key : t.keySet())
            put(key, t.get(key));
        return this;
    }

    void display() {

        Iterator<Variable> iterator = this.keySet().iterator();
        while (iterator.hasNext()) {
            Variable key = (Variable) iterator.next();
            System.out.println("<" + key + ", " + this.get(key) + ">");

        }

    }
}
