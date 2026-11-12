package com.cover.time2gather.support;

public final class FirestoreTestEnvironment {
    private FirestoreTestEnvironment() { }
    public static String endpoint() {
        String host = System.getenv("FIRESTORE_EMULATOR_HOST");
        if (host == null || host.isBlank()) return TestDatabases.firestore().getEmulatorEndpoint().replace("localhost:", "127.0.0.1:");
        if (!host.matches("(127\\.0\\.0\\.1|localhost):[0-9]+")) {
            throw new IllegalArgumentException("An external test emulator must be local");
        }
        return host;
    }
}
