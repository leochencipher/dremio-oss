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
import { connect } from 'react-redux';
import Immutable from 'immutable';
import Radium from 'radium';

import { formLabel } from 'uiTheme/radium/typography';
import { WHITE, CELL_EXPANSION_HEADER } from 'uiTheme/radium/colors';
import AccelerationAggregation from './AccelerationAggregation';
import AccelerationRaw from './AccelerationRaw';

@Radium
export class AccelerationAdvanced extends Component {
  static propTypes = {
    acceleration: PropTypes.instanceOf(Immutable.Map).isRequired,
    fields: PropTypes.object,
    location: PropTypes.object.isRequired
  };

  static getFields() {
    return [
      ...AccelerationAggregation.getFields(),
      ...AccelerationRaw.getFields()
    ];
  }

  static validate(values) {
    return {
      ...AccelerationAggregation.validate(values),
      ...AccelerationRaw.validate(values)
    };
  }

  state = {
    activeTab: null
  }

  getActiveTab() {
    if (this.state.activeTab) return this.state.activeTab;

    const {layoutId} = (this.props.location.state || {});
    if (!layoutId) return 'RAW';

    const found = this.props.acceleration.getIn([
      'aggregationLayouts', 'layoutList'
    ]).some(layout => layout.getIn(['id', 'id']) === layoutId);

    return found ? 'AGGREGATION' : 'RAW';
  }

  renderTableQueries() {
    const { fields, acceleration } = this.props;
    return this.getActiveTab() === 'AGGREGATION'
      ? <AccelerationAggregation acceleration={acceleration} fields={fields}/>
      : <AccelerationRaw acceleration={acceleration} fields={fields}/>;
  }

  render() {
    const activeTab = this.getActiveTab();
    return (
      <div data-qa='acceleration-advanced'>
        <div style={styles.tabs}>
          <div
            data-qa='raw-queries-tab'
            style={[styles.tab, {backgroundColor: activeTab === 'RAW' ? CELL_EXPANSION_HEADER : WHITE }]}
            key='raw'
            onClick={() => this.setState({ activeTab: 'RAW' })}
          >
            {la('Raw Reflections')}
          </div>
          <div
            data-qa='aggregation-queries-tab'
            style={[styles.tab, {backgroundColor: activeTab === 'AGGREGATION' ? CELL_EXPANSION_HEADER : WHITE }]}
            key='aggregation'
            onClick={() => this.setState({ activeTab: 'AGGREGATION' })}
          >
            {la('Aggregation Reflections')}
          </div>
        </div>
        {this.renderTableQueries()}
      </div>
    );
  }
}

const mapStateToProps = state => {
  const location = state.routing.locationBeforeTransitions;
  return {
    location
  };
};

export default connect(mapStateToProps)(AccelerationAdvanced);

const styles = {
  base: {
    padding: '0 3px',
    width: '100%',
    overflow: 'hidden'
  },
  tabs: {
    display: 'flex',
    alignItems: 'center',
    height: 40,
    marginBottom: 5,
    width: '100%'
  },
  tab: {
    ...formLabel,
    height: 25,
    padding: 5,
    marginRight: 10,
    alignContent: 'center',
    ':hover': {
      backgroundColor: CELL_EXPANSION_HEADER,
      cursor: 'pointer'
    }
  }
};
