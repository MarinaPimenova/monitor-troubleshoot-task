## OutOfMemory (OOM) error troubleshooting
#### Get OOM error
Execute and press any key:
```
    java -jar -Xmx100m heap-1.0.0-SNAPSHOT.jar
```

#### Use jvisualvm to observe OOM
- Execute:

```
    java -jar -Xmx100m heap-1.0.0-SNAPSHOT.jar
```
- In jvisualvm connect to our java process
- Go to "Monitor" tab
- Press any key in our application
- Observe how heap grows

#### Get heap dump
##### Using -XX:+HeapDumpOnOutOfMemoryError option
- Execute and press any key:

```
    java -jar -Xmx100m -XX:+HeapDumpOnOutOfMemoryError heap-1.0.0-SNAPSHOT.jar
```

##### [Optional] Using jcmd
Get pid using `jps` here and further through this document:
```
    jps -lvm
```
```
    jcmd <pid> GC.heap_dump <filename>
```

##### [Optional] Using jmap
```
    jmap -dump:format=b,file=snapshot.hprof <pid>
```

#### Get heap histogram
##### Using jcmd
```
    jcmd <pid> GC.class_histogram
```
##### Using jmap
```
    jmap -histo <pid> 
```

#### Analyze heap dump
##### Using Java Visual VM
- Open retrieved heap dump in jvisualvm
- Identify memory leak

##### OQL
Execute OQL in jvisualvm:
```
    select objs from java.lang.Object[] objs where objs.length > 100
    select referrers(objs) from java.lang.Object[] objs where objs.length > 100
    select referrers(arr) from java.util.ArrayList arr where arr.size > 100
```
Startup `jhat` (note: `jhat` was decommissioned in JDK 9)
```
    jhat <head_dump.hprof>
```
Execute OQL in jhat
```
    select [objs, objs.length] from [Ljava.lang.Object; objs where objs.length > 100
    select referrers(objs) from [Ljava.lang.Object; objs where objs.length > 100
    select referrers(arr) from java.util.ArrayList arr where arr.size > 100
```
Please note small OQL syntax difference in jhat and jvisualvm.

## Report about Execute OQL in jvisualvm
```shell
visualvm.exe --jdkhome "C:\Program Files\Amazon Corretto\jdk21.0.7_6" --console suppres
```

```shell
java -jar -Xmx100m -XX:+HeapDumpOnOutOfMemoryError heap/target/heap-1.0-SNAPSHOT.jar
```

### select objs from java.lang.Object[] objs where objs.length > 100
### What it asks for

This query searches the **heap dump** and returns:

> **All object arrays (`Object[]`) whose length is greater than 100 elements**

Important clarifications:

* `java.lang.Object[]` means **any object array**, including:

    * `Object[]`
    * `String[]`
    * `Foo[]`
    * `List[]`
* Primitive arrays like `int[]`, `byte[]`, `char[]` are **NOT included**
* `objs.length` is the **array length**, not memory size in bytes

---

## How to read the result table

Each row like this:

```
[] java.lang.Object[][#3518]: 2,126 items
```

means:

### 1️⃣ `[] java.lang.Object[]`

* This is an **array instance**
* The `[]` indicates it’s an array
* Element type is `java.lang.Object` (or a subclass at runtime)

---

### 2️⃣ `[#3518]`

* This is the **heap object ID**
* Used internally by VisualVM
* Helps distinguish different array instances of the same type

---

### 3️⃣ `2,126 items`

* This is the **array length**
* Equivalent to:

  ```java
  objs.length == 2126
  ```

---

### 4️⃣ `Size` column (e.g. `8,520 B`)

* Memory used by the array object itself:

    * Array header
    * References to elements
* **Does NOT include memory used by the referenced objects**

Example:

```text
Object[] (2126 references) ≈ 8.5 KB
```

---

### 5️⃣ `Retained` 

* You need to click **“GC Roots”** or **“Compute retained sizes”**
* Retained size answers:

  > “How much memory would be freed if this array were garbage collected?”

---

## What this tells you about your application

This result means:

* Your heap contains **multiple large object arrays**
* Some arrays contain **thousands of object references**
* Typical causes:

    * Large `ArrayList` backing arrays
    * Cached results
    * Batching logic
    * Framework internals (e.g. Spring, Hibernate, Netty)

Example mapping:

```java
ArrayList<?> list;
// internally backed by:
Object[] elementData;
```

If `list.size() > 100`, it will appear here.

---

## How to investigate further

### Inspect array contents

* Click an array → **Preview / Fields**
* See what objects it references

### Find who owns it

* Use **References** or **Paths to GC Roots**
* Identify leaks or long-lived caches

### Improve the query

Examples:

```sql
-- Show arrays with their length
select {array: objs, length: objs.length}
from java.lang.Object[] objs
where objs.length > 100
```

```sql
-- Focus on very large arrays
where objs.length > 1000
```

---

## One-line summary

> **This OQL query lists all object arrays in the heap whose element count exceeds 100, helping you identify large collections or potential memory hotspots.**



### select referrers(objs) from java.lang.Object[] objs where objs.length > 100
**Short explanation (Java / VisualVM OQL view):**

```sql
select referrers(objs)
from java.lang.Object[] objs
where objs.length > 10
```

**Meaning of the result you see:**

* The query finds **object arrays (`Object[]`) with more than 10 elements**
* It then lists **the objects that reference those arrays** (their owners)
* In the result, you see classes like:

    * `java.util.ImmutableCollections$ListN`
    * `java.util.ImmutableCollections$SetN`
    * `java.util.ArrayList`

**What this tells you:**

* These arrays are **backing arrays** of Java collections
* They are kept alive by **collection objects**, not by themselves
* The retained sizes indicate how much memory would be freed if the **owning collection** were garbage-collected

**Key takeaway:**

> **The result shows which collection objects (often JDK immutable collections or `ArrayList`s) are holding references to object arrays larger than 10 elements—this is normal heap structure, not a leak by itself.**

### select referrers(arr) from java.util.ArrayList arr where arr.size > 100
---
It filters the heap for all java.util.ArrayList instances containing more than 100 elements and then identifies the objects that hold a reference to those lists.
Breakdown of the Results

Looking at your VisualVM output, the query has flagged two primary "culprits" (referrers):

    jdk.jfr.internal.MetadataRepository: This indicates that Java Flight Recorder (JFR) is currently holding a large list. This is often normal system overhead if you have profiling active.

    com.hw.heap.Process#1: This is a custom class. The tree shows it holds a reference to an ArrayList#4.

        GC Root Note: The [GC root - Java frame] tag is critical. It means this object is currently "alive" because it’s being referenced by a local variable in a running thread's stack.

Why this matters

From a memory management perspective, you use this to:

    Identify Memory Leaks: If you see your own custom classes (like Process) holding large lists that should have been cleared, you've found a leak.

    Analyze Retained Size: Note the Retained column (e.g., 89,760 B for the MetadataRepository). This tells you how much memory would be freed if that specific referrer was garbage collected.

    Note: The arr.size in OQL refers to the number of elements currently stored in the list, not the capacity of the underlying backing array.