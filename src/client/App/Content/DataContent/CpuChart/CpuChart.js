import Api                      from "App/Api";
import { roundWithMaxDecimals } from "charts/ChartUtils";
import ShapeChart               from "charts/ShapeChart";
import PropTypes                from "prop-types";
import React                    from "react";
import autoBind                 from "react-autobind";
import LoadingSpinner           from "../../../LoadingSpinner/LoadingSpinner";
import "./CpuChart.sass";

class CpuChart extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this.state = {
            selectedHost: props.selectedHost,
            selectedMode: props.selectedMode,
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
        if (
            newProps.selectedHost !== this.state.selectedHost ||
            newProps.selectedMode !== this.state.selectedMode
        ) {
            this.setState({
                selectedHost: newProps.selectedHost,
                selectedMode: newProps.selectedMode,
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
        const {selectedMode} = this.props;
        const {data, loading} = this.state;
        if (loading) {
            return (
                <div id="cpu-chart" className="empty">
                    <LoadingSpinner/>
                </div>
            );
        } else if (data === null) {
            return (
                <div id="cpu-chart" className="empty">
                    <p>{`No ${selectedMode} CPU Data`}</p>
                </div>
            );
        } else {
            return (
                <div id="cpu-chart">
                    <ShapeChart
                        title="CPU"
                        legend={true}
                        data={data}
                        xScaleType="time"
                        y0Domain={[0, 100]}
                        y0MaxTickTextWidth={4}
                        y0TickFormat={d => roundWithMaxDecimals(d, 1) + "%"}
                        y0Title="CPU Usage"
                    />
                </div>
            );
        }
    }

    _fetchDataNowAndResetTimer() {
        clearInterval(this.timer);
        this._fetchData();
        let period;
        switch (this.state.selectedMode) {
            case "LIVE":
                period = 5000;
                break;
            default:
                period = 60000;
                break;
        }
        this.timer = setInterval(this._fetchData, period);
    }

    _fetchData() {
        if (this.request) {
            this.request.cancel();
        }
        this.request = Api.getCpuChart(this.state.selectedHost, this.state.selectedMode, this._onDataFetched);
    }

    _onDataFetched(data, error) {
        this.request = null;
        this.setState({loading: false});
        if (!error && data["data"]) {
            this.setState({data: data["data"]});
        }
    }

}

CpuChart.propTypes = {
    selectedHost: PropTypes.string.isRequired,
    selectedMode: PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"]).isRequired,
};

export default CpuChart;
