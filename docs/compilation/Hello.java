/*
 * Hello.java - Simple Java program demonstrating Java bytecode compilation and JVM execution
 *
 * Compilation (Bytecode generation):
 *   javac Hello.java
 *
 * Execution (JVM bytecode execution):
 *   java Hello
 */

public class Hello {
    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println(" Hello from Java (JVM Bytecode Execution)! ");
        System.out.println("=========================================");
        System.out.println("Compilation: javac translates this into portable .class bytecode.");
        System.out.println("Execution: The JVM interprets and JIT-compiles bytecode into native CPU instructions.");
        System.out.println("Platform Independence: 'Write Once, Run Anywhere' (WORA).");
    }
}
