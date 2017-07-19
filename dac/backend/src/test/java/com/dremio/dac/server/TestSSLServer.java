/*
 * Copyright (C) 2017 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.dac.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.dremio.common.AutoCloseables;
import com.dremio.dac.daemon.DACDaemon;
import com.dremio.services.fabric.FabricServiceImpl;
import com.dremio.test.DremioTest;

/**
 * Test starting the DAC server with SSL/HTTPS enabled.
 */
public class TestSSLServer extends BaseClientUtils {
  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  private static DACDaemon currentDremioDaemon;
  private static Client client;
  private static WebTarget apiV2;

  @BeforeClass
  public static void setup() throws Exception {
    final String hostname = FabricServiceImpl.getAddress(false);
    final String localWritePathString = tempFolder.getRoot().getAbsolutePath();
    currentDremioDaemon = DACDaemon.newDremioDaemon(
        DacConfig
            .newDebugConfig(DremioTest.DEFAULT_SABOT_CONFIG)
            .masterNode(hostname)
            .autoPort(true)
            .allowTestApis(true)
            .serveUI(false)
            .webSSLEnabled(true)
            .inMemoryStorage(true)
            .addDefaultUser(true)
            .writePath(localWritePathString)
            .clusterMode(DACDaemon.ClusterMode.LOCAL),
            DremioTest.CLASSPATH_SCAN_RESULT
    );

    currentDremioDaemon.init();
    initClient(hostname);

    final Path keyStoreDirectory = Paths.get(localWritePathString,
      HttpsConnectorGenerator.KEY_STORE_DIRECTORY);
    Assert.assertTrue(Files.exists(keyStoreDirectory));
    Assert.assertEquals(Files.getPosixFilePermissions(keyStoreDirectory),
      HttpsConnectorGenerator.KEY_STORE_DIRECTORY_PERMISSIONS);

    final Path keyStorePath = Paths.get(localWritePathString,
      HttpsConnectorGenerator.KEY_STORE_DIRECTORY, HttpsConnectorGenerator.KEY_STORE_FILE);
    Assert.assertTrue(Files.exists(keyStorePath));
    Assert.assertEquals(Files.getPosixFilePermissions(keyStorePath),
      HttpsConnectorGenerator.KEY_STORE_FILE_PERMISSIONS);

    final Path trustStorePath = Paths.get(localWritePathString, HttpsConnectorGenerator.TRUST_STORE_FILE);
    Assert.assertTrue(Files.exists(trustStorePath));
    Assert.assertEquals(Files.getPosixFilePermissions(trustStorePath),
      HttpsConnectorGenerator.TRUST_STORE_FILE_PERMISSIONS);
  }

  protected static void initClient(final String hostname) {
    client = ClientBuilder.newBuilder()
        .trustStore(currentDremioDaemon.getWebServer().getTrustStore())
        .register(MultiPartFeature.class)
        .build();

    apiV2 = client
        .target(String.format("https://%s:%d", hostname, currentDremioDaemon.getWebServer().getPort()))
        .path("apiv2");
  }

  @Test
  public void basic() throws Exception {
    expectSuccess(apiV2.path("server_status").request(MediaType.APPLICATION_JSON_TYPE).buildGet());
  }

  @AfterClass
  public static void shutdown() throws Exception {
    AutoCloseables.close(
        new AutoCloseable() {
          @Override
          public void close() throws Exception {
            client.close();
          }
        },
        currentDremioDaemon
    );
  }
}
