import React, {Component} from 'react';
import logo from '../images/logo.svg'

// Adaptation of login.ftl

class Login extends Component {
    render() {
        return (
            <div className="wrapper">
                <div id="content" className="no-side-bar">
                    <main>
                        <form className="khq-login" method="POST" action="/login">
                            <div>
                                <h3 className="logo"><img src={logo}
                                                          alt=""/><sup><strong>HQ</strong></sup></h3>
                            </div>

                            <div className="input-group mb-3">
                                <div className="input-group-prepend">
                                    <span className="input-group-text"><i className="fa fa-user"/></span>
                                </div>
                                <input type="text" name="username" className="form-control" placeholder="Username"
                                       aria-label="Username" required="" autoFocus=""/>
                            </div>

                            <div className="input-group mb-3">
                                <div className="input-group-prepend">
                                    <span className="input-group-text"><i className="fa fa-lock"/></span>
                                </div>
                                <input type="password" name="password" className="form-control" placeholder="Password"
                                       aria-label="Password" required=""/>
                            </div>

                            <div className="form-group text-right">
                                <input type="submit" value="Login" className="btn btn-primary btn-lg"/>
                            </div>
                        </form>

                    </main>
                </div>
            </div>
    );
    }
    }

    export default Login;
