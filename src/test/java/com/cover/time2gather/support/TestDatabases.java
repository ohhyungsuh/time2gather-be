package com.cover.time2gather.support;

import org.testcontainers.gcloud.FirestoreEmulatorContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Lazily start pinned, disposable databases. Missing Docker fails integration tests. */
public final class TestDatabases {
    private TestDatabases() { }

    private static class FirestoreHolder {
        static final FirestoreEmulatorContainer INSTANCE = start();
        private static FirestoreEmulatorContainer start() {
            var container = new FirestoreEmulatorContainer(DockerImageName.parse(
                    "gcr.io/google.com/cloudsdktool/google-cloud-cli:583.0.0-emulators"));
            container.start();
            return container;
        }
    }
    private static class MysqlHolder {
        static final MySQLContainer INSTANCE = start();
        private static MySQLContainer start() {
            var container = new MySQLContainer(DockerImageName.parse("mysql:8.4.7"))
                    .withDatabaseName("time2gather_test").withUsername("test").withPassword("test");
            container.start();
            return container;
        }
    }
    public static FirestoreEmulatorContainer firestore() { return FirestoreHolder.INSTANCE; }
    public static MySQLContainer mysql() { return MysqlHolder.INSTANCE; }
}
