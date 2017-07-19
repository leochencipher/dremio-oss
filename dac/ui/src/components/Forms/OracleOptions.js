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
import { Component, PropTypes } from 'react';

import { formRow } from 'uiTheme/radium/forms';

import FieldWithError from 'components/Fields/FieldWithError';
import TextField from 'components/Fields/TextField';

export default class General extends Component {
  static getFields() {
    return ['instance'];
  }

  static propTypes = {
    instance: PropTypes.object
  };

  static validate(values) {
    const errors = {};
    if (!values.name) {
      errors.name = 'Name is required';
    }
    return errors;
  }

  constructor(props) {
    super(props);
  }

  render() {
    const {instance} = this.props;

    return (
      <div style={formRow}>
        <FieldWithError label='Instance' {...instance}>
          <TextField {...instance} />
        </FieldWithError>
      </div>
    );
  }
}
