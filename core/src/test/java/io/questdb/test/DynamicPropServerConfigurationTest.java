/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.test;

import io.questdb.*;
import io.questdb.std.FilesFacadeImpl;
import org.junit.Assert;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DynamicPropServerConfigurationTest extends AbstractTest {

    @Test
    public void TestPgWireCredentialsReloadByDeletingProp() throws Exception {
        Path serverConfPath = Path.of(temp.getRoot().getAbsolutePath(), "dbroot", "conf", "server.conf");
        Files.createDirectories(serverConfPath.getParent());
        File serverConf = serverConfPath.toFile();
        try (FileWriter w = new FileWriter(serverConf)) {
            w.write("pg.user=steven\n");
            w.write("pg.password=sklar\n");
        }

        try (ServerMain serverMain = new ServerMain(getBootstrap())) {
            serverMain.start();

            try (Connection conn = getConnection("steven", "sklar")) {
                Assert.assertFalse(conn.isClosed());
            }

            // Overwrite file to remove props
            try (FileWriter w = new FileWriter(serverConf, false)) {
                w.write("\n");
            }

            // todo: Somehow synchronize the config reloader callback with this test
            Thread.sleep(2000);
            try (Connection conn = getConnection("admin", "quest")) {
                Assert.assertFalse(conn.isClosed());
            }

        }
    }

    @Test
    public void TestPgWireCredentialsReloadWithChangedProp() throws Exception {
        Path serverConfPath = Path.of(temp.getRoot().getAbsolutePath(), "dbroot", "conf", "server.conf");
        Files.createDirectories(serverConfPath.getParent());
        File serverConf = serverConfPath.toFile();
        try (FileWriter w = new FileWriter(serverConf)) {
            w.write("pg.user=steven\n");
            w.write("pg.password=sklar\n");
        }


        try (ServerMain serverMain = new ServerMain(getBootstrap())) {
            serverMain.start();

            try (Connection conn = getConnection("steven", "sklar")) {
                Assert.assertFalse(conn.isClosed());
            }

            try (FileWriter w = new FileWriter(serverConf)) {
                w.write("pg.user=nevets\n");
                w.write("pg.password=ralks\n");
            }

            // todo: Somehow synchronize the config reloader callback with this test
            Thread.sleep(2000);
            try (Connection conn = getConnection("nevets", "ralks")) {
                Assert.assertFalse(conn.isClosed());
            }

            Assert.assertThrows(PSQLException.class, () -> getConnection("admin", "quest"));

        }
    }

    @Test
    public void TestPgWireCredentialsReloadWithChangedPropAfterRecreatedFile() throws Exception {
        Path serverConfPath = Path.of(temp.getRoot().getAbsolutePath(), "dbroot", "conf", "server.conf");
        try (ServerMain serverMain = new ServerMain(getBootstrap())) {
            serverMain.start();

            try (Connection conn = getConnection("admin", "quest")) {
                Assert.assertFalse(conn.isClosed());
            }

            File serverConf = serverConfPath.toFile();
            Assert.assertTrue(serverConf.delete());
            Assert.assertTrue(serverConf.createNewFile());

            try (FileWriter w = new FileWriter(serverConf)) {
                w.write("pg.user=steven\n");
                w.write("pg.password=sklar\n");
            }
            Thread.sleep(2000);

            try (Connection conn = getConnection("steven", "sklar")) {
                Assert.assertFalse(conn.isClosed());
            }

            Assert.assertThrows(PSQLException.class, () -> getConnection("admin", "quest"));
        }
    }

    @Test
    public void TestPgWireCredentialsReloadWithNewProp() throws Exception {
        try (ServerMain serverMain = new ServerMain(getBootstrap())) {

            Path serverConfPath = Path.of(temp.getRoot().getAbsolutePath(), "dbroot", "conf", "server.conf");
            File serverConf = serverConfPath.toFile();
            Assert.assertTrue(serverConf.exists());

            serverMain.start();

            try (Connection conn = getConnection("admin", "quest")) {
                Assert.assertFalse(conn.isClosed());
            }

            try (FileWriter w = new FileWriter(serverConf)) {
                w.write("pg.user=steven\n");
                w.write("pg.password=sklar\n");
            }

            // todo: Somehow synchronize the config reloader callback with this test
            Thread.sleep(2000);

            try (Connection conn = getConnection("steven", "sklar")) {
                Assert.assertFalse(conn.isClosed());
            }

            Assert.assertThrows(PSQLException.class, () -> getConnection("admin", "quest"));

        }
    }

    public Bootstrap getBootstrap() {
        return new Bootstrap(
                getBootstrapConfig(),
                Bootstrap.getServerMainArgs(root)
        );
    }

    public BootstrapConfiguration getBootstrapConfig() {
        return new DefaultBootstrapConfiguration() {
            @Override
            public ServerConfiguration getServerConfiguration(Bootstrap bootstrap) {
                try {
                    return new DynamicPropServerConfiguration(
                            bootstrap.getRootDirectory(),
                            bootstrap.loadProperties(),
                            getEnv(),
                            bootstrap.getLog(),
                            bootstrap.getBuildInformation(),
                            FilesFacadeImpl.INSTANCE,
                            bootstrap.getMicrosecondClock(),
                            FactoryProviderFactoryImpl.INSTANCE,
                            true,
                            SynchronizedConfigReloader::new
                    );
                } catch (Exception exc) {
                    Assert.fail(exc.getMessage());
                    return null;
                }
            }
        };
    }

    private static Connection getConnection(String user, String pass) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", pass);
        final String url = String.format("jdbc:postgresql://127.0.0.1:%d/qdb", 8812);
        return DriverManager.getConnection(url, properties);
    }

    private class SynchronizedConfigReloader implements FileEventCallback {
        private ConfigReloader delegate;

        public SynchronizedConfigReloader(DynamicServerConfiguration config) {
            try {
                delegate = new ConfigReloader(config);
            } catch (IOException exc) {
                Assert.fail(exc.getMessage());
            }

        }

        @Override
        public void onFileEvent() {
            if (delegate == null) {
                Assert.fail();
            }

            delegate.onFileEvent();
            // todo: synchronize here

        }

    }
}