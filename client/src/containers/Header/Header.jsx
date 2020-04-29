import React from 'react';

import { Link, withRouter } from 'react-router-dom';
import { responsiveFontSizes } from '@material-ui/core';
function Header({ title, children }) {
  let login = localStorage.getItem('login');
  return (
    <React.Fragment>
      <p style={{marginTop:'2.8%'}}/>
        {login === 'true' && (
          <a
            style={{ cursor: 'pointer',paddingLeft: '94%',responsiveFontSizes:'115%'}}
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
          <Link to="/login" data-turbolinks="false" style={{ paddingLeft: '94%',fontSize:'115%'}}>
            <i className="fa fa-fw fa-sign-in" aria-hidden="true" />
            Login
          </Link>
        )}
      <div className="title">
        <h1>{title}</h1>
        {children}
      </div>
    </React.Fragment>
  );
}

export default Header;
