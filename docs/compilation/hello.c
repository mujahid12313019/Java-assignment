/*
 * hello.c - Simple C program demonstrating native C compilation process
 *
 * Compilation:
 *   gcc hello.c -o hello
 * Execution:
 *   ./hello       (Linux/macOS)
 *   hello.exe     (Windows)
 */

#include <stdio.h>

int main(void) {
    printf("=========================================\n");
    printf(" Hello from C (Direct Native Execution)! \n");
    printf("=========================================\n");
    printf("Architecture: Compiled directly into native machine code (x86_64 CPU instructions).\n");
    printf("Operating System handles direct binary execution.\n");
    return 0;
}
