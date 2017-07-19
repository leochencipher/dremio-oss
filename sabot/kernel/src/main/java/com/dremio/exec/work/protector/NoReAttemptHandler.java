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
package com.dremio.exec.work.protector;

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.work.AttemptId;
import com.dremio.proto.model.attempts.AttemptReason;
import com.dremio.sabot.op.screen.QueryWritableBatch;

/**
 * Re-attempt handler that doesn't support re-attempts
 */
class NoReAttemptHandler implements ReAttemptHandler {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ReAttemptHandler.class);

  @Override
  public void newAttempt() {}

  @Override
  public boolean hasOOM() {
    throw new IllegalStateException("should not be called!");
  }

  @Override
  public AttemptReason isRecoverable(final ReAttemptContext context) {
    logger.info("{}: cannot re-attempt the query, re-attempts are disabled", context.getAttemptId());
    return AttemptReason.NONE;
  }

  @Override
  public QueryWritableBatch convertIfNecessary(QueryWritableBatch result) {
    return result;
  }
}
