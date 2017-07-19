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
package com.dremio.service.coordinator;

import java.util.EnumSet;
import java.util.Set;

import com.dremio.exec.proto.CoordinationProtos.Roles;
import com.dremio.service.Service;

/**
 * Pluggable interface built to manage cluster coordination. Allows SabotNode or DremioClient to register its capabilities
 * as well as understand other node's existence and capabilities.
 **/
public abstract class ClusterCoordinator implements Service {
  /**
   * Cluster coordinator options
   */
  public static final class Options {
    private Options() {}

    public static final String CLUSTER_ID = "dremio.exec.cluster-id";
    public static final String ZK_CONNECTION = "dremio.exec.zk.connect";
    public static final String ZK_RETRY_BASE_DELAY = "dremio.exec.zk.retry.delay.base";
    public static final String ZK_RETRY_MAX_DELAY = "dremio.exec.zk.retry.delay.max";
    public static final String ZK_ROOT = "dremio.exec.zk.root";
    public static final String ZK_TIMEOUT = "dremio.exec.zk.timeout";

  }

  /**
   * Node roles for cluster coordination
   */
  public enum Role {
    COORDINATOR {
      @Override
      protected void updateEndpointRoles(Roles.Builder roles, boolean enable) {
        roles.setSqlQuery(enable);
      }

      @Override
      protected boolean contains(Roles roles) {
        return roles.getSqlQuery();
      }
    },

    EXECUTOR {
      @Override
      protected void updateEndpointRoles(Roles.Builder roles, boolean enable) {
        roles.setJavaExecutor(enable);
      }

      @Override
      protected boolean contains(Roles roles) {
        return roles.getJavaExecutor();
      }
    };

    protected abstract boolean contains(Roles roles);

    protected abstract void updateEndpointRoles(Roles.Builder roles, boolean enable);

    public static Set<Role> fromEndpointRoles(Roles endpointRoles) {
      EnumSet<Role> roles = EnumSet.noneOf(Role.class);
      for(Role role: values()) {
        if (role.contains(endpointRoles)) {
          roles.add(role);
        }
      }

      return roles;
    }

    public static Roles toEndpointRoles(Set<Role> roles) {
      Roles.Builder builder = Roles.newBuilder();
      for(Role role: Role.values()) {
        role.updateEndpointRoles(builder, roles.contains(role));
      }

      return builder.build();
    }
  }

  /**
   * Get a provider which returns the up-to-date list of endpoints for a given role.
   *
   * @param role the role to look up for
   * @return a provider for a collection of endpoints
   * @throws NullPointerException if role is {@code null}
   */
  public abstract ServiceSet getServiceSet(Role role);

  public abstract DistributedSemaphore getSemaphore(String name, int maximumLeases);
}
