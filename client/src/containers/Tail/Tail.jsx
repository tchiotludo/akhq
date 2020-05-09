import React, { Component } from 'react';

import Header from '../Header';

class Tail extends Component {
  render() {
    return (
      <div>
        <Header title="Live Tail" history={this.props.history} />
        Tail
      </div>
    );
  }
}

export default Tail;
