package com.hw.deadlock;

class SimulateDeadLock {

    private final Object lock1 = new Object();

    private final Object lock2 = new Object();

    /**
     * 1. Acquire lock1
     * 2. Acquire lock2
     */
    void method1() {
        synchronized(lock1) {
            System.out.println("1.1 Thread 01 got lock 1");
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                System.out.println("Thread 01 InterruptedException occurred");
            }
            System.out.println("1.2 Thread 01 waiting for lock 2");
            synchronized(lock2) {
                System.out.println("Thread 01 got lock 2");
            }
        }
    }

    /**
     * 1. Acquire lock2
     * 2. Acquire lock1
     */
    void method2() {
        synchronized(lock2) {
            System.out.println("2.1 Thread 02 got lock 2");
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                System.out.println("Thread 02 InterruptedException occurred");
            }
            System.out.println("2.2 Thread 02 waiting for lock 1");
            synchronized(lock1) {
                System.out.println("Thread 02 got lock 1");
            }
        }
    }
}

