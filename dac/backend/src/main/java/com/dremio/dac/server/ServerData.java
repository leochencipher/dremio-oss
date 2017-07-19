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

import java.util.Map;

import com.dremio.config.DremioConfig;
import com.dremio.dac.daemon.ServerHealthMonitor;

/**
 * Template parameters for index.html served by IndexServlet
 */
public class ServerData {
  private final String environment;
  private final ServerHealthMonitor healthMonitor;
  private final Map<String, Object> config;
  private final Map<String, Object> debug;
  private final ClientSettings settings;

  public ServerData(String environment, ServerHealthMonitor healthMonitor, DremioConfig dremioConfig, ClientSettings settings) {
    this.environment = environment;
    this.healthMonitor = healthMonitor;
    this.config = dremioConfig.getObject(DremioConfig.WEB_UI_SERVICE_CONFIG).unwrapped();
    this.debug = dremioConfig.getObject(DremioConfig.DEBUG_OPTIONS).unwrapped();
    this.settings = settings;
  }

  public String getEnvironment() {
    return environment;
  }

  public ClientSettings getSettings(){
    return settings;
  }

  public String getStatus() {
    return healthMonitor.getStatus().toString();
  }

  public Map<String, Object> getConfig() {
    return config;
  }

  public Map<String, Object> getDebug() {
    return debug;
  }

  /**
   * Description of settings for ui.
   */
  public static class ClientSettings {
    private final String supportEmailTo;
    private final String supportEmailSubjectForJobs;
    private final Boolean outsideCommunicationDisabled;

    public ClientSettings(String supportEmailTo, String supportEmailSubjectForJobs, Boolean outsideCommunicationDisabled) {
      super();
      this.supportEmailTo = supportEmailTo;
      this.supportEmailSubjectForJobs = supportEmailSubjectForJobs;
      this.outsideCommunicationDisabled = outsideCommunicationDisabled;
    }

    public String getSupportEmailTo() {
      return supportEmailTo;
    }

    public String getSupportEmailSubjectForJobs() {
      return supportEmailSubjectForJobs;
    }

    public Boolean getOutsideCommunicationDisabled() {
      return outsideCommunicationDisabled;
    }
  }
}
