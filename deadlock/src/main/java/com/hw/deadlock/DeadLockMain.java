package com.hw.deadlock;


public class DeadLockMain {

    public static void main(String[] args) {
        final SimulateDeadLock simulateDeadLock = new SimulateDeadLock();

        new Thread("Thread 1") {
            @Override
            public void run() {
                simulateDeadLock.method1();
            }
        }.start();

        new Thread("Thread 2") {
            @Override
            public void run() {
                simulateDeadLock.method2();
            }
        }.start();
    }

}

