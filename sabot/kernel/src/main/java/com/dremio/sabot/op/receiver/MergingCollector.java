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
package com.dremio.sabot.op.receiver;

import org.apache.arrow.memory.BufferAllocator;

import com.dremio.common.config.SabotConfig;
import com.dremio.exec.proto.CoordExecRPC.Collector;
import com.dremio.exec.proto.ExecProtos.FragmentHandle;
import com.dremio.sabot.exec.fragment.FragmentWorkQueue;
import com.dremio.sabot.exec.rpc.TunnelProvider;
import com.dremio.sabot.threads.sharedres.SharedResourceGroup;

public class MergingCollector extends AbstractDataCollector{

  public MergingCollector(SharedResourceGroup resourceGroup, Collector collector, BufferAllocator allocator, SabotConfig config, FragmentHandle handle,
                          FragmentWorkQueue workQueue, TunnelProvider tunnelProvider) {
    super(resourceGroup, false, collector, collector.getIncomingMinorFragmentCount(), allocator, config, handle, workQueue, tunnelProvider);
  }

  @Override
  protected RawBatchBuffer getBuffer(int minorFragmentId) {
    return buffers[0];
  }

}
