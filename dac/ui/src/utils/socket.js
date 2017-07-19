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
import invariant from 'invariant';
import { WEB_SOCKET_URL } from 'constants/Api';
import localStorageUtils from 'utils/storageUtils/localStorageUtils';

const PING_INTERVAL = 15000;
const CHECK_INTERVAL = 5000;

const WS_MESSAGE_PING = 'ping';
export const WS_MESSAGE_JOB_DETAILS = 'job-details';
export const WS_MESSAGE_JOB_DETAILS_LISTEN = 'job-details-listen';
export const WS_MESSAGE_JOB_PROGRESS = 'job-progress';
export const WS_MESSAGE_JOB_PROGRESS_LISTEN = 'job-progress-listen';

export class Socket {
  dispatch = null;
  _socket = null;
  _listenMessages = {};
  _pingId = 0;
  _checkId = 0;

  open() {
    invariant(!this._socket, 'socket already open');
    invariant(this.dispatch, 'socket requires #dispatch to be assigned');

    this._createConnection();
    this._pingId = setInterval(this._ping, PING_INTERVAL);
    this._checkId = setInterval(this._checkConnection, CHECK_INTERVAL);
  }

  close() {
    if (this._socket) this._socket.close();
    this._socket = null;
    this._listenMessages = {};
    clearInterval(this._pingId);
    clearInterval(this._checkId);
  }

  _createConnection() {
    const authToken = localStorageUtils && localStorageUtils.getAuthToken();
    this._socket = new WebSocket(WEB_SOCKET_URL, [`_dremio${authToken}`]);
    this._socket.onopen = this._handleConnectionEstablished;
    this._socket.onerror = this._handleConnectionError;
    this._socket.onmessage = this._handleMessage;
  }

  _checkConnection = () => {
    if (this._socket.readyState === WebSocket.CLOSED) {
      this._createConnection();
    }
  }

  _handleConnectionError = (e) => {
    console.error('SOCKET CONNECTION ERROR', e);
  }

  _handleConnectionEstablished = () => {
    console.info('SOCKET CONNECTED START');

    const keys = Object.keys(this._listenMessages);
    for (let i = 0; i < keys.length; i++) {
      this._sendMessage(this._listenMessages[keys[i]].message);
    }
  }

  _handleMessage = (e) => {
    try {
      const data = JSON.parse(e.data);
      if (data.type === 'connection-established') {
        console.info('SOCKET CONNECTED SUCCESS');
      } else {
        console.info(data);
      }
      this.dispatch({type: data.type, payload: data.payload});
    } catch (error) {
      console.error('SOCKET MESSAGE HANDLING ERROR', error);
    }
  }

  _ping = () => {
    this._sendMessage({type: WS_MESSAGE_PING, payload: {}});
  }

  sendListenMessage(message, forceSend) {
    const messageKey = message.type + '-' + message.payload.id;
    if (!this._listenMessages[messageKey]) {
      this._listenMessages[messageKey] = {
        message,
        listenCount: 1
      };
      this._sendMessage(message);
    } else {
      this._listenMessages[messageKey].listenCount++;
      if (forceSend) {
        this._sendMessage(message);
      }
    }
  }

  stopListenMessage(message) {
    const messageKey = message.type + '-' + message.payload.id;
    if (this._listenMessages[messageKey]) {
      this._listenMessages[messageKey].listenCount--;
      if (!this._listenMessages[messageKey].listenCount) {
        delete this._listenMessages[messageKey];
      }
    }
  }

  startListenToJobChange(jobId, forceSend) {
    invariant(jobId, `Must provide jobId to listen to. Received ${jobId}`);
    const message = {
      type: WS_MESSAGE_JOB_DETAILS_LISTEN,
      payload: {
        id: jobId
      }
    };
    this.sendListenMessage(message, forceSend);
  }

  stopListenToJobChange(jobId) {
    invariant(jobId, `Must provide jobId to stop listen to. Received ${jobId}`);
    const message = {
      type: WS_MESSAGE_JOB_DETAILS_LISTEN,
      payload: {
        id: jobId
      }
    };
    this.stopListenMessage(message);
  }

  startListenToJobProgress(jobId, forceSend) {
    invariant(jobId, `Must provide jobId to listen to. Received ${jobId}`);
    const message = {
      type: WS_MESSAGE_JOB_PROGRESS_LISTEN,
      payload: {
        id: jobId
      }
    };
    this.sendListenMessage(message, forceSend);
  }

  stopListenToJobProgress(jobId) {
    invariant(jobId, `Must provide jobId to stop listen to. Received ${jobId}`);
    const message = {
      type: WS_MESSAGE_JOB_PROGRESS_LISTEN,
      payload: {
        id: jobId
      }
    };
    this.stopListenMessage(message);
  }

  _sendMessage(message) {
    if (this._socket.readyState === WebSocket.OPEN) {
      this._socket.send(JSON.stringify(message));
    }
  }
}

export default new Socket();
