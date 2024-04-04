import React from 'react';
import { useLocation, useNavigate, useNavigationType, useParams } from 'react-router-dom';

/* eslint-disable */
export function withRouter(Component) {
  return props => {
    const location = useLocation();
    const navigate = useNavigate();
    const params = useParams();
    const navigationType = useNavigationType();

    return (
      <Component
        {...props}
        location={{ ...location }}
        router={{ navigate, navigationType }}
        params={{ ...params }}
      />
    );
  };
}
