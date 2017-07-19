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
import { CALL_API } from 'redux-api-middleware';
import { API_URL_V2 } from 'constants/Api';
import { arrayOf } from 'normalizr';

import {makeUncachebleURL} from 'ie11.js';

import schemaUtils from 'utils/apiUtils/schemaUtils';
import sourceSchema from 'dyn-load/schemas/source';

import sourcesMapper from 'utils/mappers/sourcesMapper';

export const ADD_NEW_SOURCE_START = 'ADD_NEW_SOURCE_START';
export const ADD_NEW_SOURCE_SUCCESS = 'ADD_NEW_SOURCE_SUCCESS';
export const ADD_NEW_SOURCE_FAILURE = 'ADD_NEW_SOURCE_FAILURE';

function postCreateSource(sourceModel) {
  const meta = {
    source: Immutable.fromJS(sourceModel).merge({resourcePath: `/source/${sourceModel.name}`}),
    invalidateViewIds: ['AllSources']
  };
  return {
    [CALL_API]: {
      types: [
        { type: ADD_NEW_SOURCE_START, meta},
        schemaUtils.getSuccessActionTypeWithSchema(ADD_NEW_SOURCE_SUCCESS, sourceSchema, meta),
        { type: ADD_NEW_SOURCE_FAILURE, meta}
      ],
      method: 'PUT',
      headers: {'Content-Type': 'application/json'},
      body: JSON.stringify(sourceModel),
      endpoint: makeUncachebleURL(`${API_URL_V2}/source/${encodeURIComponent(sourceModel.name)}`)
    }
  };
}

export function createSource(data, type) {
  const sourceModel = sourcesMapper.newSource(type, data);
  return (dispatch) => {
    return dispatch(postCreateSource(sourceModel));
  };
}

export const SOURCES_LIST_LOAD_START = 'SOURCES_LIST_LOAD_START';
export const SOURCES_LIST_LOAD_SUCCESS = 'SOURCES_LIST_LOAD_SUCCESS';
export const SOURCES_LIST_LOAD_FAILURE = 'SOURCES_LIST_LOAD_FAILURE';

function fetchSourceListData() {
  const meta = {viewId: 'AllSources', mergeEntities: true};
  return {
    [CALL_API]: {
      types: [
        {type: SOURCES_LIST_LOAD_START, meta},
        schemaUtils.getSuccessActionTypeWithSchema(SOURCES_LIST_LOAD_SUCCESS, { sources: arrayOf(sourceSchema) }, meta),
        {type: SOURCES_LIST_LOAD_FAILURE, meta}
      ],
      method: 'GET',
      endpoint: API_URL_V2 + makeUncachebleURL('/sources')
    }
  };
}

export function loadSourceListData() {
  return (dispatch) => {
    return dispatch(fetchSourceListData());
  };
}

export const REMOVE_SOURCE_START = 'REMOVE_SOURCE_START';
export const REMOVE_SOURCE_SUCCESS = 'REMOVE_SOURCE_SUCCESS';
export const REMOVE_SOURCE_FAILURE = 'REMOVE_SOURCE_FAILURE';

function fetchRemoveSource(source) {
  const name = source.get('name');
  const meta = {
    name,
    invalidateViewIds: ['AllSources']
  };
  return {
    [CALL_API]: {
      types: [
        {
          type: REMOVE_SOURCE_START, meta
        },
        {
          type: REMOVE_SOURCE_SUCCESS,
          meta: {...meta, success: true}
        },
        {
          type: REMOVE_SOURCE_FAILURE, meta
        }
      ],
      method: 'DELETE',
      endpoint: makeUncachebleURL(`${API_URL_V2}${source.getIn(['links', 'self'])}?version=${source.get('version')}`)
    }
  };
}

export function removeSource(source) {
  return (dispatch) => {
    return dispatch(fetchRemoveSource(source));
  };
}

export const RENAME_SOURCE_START = 'RENAME_SOURCE_START';
export const RENAME_SOURCE_SUCCESS = 'RENAME_SOURCE_SUCCESS';
export const RENAME_SOURCE_FAILURE = 'RENAME_SOURCE_FAILURE';

function fetchRenameSource(oldName, newName) {
  return {
    [CALL_API]: {
      types: [
        {
          type: RENAME_SOURCE_START,
          meta: { oldName, newName }
        },
        {
          type: RENAME_SOURCE_SUCCESS,
          meta: { oldName }
        },
        {
          type: RENAME_SOURCE_FAILURE,
          meta: { oldName }
        }
      ],
      method: 'POST',
      endpoint: `${API_URL_V2}/source/${oldName}/rename?renameTo=${newName}`
    }
  };
}

export function renameSource(oldName, newName) {
  return (dispatch) => {
    return dispatch(fetchRenameSource(oldName, newName)).then(() => {
      return dispatch(fetchSourceListData());
    });
  };
}

export const SET_SOURCE_PIN_STATE = 'SET_SOURCE_PIN_STATE';

export function setSourcePin(sourceName, isActivePin) {
  return (dispatch) => {
    dispatch({
      type: SET_SOURCE_PIN_STATE,
      name: sourceName,
      isActivePin
    });
  };
}

export const GET_CREATED_SOURCE_START = 'GET_CREATED_SOURCE_START';
export const GET_CREATED_SOURCE_SUCCESS = 'GET_CREATED_SOURCE_SUCCESS';
export const GET_CREATED_SOURCE_FAILURE = 'GET_CREATED_SOURCE_FAILURE';

export function fetchSource(sourceName, viewId) {
  const meta = {viewId};
  return {
    [CALL_API]: {
      types: [
        {type: GET_CREATED_SOURCE_START, meta},
        {type: GET_CREATED_SOURCE_SUCCESS, meta},
        {type: GET_CREATED_SOURCE_FAILURE, meta}
      ],
      method: 'GET',
      endpoint: `${API_URL_V2}/source/${sourceName}?includeContents=false`
    }
  };
}

export function loadSource(sourceName, viewId) {
  return (dispatch) => {
    return dispatch(fetchSource(sourceName, viewId));
  };
}
