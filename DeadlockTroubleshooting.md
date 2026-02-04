## Deadlock troubleshooting
#### Get deadlock
- Execute java application that simulates deadlock:

```
    java -jar deadlock-1.0.0-SNAPSHOT.jar
```

- Get thread dump and locate lines similar to:

```
Found one Java-level deadlock:
=============================
"Thread 2":
  waiting to lock monitor 0x000000001bf40b68 (object 0x000000076b7777c8, a java.lang.Object),
  which is held by "Thread 1"
"Thread 1":
  waiting to lock monitor 0x000000001bf43608 (object 0x000000076b7777d8, a java.lang.Object),
  which is held by "Thread 2"

Java stack information for the threads listed above:
===================================================
"Thread 2":
        at com.epam.jmp.mat.deadlock.SimulateDeadLock.method2(SimulateDeadLock.java:44)
        - waiting to lock <0x000000076b7777c8> (a java.lang.Object)
        - locked <0x000000076b7777d8> (a java.lang.Object)
        at com.epam.jmp.mat.deadlock.DeadLockMain$2.run(DeadLockMain.java:18)
"Thread 1":
        at com.epam.jmp.mat.deadlock.SimulateDeadLock.method1(SimulateDeadLock.java:24)
        - waiting to lock <0x000000076b7777d8> (a java.lang.Object)
        - locked <0x000000076b7777c8> (a java.lang.Object)
        at com.epam.jmp.mat.deadlock.DeadLockMain$1.run(DeadLockMain.java:11)

Found 1 deadlock.
```

#### Get thread dump
1} jstack
```
    jstack -l <pid>
```
2} kill -3
```
    kill -3 <pid>
```
3} jvisualvm

4} Windows (Ctrl + Break)

5} jcmd
```
    jcmd <pid> Thread.print
```

## Report about

```shell
java -jar deadlock/target/deadlock-1.0-SNAPSHOT.jar
```

The result is
```shell
AzureAD@XPLTVILW009E MINGW64 ~/sb-projects/java-advanced-core-course-2026/monitor-troubleshoot-task (feature/Task_3_monitor)
$ java -jar deadlock/target/deadlock-1.0-SNAPSHOT.jar
1.1 Thread 01 got lock 1
2.1 Thread 02 got lock 2
1.2 Thread 01 waiting for lock 2
2.2 Thread 02 waiting for lock 1

```
Java stack information for the threads listed above:
===================================================
"Thread 1":
at com.hw.deadlock.SimulateDeadLock.method1(SimulateDeadLock.java:24)
- waiting to lock <0x0000000626218750> (a java.lang.Object)
- locked <0x0000000626218740> (a java.lang.Object)
at com.hw.deadlock.DeadLockMain$1.run(DeadLockMain.java:12)
"Thread 2":
at com.hw.deadlock.SimulateDeadLock.method2(SimulateDeadLock.java:44)
- waiting to lock <0x0000000626218740> (a java.lang.Object)
- locked <0x0000000626218750> (a java.lang.Object)
at com.hw.deadlock.DeadLockMain$2.run(DeadLockMain.java:19)

Found 1 deadlock.
## 1️⃣ What the console output tells us

```text
1.1 Thread 01 got lock 1
2.1 Thread 02 got lock 2
1.2 Thread 01 waiting for lock 2
2.2 Thread 02 waiting for lock 1
```

This already describes the deadlock pattern:

| Thread   | Holds  | Waiting for |
| -------- | ------ | ----------- |
| Thread 1 | lock 1 | lock 2      |
| Thread 2 | lock 2 | lock 1      |

Each thread owns one lock and is waiting for the other → **circular wait**.

---

## 2️⃣ JVM deadlock report explained

The JVM detects the deadlock and prints:

```text
Found 1 deadlock.
```

Let’s break down the stack traces.

---

## 3️⃣ Thread 1 analysis

```text
"Thread 1":
at com.hw.deadlock.SimulateDeadLock.method1(SimulateDeadLock.java:24)
- waiting to lock <0x0000000626218750> (a java.lang.Object)
- locked <0x0000000626218740> (a java.lang.Object)
```

Meaning:

* Thread 1 **already holds** lock `0x...8740` (lock 1)
* It is **blocked**, waiting to acquire lock `0x...8750` (lock 2)
* The thread is stuck inside `method1`

In plain English:

> Thread 1 entered `method1`, acquired lock 1, and then tried to acquire lock 2 — but lock 2 is already taken.

---

## 4️⃣ Thread 2 analysis

```text
"Thread 2":
at com.hw.deadlock.SimulateDeadLock.method2(SimulateDeadLock.java:44)
- waiting to lock <0x0000000626218740> (a java.lang.Object)
- locked <0x0000000626218750> (a java.lang.Object)
```

Meaning:

* Thread 2 **already holds** lock `0x...8750` (lock 2)
* It is **blocked**, waiting to acquire lock `0x...8740` (lock 1)
* The thread is stuck inside `method2`

In plain English:

> Thread 2 entered `method2`, acquired lock 2, and then tried to acquire lock 1 — but lock 1 is already taken.

---

## 5️⃣ Why this is a deadlock (formal conditions)

All **four deadlock conditions** are met:

1. **Mutual exclusion**
   Locks are `synchronized` → only one thread can hold each lock.

2. **Hold and wait**
   Each thread holds one lock and waits for another.

3. **No preemption**
   JVM cannot forcibly take a monitor lock from a thread.

4. **Circular wait**
   Thread 1 → lock 2 → Thread 2 → lock 1 → Thread 1

Once all four occur, the JVM cannot make progress.

---

## 6️⃣ Object IDs in the report

```text
<0x0000000626218740>
<0x0000000626218750>
```

These are **monitor identities** for the two `java.lang.Object` instances used as locks.

They help tools like:

* `jstack`
* VisualVM
* JMC

to correlate which threads are fighting over the same monitors.

---

## 7️⃣ Root cause in the code (conceptually)

Your code likely looks like this pattern:

```java
// Thread 1
synchronized (lock1) {
    synchronized (lock2) {
        ...
    }
}

// Thread 2
synchronized (lock2) {
    synchronized (lock1) {
        ...
    }
}
```

The **lock acquisition order is inconsistent**, which is the direct cause of the deadlock.

---

## 8️⃣ Why the program never finishes

* Both threads are in `BLOCKED` state
* Neither can release its lock
* JVM threads are non-daemon → JVM stays alive
* Only external interruption (kill process) ends it

---

## 9️⃣ Key takeaway (Java developer view)

> **This is a classic monitor deadlock caused by inconsistent lock ordering: each thread acquires the same locks in a different order, creating a circular wait that the JVM correctly detects and reports.**

---


