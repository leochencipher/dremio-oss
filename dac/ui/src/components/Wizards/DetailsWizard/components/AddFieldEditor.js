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
import { connect }   from 'react-redux';
import pureRender from 'pure-render-decorator';
import Radium from 'radium';

import SqlAutoComplete from 'pages/ExplorePage/components/SqlEditor/SqlAutoComplete';
import FunctionsHelpPanel from 'pages/ExplorePage/components/SqlEditor/FunctionsHelpPanel';
import DragTarget from 'components/DragComponents/DragTarget';

import './AddFieldEditor.less';

// TODO: because api can't return info about one dataset, we load all for space, should be remove in future
// TODO: remove code duplication with sql part
@pureRender
@Radium
export class AddFieldEditor extends Component {
  static propTypes = {
    pageType: PropTypes.string,
    currentQuery: PropTypes.string,
    activeMode: PropTypes.bool,
    dragType: PropTypes.string,
    blockHeight: PropTypes.number,
    tooltip: PropTypes.string,
    initialValue: PropTypes.string,
    sqlHeight: PropTypes.number,
    name: PropTypes.string,
    functionPaneHeight: PropTypes.number,
    onFocus: PropTypes.func,
    onBlur: PropTypes.func,
    onChange: PropTypes.func,
    style: PropTypes.object
  };

  constructor(props) {
    super(props);
    this.showDropDown = this.showDropDown.bind(this);
    this.hideDropDown = this.hideDropDown.bind(this);
    this.addFuncToSqlEditor = this.addFuncToSqlEditor.bind(this);
    this.toggleFunctionsHelpPanel = this.toggleFunctionsHelpPanel.bind(this);
    this.onDropFunc = this.onDropFunc.bind(this);
    this.state = {
      sqlState: true,
      funcHelpPanel: true,
      dropFunc: '',
      dropDownVisible:false
    };
    this.preSqlSize = 0;
  }

  onDropFunc = (e) => {
    this.refs.editor.insertFunction(e.id, false, e.args);
  }

  getFunctionsPanel() {
    return (
      <div>
        <FunctionsHelpPanel
          height={this.props.functionPaneHeight}
          isVisible={this.state.funcHelpPanel}
          dragType={this.props.dragType}
          addFuncToSqlEditor={this.addFuncToSqlEditor}/>
      </div>
    );
  }

  getItem(item, separator, isLast) {
    return (
      <div className='item' key={item.name}>
        <span style={item.style}>{item.name}{separator}</span>
        <span className='values'>
          {item.values.map((value) => <span className='value' key={value}>{' ' + value}</span>)}
        </span>
        <span className='separate-line'>{isLast ? '' : '|'}</span>
      </div>
    );
  }

  addFuncToSqlEditor(sqlData) {
    this.setState({
      dropFunc: sqlData
    });
    this.refs.editor.insertFunction(sqlData);
  }

  hideDropDown = () => this.setState({ dropDownVisible: false })

  showDropDown = () => this.setState({ dropDownVisible: true })

  toggleFunctionsHelpPanel = () => this.setState({ funcHelpPanel: !this.state.funcHelpPanel })


  render() {
    const { name, onBlur, onFocus, onChange } = this.props;
    const inputProps = { name, onBlur, onFocus, onChange };
    const sqlBlock = this.state.sqlState
      ? <div>
        <SqlAutoComplete
          pageType={this.props.pageType}
          onChange={inputProps.onChange}
          tooltip={this.props.tooltip}
          defaultValue={this.props.initialValue}
          ref='editor'
          hideDropDown={this.hideDropDown}
          height={this.props.sqlHeight  || 150}
          disableAutocomplete
          sqlSize={this.props.sqlHeight  || 150}
          style={{marginLeft: 12}}/>
      </div>
      : <div/>;

    return (
      <div style={{width: '100%'}}>
        <div style={{height: this.props.blockHeight || 116, position: 'relative', marginTop: 10}}>
          <div className='sql-part add-field-editor'
            onClick={this.hideDropDown}
            style={[styles.base, this.props.style]}>
            {this.getFunctionsPanel()}
            <DragTarget dragType={this.props.dragType} onDrop={this.onDropFunc}>
              {sqlBlock}
            </DragTarget>
          </div>
        </div>
      </div>
    );
  }
}

function mapStateToProps(state) {
  return {
    height: state.explore.ui.get('sqlSize')
  };
}

export default connect(mapStateToProps)(AddFieldEditor);

const styles = {
  base: {
    width: '100%'
  }
};
