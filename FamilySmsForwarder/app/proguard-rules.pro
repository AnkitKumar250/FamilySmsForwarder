# Keep Room entities and DAOs
-keep class com.familysms.forwarder.data.local.** { *; }

# Keep BroadcastReceivers and Workers instantiated via reflection
-keep class com.familysms.forwarder.receiver.** { *; }
-keep class com.familysms.forwarder.worker.** { *; }
