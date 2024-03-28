import React from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';

/* eslint-disable */
export function withRouter(Component) {
  return props => {
    const location = useLocation();
    const navigate = useNavigate();
    const params = useParams();

    return (
      <Component
        {...props}
        location={{ ...location }}
        router={{ navigate }}
        params={{ ...params }}
      />
    );
  };
}
