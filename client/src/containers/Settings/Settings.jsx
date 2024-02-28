import React from 'react';
import Joi from 'joi-browser';
import Form from '../../components/Form/Form';
import Header from '../Header';
import { SETTINGS_VALUES } from '../../utils/constants';
import { setUIOptions } from '../../utils/localstorage';
import './styles.scss';
import { toast } from 'react-toastify';
import { getClusterUIOptions } from '../../utils/functions';
import { withRouter } from '../../utils/withRouter';

class Settings extends Form {
  state = {
    clusterId: '',
    formData: {
      topicDefaultView: '',
      topicDataSort: '',
      topicDataDateTimeFormat: '',
      skipConsumerGroups: false,
      skipLastRecord: false,
      showAllConsumerGroups: true
    },
    errors: {}
  };

  topicDefaultView = Object.entries(SETTINGS_VALUES.TOPIC.TOPIC_DEFAULT_VIEW).map(([value]) => ({
    _id: value,
    name: value
  }));
  topicDataSort = Object.entries(SETTINGS_VALUES.TOPIC_DATA.SORT).map(([value]) => ({
    _id: value,
    name: value
  }));
  topicDataDateTimeFormat = Object.entries(SETTINGS_VALUES.TOPIC_DATA.DATE_TIME_FORMAT).map(
    ([value]) => ({ _id: value, name: value })
  );

  schema = {
    topicDefaultView: Joi.string().optional(),
    topicDataSort: Joi.string().optional(),
    topicDataDateTimeFormat: Joi.string().required(),
    skipConsumerGroups: Joi.boolean().optional(),
    skipLastRecord: Joi.boolean().optional(),
    showAllConsumerGroups: Joi.boolean().optional()
  };

  componentDidMount() {
    const { clusterId } = this.props.params;
    this.setState({ clusterId }, () => {
      this._initializeVars(() => {
        this.setState({
          formData: {
            topicDefaultView:
              this.state.uiOptions && this.state.uiOptions.topic
                ? this.state.uiOptions.topic.defaultView
                : '',
            topicDataSort:
              this.state.uiOptions && this.state.uiOptions.topicData
                ? this.state.uiOptions.topicData.sort
                : '',
            topicDataDateTimeFormat:
              this.state.uiOptions && this.state.uiOptions.topicData
                ? this.state.uiOptions.topicData.dateTimeFormat
                : '',
            skipConsumerGroups:
              this.state.uiOptions && this.state.uiOptions.topic
                ? this.state.uiOptions.topic.skipConsumerGroups
                : false,
            skipLastRecord:
              this.state.uiOptions && this.state.uiOptions.topic
                ? this.state.uiOptions.topic.skipLastRecord
                : false,
            showAllConsumerGroups:
              this.state.uiOptions && this.state.uiOptions.topic
                ? this.state.uiOptions.topic.showAllConsumerGroups
                : false
          }
        });
      });
    });
  }

  checkedSkipConsumerGroups = event => {
    const { formData } = this.state;
    formData.skipConsumerGroups = event.target.checked;
    this.setState({ formData });
  };

  async _initializeVars(callBackFunction) {
    const uiOptions = await getClusterUIOptions(this.state.clusterId);
    this.setState(
      {
        uiOptions: uiOptions ?? {}
      },
      callBackFunction
    );
  }

  doSubmit() {
    const { clusterId, formData } = this.state;
    setUIOptions(clusterId, {
      topic: {
        defaultView: formData.topicDefaultView,
        skipConsumerGroups: formData.skipConsumerGroups,
        skipLastRecord: formData.skipLastRecord,
        showAllConsumerGroups: formData.showAllConsumerGroups
      },
      topicData: {
        sort: formData.topicDataSort,
        dateTimeFormat: formData.topicDataDateTimeFormat
      }
    });
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
                this.setState({ formData });
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
                />
              </span>
            </div>
            <div className="select-wrapper settings-wrapper row">
              <span className="col-sm-2 col-form-label">Skip Last Record Date</span>
              <span className="col-sm-10 row-checkbox-settings">
                <input
                  type="checkbox"
                  value="skipLastRecord"
                  checked={this.state.formData.skipLastRecord || false}
                  onChange={event => {
                    const { formData } = this.state;
                    formData.skipLastRecord = event.target.checked;
                    this.setState({ formData });
                  }}
                />
              </span>
            </div>
            <div className="select-wrapper settings-wrapper row">
              <span className="col-sm-2 col-form-label">Show All Consumer Groups</span>
              <span className="col-sm-10 row-checkbox-settings">
                <input
                  type="checkbox"
                  value="showAllConsumerGroups"
                  checked={this.state.formData.showAllConsumerGroups || false}
                  onChange={event => {
                    const { formData } = this.state;
                    formData.showAllConsumerGroups = event.target.checked;
                    this.setState({ formData });
                  }}
                />
              </span>
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
                this.setState({ formData });
              },
              'col-sm-10',
              'select-wrapper settings-wrapper',
              true,
              { className: 'form-control' }
            )}
            {this.renderSelect(
              'topicDataDateTimeFormat',
              'Time Format',
              this.topicDataDateTimeFormat,
              ({ currentTarget: input }) => {
                const { formData } = this.state;
                formData.topicDataDateTimeFormat = input.value;
                this.setState({ formData });
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

export default withRouter(Settings);
