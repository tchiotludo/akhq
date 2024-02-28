import React from 'react';
import { useLocation, useMatch, useNavigate, useParams } from 'react-router-dom';

/* eslint-disable */
export function withRouter<ComponentProps>(Component: React.FunctionComponent<ComponentProps>) {
  function ComponentWithRouterProp(props: ComponentProps) {
    const location = useLocation();
    const navigate = useNavigate();
    const params = useParams();

    return <Component {...props} location={{...location}} router={{navigate}} params={{...params}} />;
  }

  return ComponentWithRouterProp;
}
