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
import { shallow } from 'enzyme';

import ModalForm from './ModalForm';

describe('ModalForm', () => {

  let commonProps;
  beforeEach(() => {
    commonProps = {
      confirmText: 'Save',
      onSubmit: sinon.spy(),
      onCancel: sinon.spy()
    };
  });

  it('should render form', () => {
    const wrapper = shallow(<ModalForm {...commonProps} />);
    expect(wrapper.type()).to.eql('form');
    expect(wrapper.prop('onSubmit')).to.equal(commonProps.onSubmit);
  });

  it('should pass props to ConfirmCancelFooter', () => {
    const wrapper = shallow(<ModalForm {...commonProps} />);
    const footerProps = wrapper.find('ConfirmCancelFooter').props();

    expect(footerProps.cancel).to.equal(commonProps.onCancel);
    expect(footerProps.confirmText).to.equal(commonProps.confirmText);
  });

  it('should render message when there is an error', () => {
    let wrapper = shallow(<ModalForm {...commonProps} />);
    expect(wrapper.find('Message')).to.have.length(0);

    wrapper = shallow(<ModalForm {...commonProps} error={{ message: 'foo error' }}/>);
    expect(wrapper.find('Message').first().prop('message')).to.eql('foo error');
  });

  it('should dismiss both message and dummy message', () => {
    const wrapper = shallow(<ModalForm {...commonProps} error={{ message: 'foo error' }}/>);
    expect(wrapper.find('Message').first().prop('dismissed')).to.be.false;

    wrapper.instance().handleDismissMessage();
    wrapper.update();
    expect(wrapper.find('Message').first().prop('dismissed')).to.be.true;
  });
});
