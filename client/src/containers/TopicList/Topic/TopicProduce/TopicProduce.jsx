import React, { Component } from 'react';
import Form from '../../../../components/Form/Form';
import Header from '../../../Header';
class TopicProduce extends Component {
  state = {
    clusterId: this.props.clusterId,
    topicId: this.props.topicId
  };
  componentDidMount() {
    const { clusterId, topicId } = this.props.match.params;

    this.setState({ clusterId, topicId });
  }

  render() {
    const { clusterId, topicId } = this.state;
    console.log('topicId' + topicId);
    return (
      <div id="content">
        <Header title={`Produce to ${topicId} `} />
      </div>
    );
  }
}

export default TopicProduce;

