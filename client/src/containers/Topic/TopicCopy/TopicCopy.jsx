import React from 'react';
import Header from '../../Header/Header';
import Form from '../../../components/Form/Form';
import {
    formatDateTime,
    transformListObjsToViewOptions,
    transformStringArrayToViewOptions
} from '../../../utils/converters';
import Joi from 'joi-browser';
import {
    uriClusters,
    uriTopicsCopy,
    uriTopicsInfo,
    uriTopicsName, uriTopicsOffsetsByTimestamp
} from '../../../utils/endpoints';
import './styles.scss';
import {toast} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Dropdown from "react-bootstrap/Dropdown";
import DatePicker from "../../../components/DatePicker";
import moment from "moment";
import Input from "../../../components/Form/Input";

class TopicCopy extends Form {
  state = {
    clusterId: '',
    topicId: '',
    clusters: [],
    topics: [],
    selectedTopic: undefined,
    checked: {},
    formData: {},
    errors: {},
    loading: true
  };

  schema = {};

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;

    this.schema['clusterListView'] = Joi.string().required();
    this.schema['topicListView'] = Joi.string().required();
    this.schema['lastMessagesNr'] = Joi.string().allow('');

    this.setState({ clusterId, topicId, formData: { clusterListView: clusterId, lastMessagesNr: ''} }, () => {
      this.setupInitialData(clusterId, topicId);
    });
  }


  setupInitialData = (clusterId, topicId) => {
    const { formData, checked } = this.state;

    const requests = [
        this.getApi(uriClusters()),
        this.getApi(uriTopicsName(clusterId)),
        this.getApi(uriTopicsInfo(clusterId, topicId))
        ];

    Promise.all(requests)
        .then(data => {
          data[2].data.partitions.forEach(partition => {
                const name = `partition-${partition.id}`;
                const checkName = `check-${name}`;

                this.schema[name] = Joi.number()
                    .min(partition.firstOffset || 0)
                    .max(partition.lastOffset || 0)
                    .required()
                    .label(`Partition ${partition.id} offset`);

                formData[name] = partition.firstOffset || '0';
                checked[checkName] = true;
          });

          this.setState({
            formData,
            checked,
            selectedTopic: data[2].data,
            clusters: transformListObjsToViewOptions(data[0].data, 'id', 'id'),
            topics: transformStringArrayToViewOptions(data[1].data),
            loading: false
          });
        })
        .catch(err => {
          console.error('Error:', err);
        });
  }

  getTopics = (clusterId) => {
    this
        .getApi(uriTopicsName(clusterId))
        .then(res => {
          this.setState(
              { topics: transformStringArrayToViewOptions(res.data), loading: false }
          );
        })
        .catch(err => {
          console.error('Error:', err);
        });
  };

  handleOnChangeTopic = () => {
    const { formData } = this.state;

    this
        .getApi(uriTopicsInfo(formData.clusterListView, formData.topicListView))
        .then(res => {
          this.setState(
              { selectedTopic: res.data, loading: false }
          );
        })
        .catch(err => {
          console.error('Error:', err);
        });
  };

  createSubmitBody = (formData, checked) => {
    let body = [];
    let splitName = [];
    let partition = '';
    const checkedPartition= [];

    Object.keys(checked).forEach(checkedName => {
        splitName = checkedName.split('-');
        partition = splitName.pop();
        checkedPartition[partition] = checked[checkedName];
    });

    Object.keys(formData).filter(value => value.startsWith('partition')).forEach(name => {
      splitName = name.split('-');
      partition = splitName.pop();

      if(checkedPartition[partition] === true) {
          body.push({
              partition,
              offset: formData[name]
          });
      }
    });
    return body;
  };

  async doSubmit() {
    const { clusterId, topicId, formData, checked } = this.state;
    const result = await this.postApi(
        uriTopicsCopy(clusterId, topicId, formData.clusterListView, formData.topicListView),
        this.createSubmitBody(formData, checked)
    );

    toast.success(`Copied ${result.data.records} records to topic '${formData.topicListView}' successfully.`);
  }

  checkedTopicOffset = (event) => {
    const { checked } = this.state;
    checked[event.target.value] = event.target.checked;

    this.setState({ checked: checked });
  }

  renderTopicPartition = () => {
    const { selectedTopic } = this.state;
    const renderedItems = [];

    if(selectedTopic) {

      renderedItems.push(
          <fieldset id={`fieldset-${selectedTopic.name}`} key={selectedTopic.name}>
            <legend id={`legend-${selectedTopic.name}`}>Partitions</legend>
            {this.renderPartitionInputs(selectedTopic.partitions)}
          </fieldset>
      );
    }
    return renderedItems;
  };

  renderPartitionInputs = (partitions) => {
    const { checked } = this.state;
    const renderedInputs = [];

    partitions.forEach(partition => {
      const name = `partition-${partition.id}`;
      const checkName = `check-${name}`;

      renderedInputs.push(
        <div className="form-group row row-checkbox" key={name}>
            { <input
                type="checkbox"
                value={checkName}
                checked={checked[checkName] || false}
                onChange={this.checkedTopicOffset}/>

            }
          <div className="col-sm-10 partition-input">
            <span id={`partition-${partition.id}-input`}>

              {this.renderInput(
                name,
                `Partition: ${partition.id}`,
                'Offset',
                'number',
                undefined,
                true,
                'partition-input-div',
                `partition-input ${name}-input`
              )}
            </span>
          </div>
        </div>
      );
    });

    return renderedInputs;
  };

  unCheckAll = (value)  => {
    const {checked} = this.state;

    Object.keys(checked).forEach(name => {
        checked[name] = value;
    });

    this.setState({ checked});
  }

  resetToFirstOffsets = () => {
    const { selectedTopic, formData } = this.state;

    selectedTopic.partitions.forEach(partition => {
      const name = `partition-${partition.id}`;
      formData[name] = partition.firstOffset || '0';
    });

    this.setState({ formData });
  };

  resetToLastOffsets = () => {
    const { selectedTopic, formData } = this.state;

    selectedTopic.partitions.forEach(partition => {
        const name = `partition-${partition.id}`;
        formData[name] = partition.lastOffset || '0';
    });

    this.setState({ formData });
  };

  resetToCalculatedOffsets = ({ currentTarget: input }) => {
    const { selectedTopic, formData } = this.state;

    selectedTopic.partitions.forEach(partition => {
        const name = `partition-${partition.id}`;
        const calculatedOffset = (partition.lastOffset || 0) - input.value;
        formData[name] = (!calculatedOffset || calculatedOffset < 0 )? '0' : calculatedOffset;
    });

    formData['lastMessagesNr'] = input.value;
    this.setState({ formData });
  };

  async getTopicOffset() {
    const { clusterId, topicId, timestamp} = this.state;
    const momentValue = moment(timestamp);

    const date =
        timestamp.toString().length > 0
            ? formatDateTime(
            {
                year: momentValue.year(),
                monthValue: momentValue.month(),
                dayOfMonth: momentValue.date(),
                hour: momentValue.hour(),
                minute: momentValue.minute(),
                second: momentValue.second(),
                milli: momentValue.millisecond()
            },
            'YYYY-MM-DDTHH:mm:ss.SSS'
        ) + 'Z'
            : '';

    let data = {};
    if (date !== '') {
        data = await this.getApi(uriTopicsOffsetsByTimestamp(clusterId, topicId, date));
        data = data.data;
        this.handleOffsetsByTimestamp(data);
    }
  }

  handleOffsetsByTimestamp = partitions => {
    const { formData } = this.state;
    partitions.forEach(partition => {
        const name = `partition-${partition.partition}`;
        formData[name] = partition.offset || '0';
    });
    this.setState({ formData });
  };

  renderResetButton = () => {
    const { timestamp, formData} = this.state;
    const { loading } = this.props.history.location;

    return (
        <span>

    <div
        className="btn btn-secondary"
        type="button"
        style={{ marginRight: '0.5rem' }}
        onClick={() => this.unCheckAll(true)}
    >
      Check all
    </div>
    <div
        className="btn btn-secondary"
        type="button"
        style={{ marginRight: '0.5rem' }}
        onClick={() => this.unCheckAll(false)}
    >
      Uncheck all
    </div>
    <div
        className="btn btn-secondary"
        type="button"
        style={{ marginRight: '0.5rem' }}
        onClick={() => this.resetToFirstOffsets()}
    >
      Reset to first offsets
    </div>
    <div
        className="btn btn-secondary"
        type="button"
        style={{ marginRight: '0.5rem' }}
        onClick={() => this.resetToLastOffsets()}
    >
      Reset to last offsets
    </div>
    <div
        className="btn btn-secondary"
        type="button"
        style={{ marginRight: '0.5rem', padding: 0 }}
    >
      <Dropdown>
        <Dropdown.Toggle>Filter datetime</Dropdown.Toggle>
          {!loading && (
              <Dropdown.Menu>
                  <div>
                      <DatePicker
                          showTimeInput
                          showDateTimeInput
                          value={timestamp}
                          label={''}
                          onChange={value => {
                              this.setState({ timestamp: value }, () => this.getTopicOffset());
                          }}
                      />
                  </div>
              </Dropdown.Menu>
          )}
      </Dropdown>

    </div>

    <div
        className="btn btn-secondary"
        type="button"
        style={{ marginRight: '0.5rem', padding: 0 }}
    >
      <Dropdown>
        <Dropdown.Toggle>Last x messages per partition</Dropdown.Toggle>
          {!loading && (
              <Dropdown.Menu>
                  <div>
                      <Input
                          type='number'
                          name='lastMessagesNr'
                          id='lastMessagesNr'
                          value={formData['lastMessagesNr'] || ''}
                          label=''
                          placeholder='Last x messages'
                          onChange={this.resetToCalculatedOffsets}
                          noStyle=''
                          wrapperClass='input-nr-messages'
                          inputClass=''
                      />
                  </div>
              </Dropdown.Menu>
          )}
      </Dropdown>

    </div>
  </span>
    );
  };


  render() {
    const { clusterId, topicId, clusters, topics } = this.state;

    return (
      <div>
        <Header title={`Copy topic ${topicId} from cluster ${clusterId}`} history={this.props.history} />
        <form
          className="khq-form khq-copy-topic"
          onSubmit={() => this.handleSubmit()}
        >
          {this.renderTopicPartition()}
            <fieldset id="cluster" key="cluster">
                <legend id="cluster">Target Topic</legend>

          {this.renderSelect(
              'clusterListView',
              'Cluster',
              clusters,
              ({ currentTarget: input }) => {
                    const { formData } = this.state;
                    formData.clusterListView = input.value;
                    this.setState({formData});
                    this.getTopics(input.value);
                },
              'col-sm-10',
              'select-wrapper select-wrapper-copy',
              false,
              { className: 'form-control' }
          )}

          {this.renderSelect(
              'topicListView',
              'Topic',
              topics,
              ({ currentTarget: input }) => {
                    const { formData } = this.state;
                    formData.topicListView = input.value;
                    this.setState({formData});

              },
              'col-sm-10',
              'select-wrapper select-wrapper-copy',
              true,
              { className: 'form-control' }
          )}
            </fieldset>

            {this.renderButton(
                'Copy',
                this.handleSubmit,
                undefined,
                'submit',
                this.renderResetButton()
            )}
        </form>
      </div>
    );
  }
}

export default TopicCopy;
