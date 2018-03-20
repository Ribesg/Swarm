import PropTypes from "prop-types";
import React     from "react";
import autoBind  from "react-autobind";
import Api       from "../Api";
import "./Login.sass";

class Login extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this.state = {
            loading: false,
            canSubmit: false,
        };
    }

    render() {
        const {loading, canSubmit} = this.state;
        return (
            <div id="login">
                <form className={loading ? "loading" : null} onSubmit={this._onFormSubmit}>
                    <label htmlFor="user">Login</label>
                    <input
                        id="user"
                        ref={input => this._userInput = input}
                        type="text"
                        autoComplete="username"
                        onChange={this._onAnyInputChange}
                    />
                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        ref={input => this._passwordInput = input}
                        type="password"
                        autoComplete="current-password"
                        onChange={this._onAnyInputChange}
                    />
                    <button type="submit" disabled={!canSubmit}>Login</button>
                </form>
            </div>
        );
    }

    _onAnyInputChange() {
        const user = this._userInput.value;
        const password = this._passwordInput.value;
        this.setState({canSubmit: user.length !== 0 && password.length !== 0});
    }

    _onFormSubmit(event) {
        event.preventDefault();
        this.setState({loading: true});
        if (this.request) {
            this.request.cancel();
        }
        const user = this._userInput.value;
        const password = this._passwordInput.value;
        this.request = Api.postLogin(user, password, this._onLoginResult);
    }

    _onLoginResult(data, error) {
        this.request = null;
        this.setState({loading: false});
        if (error) {
            // TODO
        } else {
            this.props.onLoggedIn();
        }
    }

}

Login.propTypes = {
    onLoggedIn: PropTypes.func.isRequired,
};

Login.defaultProps = {};

export default Login;
