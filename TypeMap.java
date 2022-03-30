import java.util.*;

public class TypeMap extends HashMap<Variable, Type> {
    // TypeMap is implemented as a Java HashMap.
    // Plus a 'display' method to facilitate experimentation.
    public void display() {
        System.out.print(" { ");
        for (Variable v : this.keySet()) {
            System.out.print("<" + v.toString() + ", " + this.get(v) + "> ");
        }
        System.out.println("}");

    }

    public void display(Functions functions) {
        System.out.print(" { ");
        for (Function f : functions) {
            System.out.print("<" + f.id + ", " + f.t + ", ");
            System.out.print("{ ");
            for (Declaration v : f.params) {
                System.out.print("<" + v.v.toString() + ", " + v.t + "> ");
            }
            System.out.println("},");
        }
        for (Variable v : this.keySet()) {
            System.out.println("<" + v.toString() + ", " + this.get(v) + "> ");
        }
        System.out.println("}");

    }

}
