import React from 'react';
import Header from '../../Header/Header';
import Form from '../../../components/Form/Form';
import {transformListObjsToViewOptions, transformStringArrayToViewOptions} from '../../../utils/converters';
import Joi from 'joi-browser';
import {
  uriClusters,
  uriConsumerGroupUpdate,
  uriTopicsCopy,
  uriTopicsInfo,
  uriTopicsName
} from '../../../utils/endpoints';
import './styles.scss';
import {toast} from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

class TopicCopy extends Form {
  state = {
    clusterId: '',
    topicId: '',
    clusters: [],
    topics: [],
    selectedTopic: undefined,
    formData: {},
    errors: {},
    loading: true
  };

  schema = {};

  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;

    this.schema['clusterListView'] = Joi.string().required();
    this.schema['topicListView'] = Joi.string().required();

    this.setState({ clusterId, topicId, formData: { clusterListView: clusterId} }, () => {
      this.setupInitialData(clusterId);
    });
  }


  setupInitialData = (clusterId) => {
    const requests = [
        this.getApi(uriClusters()),
        this.getApi(uriTopicsName(clusterId))];

    Promise.all(requests)
        .then(data => {
          console.log(data);
          this.setState({
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
            res.data.partitions.forEach(partition => {
            const name = `partition-${partition.id}`;
            this.schema[name] = Joi.number()
                .min(partition.firstOffset || 0)
                .max(partition.lastOffset || 0)
                .required()
                .label(`Partition ${partition.id} offset`);

            formData[name] = partition.firstOffset || '0';
          })

          this.setState(
              { formData, selectedTopic: res.data, loading: false }
          );
        })
        .catch(err => {
          console.error('Error:', err);
        });
  };

  createSubmitBody = formData => {
    let body = [];
    let splitName = [];
    let topic = '';
    let partition = '';

    Object.keys(formData).filter(value => value.startsWith('partition')).forEach(name => {
      splitName = name.split('-');
      partition = splitName.pop();

      body.push({
        partition,
        offset: formData[name]
      });
    });
    return body;
  };

  async doSubmit() {
    const { clusterId, topicId, formData } = this.state;

    console.log(formData);

    await this.postApi(
        uriTopicsCopy(clusterId, topicId, formData.clusterListView, formData.topicListView),
        this.createSubmitBody(formData)
    );

   // this.setState({ state: this.state });
    toast.success(`Copied topic '${topicId}' successfully.`);
  }

  renderTopicPartition = () => {
    const { selectedTopic } = this.state;
    const renderedItems = [];

    if(selectedTopic) {

      renderedItems.push(
          <fieldset id={`fieldset-${selectedTopic.name}`} key={selectedTopic.name}>
            <legend id={`legend-${selectedTopic.name}`}>{selectedTopic.name}</legend>
            {this.renderPartitionInputs(selectedTopic.partitions)}
          </fieldset>
      );
    }
    return renderedItems;
  };

  renderPartitionInputs = (partitions) => {
    const renderedInputs = [];

    partitions.forEach(partition => {
      const name = `partition-${partition.id}`;

      renderedInputs.push(
        <div className="form-group row" key={name}>
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


  render() {
    const { clusterId, topicId, clusters, topics } = this.state;

    return (
      <div>
        <Header title={`Copy topic ${topicId} from cluster ${clusterId}`} history={this.props.history} />
        <form
          className="khq-form khq-copy-topic"
          onSubmit={() => this.handleSubmit()}
        >
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
              '',
              'select-wrapper',
              { className: 'form-control' }
          )}
          {this.renderSelect(
              'topicListView',
              'Topic',
              topics,
              ({ currentTarget: input }) => {
                    const { formData } = this.state;
                    formData.topicListView = input.value;
                    this.setState({formData}, () => this.handleOnChangeTopic());

              },
              '',
              'select-wrapper',
              { className: 'form-control' }
          )}
          {this.renderTopicPartition()}
          {this.renderButton(
              'Copy',
              this.handleSubmit,
              undefined,
              'submit'
          )}
        </form>
      </div>
    );
  }
}

export default TopicCopy;
