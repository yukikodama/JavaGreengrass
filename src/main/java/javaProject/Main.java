package javaProject;

public class Main {
    public static final void main(String... args) {
        System.out.println("HelloWorld.");
        try {
            new Main().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute() {
    }
}
