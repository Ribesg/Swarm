import React          from "react";
import autoBind       from "react-autobind";
import "react-select/dist/react-select.css";
import Api            from "./Api";
import "./App.sass";
import Content        from "./Content/Content";
import LoadingSpinner from "./LoadingSpinner/LoadingSpinner";
import Login          from "./Login/Login";
import TopForm        from "./TopForm/TopForm";

class App extends React.PureComponent {

    constructor() {
        super();
        autoBind(this);
        this.request = null;
        this.state = {
            hosts: null,
            loading: true,
            loggedIn: false,
            selectedHost: this._getUrlParameter("host", null),
            selectedMode: this._getUrlParameter("mode", "LIVE"),
        };
        this.timer = null;
        Api.set401Callback(this._on401Error);
    }

    componentDidMount() {
        this._fetchHostsNowAndResetTimer();
    }

    componentWillUnmount() {
        this._clearTimer();
        this._cancelRequest();
    }

    render() {
        const {hosts, loading, loggedIn, selectedHost, selectedMode} = this.state;
        this._setUrl(this.state.selectedHost, this.state.selectedMode);
        let content;
        if (loading) {
            content = <LoadingSpinner size="big"/>;
        } else if (loggedIn) {
            content = [
                <TopForm
                    key="form"
                    hosts={hosts}
                    modes={["LIVE", "HOUR", "DAY", "WEEK"]}
                    onHostSelected={this._onHostSelected}
                    onModeSelected={this._onModeSelected}
                    onLogout={this._onLogout}
                    selectedHost={selectedHost}
                    selectedMode={selectedMode}
                />,
                <Content
                    key="content"
                    selectedHost={selectedHost}
                    selectedMode={selectedMode}
                />,
            ];
        } else {
            content = <Login onLoggedIn={this._onLoggedIn}/>;
        }
        return <div id="app">{content}</div>;
    }

    _onHostSelected(host) {
        if (this.state.selectedHost !== host) {
            this.setState({selectedHost: host});
        }
    }

    _onModeSelected(mode) {
        if (this.state.selectedMode !== mode) {
            this.setState({selectedMode: mode});
        }
    }

    _onLogout() {
        this._cancelRequest();
        this._clearTimer();
        Api.postLogout(this._onLoggedOut);
        this.setState({loading: true});
    }

    _onLoggedOut(data, error) {
        this.setState({loading: false});
        if (!error) {
            this.setState({loggedIn: false});
        }
    }

    _fetchHostsNowAndResetTimer() {
        this._clearTimer();
        this._fetchHosts();
        this.timer = setInterval(this._fetchHosts, 5000);
    }

    _clearTimer() {
        if (this.timer != null) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }

    _fetchHosts() {
        this.request = Api.getHosts(this._onHostsFetched);
    }

    _cancelRequest() {
        if (this.request != null) {
            this.request.cancel();
            this.request = null;
        }
    }

    _onHostsFetched(data, error) {
        if (!error) {
            this.setState({hosts: data["hosts"], loading: false, loggedIn: true});
        }
    }

    _getUrlParameter(name, orElse) {
        const url = window.location.href;
        const escapedName = name.replace(/[\[\]]/g, "\\$&");
        const regex = new RegExp("[?&]" + escapedName + "(=([^&#]*)|&|#|$)");
        const results = regex.exec(url);
        let result = false;
        if (!results) {
            result = null;
        } else if (!results[2]) {
            result = "";
        } else {
            result = decodeURIComponent(results[2].replace(/\+/g, " "));
        }
        if (result || orElse === undefined) {
            return result;
        } else {
            return orElse;
        }
    }

    _setUrl(host, mode) {
        if (host !== null) {
            const url = `/?host=${host}&mode=${mode}`;
            window.history.pushState(null, "", url);
        }
    }

    _on401Error() {
        this._cancelRequest();
        this._clearTimer();
        this.setState({loading: false, loggedIn: false});
    }

    _onLoggedIn() {
        this.setState({loading: false, loggedIn: true});
        this._fetchHostsNowAndResetTimer();
    }

}

export default App;
