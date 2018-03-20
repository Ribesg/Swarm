import Api                      from "App/Api";
import { roundWithMaxDecimals } from "charts/ChartUtils";
import PropTypes                from "prop-types";
import React                    from "react";
import autoBind                 from "react-autobind";
import LoadingSpinner           from "../../../LoadingSpinner/LoadingSpinner";
import Table                    from "./Table";

class DiskSpaceTable extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this.state = {
            selectedHost: props.selectedHost,
            columns: null,
            data: null,
            loading: true,
        };
        this.timer = null;
        this.request = null;
    }

    componentDidMount() {
        this._fetchDataNowAndResetTimer();
    }

    componentWillReceiveProps(newProps) {
        if (newProps.selectedHost !== this.state.selectedHost) {
            this.setState({
                selectedHost: newProps.selectedHost,
                columns: null,
                data: null,
                loading: true,
            }, this._fetchDataNowAndResetTimer);
        }
    }

    componentWillUnmount() {
        clearInterval(this.timer);
        this.timer = null;
        if (this.request) {
            this.request.cancel();
            this.request = null;
        }
    }

    render() {
        const {columns, data, loading} = this.state;
        if (loading) {
            return (
                <div id="disk-space-table" className="empty">
                    <LoadingSpinner/>
                </div>
            );
        } else if (data === null) {
            return (
                <div id="disk-space-table" className="empty" style={{minHeight: "5em"}}>
                    <p>No Disk Space Data</p>
                </div>
            );
        } else {
            return (
                <div id="disk-space-table">
                    <Table columns={columns} data={data}/>
                </div>
            );
        }
    }

    _fetchDataNowAndResetTimer() {
        clearInterval(this.timer);
        this._fetchData();
        this.timer = setInterval(this._fetchData, 600000);
    }

    _fetchData() {
        if (this.request) {
            this.request.cancel();
        }
        this.request = Api.getDiskSpaceTable(this.state.selectedHost, this._onDataFetched);
    }

    _onDataFetched(data, error) {
        this.request = null;
        this.setState({loading: false});
        if (!error && data["data"]) {
            this.setState({
                columns: data["columns"],
                data: data["data"],
            });
        }
    }

}

DiskSpaceTable.propTypes = {
    selectedHost: PropTypes.string.isRequired,
};

export default DiskSpaceTable;
