import React    from "react";
import autoBind from "react-autobind";
import "react-select/dist/react-select.css";
import Api      from "./Api";
import "./App.sass";
import Content  from "./Content/Content";
import TopForm  from "./TopForm/TopForm";

class App extends React.PureComponent {

    constructor() {
        super();
        autoBind(this);
        this.request = null;
        this.state = {
            hosts: null,
            selectedHost: this._getUrlParameter("host", null),
            selectedMode: this._getUrlParameter("mode", "LIVE"),
        };
        this.timer = null;
    }

    componentDidMount() {
        this._fetchHostsNowAndResetTimer();
    }

    componentWillUnmount() {
        this._clearTimer();
        this._cancelRequest();
    }

    render() {
        const {hosts, selectedHost, selectedMode} = this.state;
        this._setUrl(this.state.selectedHost, this.state.selectedMode);
        return (
            <div id="app">
                <TopForm
                    hosts={hosts}
                    modes={["LIVE", "HOUR", "DAY", "WEEK"]}
                    onHostSelected={this._onHostSelected}
                    onModeSelected={this._onModeSelected}
                    selectedHost={selectedHost}
                    selectedMode={selectedMode}
                />
                <Content
                    selectedHost={selectedHost}
                    selectedMode={selectedMode}
                />
            </div>
        );
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

    _clearTimer() {
        if (this.timer != null) {
            clearInterval(this.timer);
            this.timer = null;
        }
    }

    _cancelRequest() {
        if (this.request != null) {
            this.request.abort();
            this.request = null;
        }
    }

    _fetchHostsNowAndResetTimer() {
        this._clearTimer();
        this._fetchHosts();
        this.timer = setInterval(this._fetchHosts, 5000);
    }

    _fetchHosts() {
        this.request = Api.getHosts(this._onHostsFetched);
    }

    _onHostsFetched(data, error) {
        if (!error) {
            this.setState({hosts: data["hosts"]});
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

}

export default App;
