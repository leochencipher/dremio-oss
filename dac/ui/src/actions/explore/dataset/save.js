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
import { push } from  'react-router-redux';
import flatten from 'lodash/flatten';
import invariant from 'invariant';

import exploreUtils from 'utils/explore/exploreUtils';

import fullDatasetSchema from 'schemas/v2/fullDataset';
import { constructFullPath } from 'utils/pathUtils';
import apiUtils from 'utils/apiUtils/apiUtils';
import { performNextAction } from 'actions/explore/nextAction';

import { postDatasetOperation, navigateToNextDataset } from './common';
import { navigateAfterReapply } from './reapply';


export function saveDataset(dataset, viewId, nextAction) {
  return (dispatch) => {
    if (dataset.getIn(['fullPath', 0]) === 'tmp') {
      return dispatch(saveAsDataset(nextAction));
    }
    return dispatch(submitSaveDataset(dataset, viewId)).then((response) => {
      if (!response.error) {
        return dispatch(afterSaveDataset(response, nextAction));
      }
      return response;
    });
  };
}

export function saveAsDataset(nextAction, message) {
  return (dispatch, getStore) => {
    const location = getStore().routing.locationBeforeTransitions;
    return dispatch(push({
      ...location,
      state: {modal: 'SaveAsDatasetModal', nextAction, message}
    }));
  };
}

export function submitSaveDataset(dataset, viewId) {
  return (dispatch) => {
    const savedVersion = dataset.get('version');
    const link = dataset.getIn(['apiLinks', 'self']);
    const href = `${link}/save?savedVersion=${savedVersion}`;
    return dispatch(postDatasetOperation({
      href,
      viewId,
      schema: fullDatasetSchema,
      metas: [{}, {mergeEntities: true}], // Save returns dataset and history only, so need to merge fullDataset
      notificationMessage: la('Successfully saved.')
    }));
  };
}

export function submitSaveAsDataset(name, fullPath, location, reapply) {
  return (dispatch) => {
    const routeParams = {
      resourceId: location.pathname.split('/')[2],
      tableId: location.pathname.split('/')[3]
    };
    const link = exploreUtils.getHrefForTransform(routeParams, location);
    const href = `${link}/${reapply ?
      'reapplyAndSave' : 'save'}?as=${constructFullPath(fullPath.concat(name), false, true)}`;
    return dispatch(postDatasetOperation({
      href,
      schema: fullDatasetSchema,
      notificationMessage: la('Successfully saved.'),
      metas: [{}, {mergeEntities: true}]
    }));
  };
}

export function submitReapplyAndSaveAsDataset(name, fullPath, location) {
  return (dispatch) => {
    const routeParams = {
      resourceId: location.pathname.split('/')[2],
      tableId: location.pathname.split('/')[3]
    };

    const link = exploreUtils.getHrefForTransform(routeParams, location);
    const href = `${link}/reapplyAndSave?as=${constructFullPath(fullPath.concat(name), false, true)}`;
    return dispatch(postDatasetOperation({
      href,
      schema: fullDatasetSchema,
      notificationMessage: la('Successfully saved.'),
      metas: [{}, {mergeEntities: true}]
    })).then((response) => {
      if (!response.error) {
        dispatch(navigateAfterReapply(response, true));
      }
      return response;
    });
  };
}

// response must be successful save
export function afterSaveDataset(response, nextAction) {
  invariant(!response.error, 'response can not be an error');
  return (dispatch) => {
    const nextDataset = apiUtils.getEntityFromResponse('datasetUI', response);
    const historyItems = response.payload.getIn(['entities', 'historyItem']).toList();
    dispatch(navigateToNextDataset(response, {replaceNav: true, isSaveAs: true}));

    // old versions need to be reloaded for occ version and possible change in display name
    dispatch(deleteOldDatasetVersions(nextDataset.get('datasetVersion'), historyItems));
    dispatch(performNextAction(nextDataset, nextAction));
    return response;
  };
}
export const DELETE_OLD_DATASET_VERSIONS = 'DELETE_OLD_DATASET_VERSIONS';

export function deleteOldDatasetVersions(currentVersion, historyItemsList) {
  const datasetVersions = historyItemsList
    .map((item) => item.get('datasetVersion'))
    .filter((datasetVersion) => datasetVersion !== currentVersion).toJS();

  return {
    type: DELETE_OLD_DATASET_VERSIONS,
    meta: {
      entityRemovePaths: flatten(datasetVersions.map((id) => [['datasetUI', id], ['tableData', id]]))
    }
  };
}
