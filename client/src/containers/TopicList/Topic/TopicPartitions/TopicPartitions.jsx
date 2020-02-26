import React, { Component } from 'react';
import { uriTopicsPartitions } from '../../../../utils/endpoints';
import Table from '../../../../components/Table';
import { get } from '../../../../utils/api';

class TopicPartitions extends Component {
  state = {
    data: [],
    selectedCluster: this.props.clusterId,
    selectedTopic: this.props.topic
  };

  componentDidMount() {
    this.getTopicsPartitions();
    console.log('props: ', this.props);
  }

  async getTopicsPartitions() {
    let partitions = [];
    const { selectedCluster, selectedTopic } = this.state;
    console.log('cluster', selectedCluster);
    console.log('topic', selectedTopic);
    try {
      partitions = await get(uriTopicsPartitions(selectedCluster, selectedTopic));
      this.handleData(partitions.data);
    } catch (err) {
      console.error('Error:', err);
    }
  }

  handleData(partitions) {
    console.log('partitions', partitions);

    if (!partitions) {
      console.log('Not getting anything from backend');
    }
    let tablePartitions = partitions.map(partition => {
      return {
        id: partition.id,
        leader: partition.leader,
        replicas: partition.replicas,
        offsets: partition.offsets,
        size: partition.size
      };
    });
    this.setState({ data: tablePartitions });
  }
  /*
  handleLeader(leader){
    return 
  }

  handleReplicas(replicas){
    replicas.map(replica=>{
      switch(replica)
    })
  }

  handleOffsets(offsets){
    return (
      $(offsets.firstOffset)
      ---->
      $(offsets.lastOffset)
    )

  }

  handleSize(size){

  }
*/

  render() {
    const { data } = this.state;
    return (
      <div>
        <Table
          colNames={['id', 'Leader', 'Replicas', 'Offsets', 'Size']}
          toPresent={['id', 'leader', 'replicas', 'offsets', 'size']}
          data={data}
        />
      </div>
    );
  }
}

export default TopicPartitions;
