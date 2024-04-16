import React from 'react';
import Header from '../../../Header/Header';
import { uriConsumerGroup, uriDeleteGroupOffsets } from '../../../../utils/endpoints';
import 'react-toastify/dist/ReactToastify.css';
import Root from '../../../../components/Root';
import Table from '../../../../components/Table';
import constants from '../../../../utils/constants';
import ConfirmModal from '../../../../components/Modal/ConfirmModal';
import { toast } from 'react-toastify';
import { withRouter } from '../../../../utils/withRouter';

class ConsumerGroupOffsetDelete extends Root {
  state = {
    clusterId: '',
    consumerGroupId: '',
    topicIds: [],
    deleteAllOffsetsForTopic: '',
    showDeleteModal: false,
    deleteMessage: ''
  };

  componentDidMount() {
    const { clusterId, consumerGroupId } = this.props.params;

    this.setState({ clusterId, consumerGroupId }, () => {
      this.getTopics();
    });
  }

  async getTopics() {
    const { clusterId, consumerGroupId } = this.state;

    let data;
    data = await this.getApi(uriConsumerGroup(clusterId, consumerGroupId));
    data = data.data;

    if (data && data.topics) {
      this.setState({ topicIds: data.topics.map(topic => ({ topic })) });
    } else {
      this.setState({ topicIds: [] });
    }
  }

  showDeleteModal = deleteMessage => {
    this.setState({ showDeleteModal: true, deleteMessage });
  };

  closeDeleteModal = () => {
    this.setState({ showDeleteModal: false, deleteMessage: '', deleteAllOffsetsForTopic: '' });
  };

  deleteOffsets = () => {
    const { clusterId, consumerGroupId, deleteAllOffsetsForTopic } = this.state;
    this.removeApi(uriDeleteGroupOffsets(clusterId, consumerGroupId, deleteAllOffsetsForTopic))
      .then(() => {
        toast.success(
          `Offsets for topic '${deleteAllOffsetsForTopic}' and consumer group '${consumerGroupId}' are deleted`
        );
        this.setState(
          { showDeleteModal: false, deleteMessage: '', deleteAllOffsetsForTopic: '' },
          () => {
            this.getTopics();
          }
        );
      })
      .catch(() => {
        this.setState({ showDeleteModal: false, deleteMessage: '', deleteAllOffsetsForTopic: '' });
      });
  };

  handleOnDelete(topicId) {
    this.setState({ deleteAllOffsetsForTopic: topicId }, () => {
      this.showDeleteModal(
        <React.Fragment>
          Do you want to delete all offsets of topic: {<code>{topicId}</code>} ?
        </React.Fragment>
      );
    });
  }

  render() {
    const { consumerGroupId } = this.state;

    return (
      <div>
        <div>
          <Header title={`Delete offsets: ${consumerGroupId}`} />
        </div>
        <div>
          <Table
            columns={[
              {
                id: 'topic',
                accessor: 'topic',
                colName: 'Topic',
                type: 'text',
                sortable: true
              }
            ]}
            data={this.state.topicIds}
            noContent={
              <tr>
                <td colSpan={3}>
                  <div className="alert alert-warning mb-0" role="alert">
                    No offsets found.
                  </div>
                </td>
              </tr>
            }
            onDelete={row => {
              this.handleOnDelete(row.topic);
            }}
            actions={[constants.TABLE_DELETE]}
          />
        </div>
        <ConfirmModal
          show={this.state.showDeleteModal}
          handleCancel={this.closeDeleteModal}
          handleConfirm={this.deleteOffsets}
          message={this.state.deleteMessage}
        />
      </div>
    );
  }
}

export default withRouter(ConsumerGroupOffsetDelete);
