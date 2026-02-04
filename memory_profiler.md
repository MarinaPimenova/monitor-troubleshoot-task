# Run memory profiler

See more [Profiling an Application](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/profiler.html)

$ java -jar -Xmx30m -XX:+HeapDumpOnOutOfMemoryError heap/target/heap-1.0-SNAPSHOT.jar
Press any key to proceed
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
WARNING: A Java agent has been loaded dynamically (C:\Program Files\Amazon Corretto\visualvm_22\visualvm\lib\jfluid-server-15.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
Profiler Agent: Waiting for connection on port 5140 (Protocol version: 21)
Profiler Agent: Established connection with the tool
*** Profiler engine warning: Cannot use ClassLoader.findLoadedClass() and/or ClassLoader.findBootstrapClass() in ClassLoaderManager
Profiler Agent: Local accelerated session
