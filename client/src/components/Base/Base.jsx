import React, { Component } from 'react';
import Sidebar from '../../containers/SideBar';

class Base extends Component {
  render() {
    console.log('Base', this.props);
    return (
      <div>
        {/* <Sidebar location={this.props.location} /> */}
        {this.props.children}
      </div>
    );
  }
}

export default Base;
