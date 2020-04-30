import React from 'react';
import { Link, withRouter } from 'react-router-dom';
import { responsiveFontSizes } from '@material-ui/core';

function Header({ title, children }) {
  let login = localStorage.getItem('login');
  return (
    <React.Fragment>
      <div
        className="title"
        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
      >
        {' '}
        <h1>{title}</h1>{' '}
        {login === 'true' && (
          <a
            style={{ float: 'right' }}
            onClick={() => {
              localStorage.setItem('login', 'false');
              this.forceUpdate();
            }}
            data-turbolinks="false"
          >
            <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
            Logout
          </a>
        )}
        {(login === 'false' || !login) && (
          <Link to="/login">
            <button data-turbolinks="false" className="btn btn-primary" style={{ float: 'right' }}>
              {' '}
              <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
              Login
            </button>
          </Link>
        )}
        {children}
      </div>
    </React.Fragment>
  );
}

export default withRouter(Header);
