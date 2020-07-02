import React from 'react';
import {BrowserRouter as Router} from 'react-router-dom';
import {baseUrl, uriClusters} from './utils/endpoints';
import Routes from './utils/Routes';
import history from './utils/history';
import api, {handleCatch} from './utils/api';
import Loading from '../src/containers/Loading';
import {MuiPickersUtilsProvider} from '@material-ui/pickers';
import MomentUtils from '@date-io/moment';
import {ToastContainer} from 'react-toastify';

class App extends React.Component {
  state = {
    clusterId: ''
  };

  componentDidMount() {
    api
      .get(uriClusters())
      .then(res => {
        this.setState({ clusterId: res.data ? res.data[0].id : '' });
      })
      .catch(err => {
        handleCatch(err);
      })
      .then(() => {
        history.replace({
          loading: false
        });
      })
  }

  render() {
    const { clusterId } = this.state;
    if (clusterId) {
      return (
        <MuiPickersUtilsProvider utils={MomentUtils}>
          <Router>
            <Routes clusterId={clusterId} location={baseUrl} />
            <ToastContainer
                draggable={false}
                closeOnClick={false}
            />
          </Router>
        </MuiPickersUtilsProvider>

      );
    } else {
      return (<div>
        <Loading show="true" />
        <ToastContainer
            draggable={false}
        />
      </div>);
    }
  }
}

export default App;
