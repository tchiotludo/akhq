import React from 'react';
import Joi from 'joi-browser';
import Form from '../../components/Form/Form';
import Header from '../Header';
import {getUIOptions, setUIOptions} from "../../utils/localstorage";
import './styles.scss';
import {toast} from "react-toastify";

class Setttings extends Form {
  state = {
    clusterId: '',
    formData: {
      topicDefaultView: '',
      topicDataSort: '',
      skipConsumerGroups: false,
      skipLastRecord: false
    },
    errors: {}
  };

  topicDefaultView = [ { _id: 'ALL', name: 'ALL' }, { _id: 'HIDE_INTERNAL', name: 'HIDE_INTERNAL' },
    { _id: 'HIDE_INTERNAL_STREAM', name: 'HIDE_INTERNAL_STREAM' }, { _id: 'HIDE_STREAM', name: 'HIDE_STREAM' } ];
  topicDataSort = [ { _id: 'OLDEST', name: 'OLDEST' }, { _id: 'NEWEST', name: 'NEWEST' } ];

  schema = {
    topicDefaultView: Joi.string().required(),
    topicDataSort: Joi.string().required(),
    skipConsumerGroups: Joi.boolean().optional(),
    skipLastRecord: Joi.boolean().optional()
  };

  componentDidMount() {
    const { clusterId } = this.props.match.params;

    const uiOptions = getUIOptions(clusterId);

    this.setState({ clusterId, formData: {
        topicDefaultView: (uiOptions && uiOptions.topic)? uiOptions.topic.defaultView : '',
        topicDataSort: (uiOptions && uiOptions.topicData)? uiOptions.topicData.sort : '',
        skipConsumerGroups: (uiOptions && uiOptions.topic)? uiOptions.topic.skipConsumerGroups : false,
        skipLastRecord: (uiOptions && uiOptions.topic)? uiOptions.topic.skipLastRecord : false
      }})
  }

  checkedSkipConsumerGroups = (event) => {
    const { formData } = this.state;
    formData.skipConsumerGroups = event.target.checked;
    this.setState({formData});
  }



  doSubmit() {
    const { clusterId, formData } = this.state;
    setUIOptions(clusterId,
        { topic: { defaultView: formData.topicDefaultView,
                               skipConsumerGroups: formData.skipConsumerGroups,
                               skipLastRecord: formData.skipLastRecord },
                      topicData: {sort: formData.topicDataSort} });
    toast.success(`Settings for cluster '${clusterId}' updated successfully.`);
  }

  render() {
    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title="Settings" history={this.props.history} />
          <fieldset id="topic" key="topic">
            <legend id="topic">Topic</legend>
          {this.renderSelect(
              'topicDefaultView',
              'Default View',
               this.topicDefaultView,
              ({ currentTarget: input }) => {
                const { formData } = this.state;
                formData.topicDefaultView = input.value;
                this.setState({formData});
              },
              'col-sm-10',
              'select-wrapper settings-wrapper',
              true,
              { className: 'form-control' }
          )}
            <div className="select-wrapper settings-wrapper row">
              <span className="col-sm-2 col-form-label">Skip Consumer Groups</span>
              <span className="col-sm-10 row-checkbox-settings">
                <input
                  type="checkbox"
                  value="skipConsumerGroups"
                  checked={this.state.formData.skipConsumerGroups || false}
                  onChange={this.checkedSkipConsumerGroups}
              /></span>
            </div>
            <div className="select-wrapper settings-wrapper row">
              <span className="col-sm-2 col-form-label">Skip Last Record Date</span>
              <span className="col-sm-10 row-checkbox-settings">
              <input
                  type="checkbox"
                  value="skipLastRecord"
                  checked={this.state.formData.skipLastRecord || false}
                  onChange={ event => {
                    const { formData } = this.state;
                    formData.skipLastRecord = event.target.checked;
                    this.setState({formData});
                  }}
              /></span>
            </div>
          </fieldset>

          <fieldset id="topicData" key="topicData">
            <legend id="topicData">Topic Data</legend>
            {this.renderSelect(
                'topicDataSort',
                'Sort',
                this.topicDataSort,
                ({ currentTarget: input }) => {
                  const { formData } = this.state;
                  formData.topicDataSort = input.value;
                  this.setState({formData});
                },
                'col-sm-10',
                'select-wrapper settings-wrapper',
                true,
                { className: 'form-control' }
            )}
          </fieldset>

          {this.renderButton(
            'Update Settings',
            () => {
              this.doSubmit();
            },
            undefined,
            'button'
          )}
        </form>
      </div>
    );
  }
}

export default Setttings;
