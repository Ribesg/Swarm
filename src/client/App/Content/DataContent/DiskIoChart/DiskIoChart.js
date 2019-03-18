import Api                      from "App/Api";
import { roundWithMaxDecimals } from "charts/ChartUtils";
import ShapeChart               from "charts/ShapeChart";
import PropTypes                from "prop-types";
import React                    from "react";
import autoBind                 from "react-autobind";
import LoadingSpinner           from "../../../LoadingSpinner/LoadingSpinner";
import "./DiskIoChart.sass";

class DiskIoChart extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this.state = {
            selectedHost: props.selectedHost,
            selectedMode: props.selectedMode,
            data: null,
            loading: true,
            maxUsage: null,
            maxSpeed: null,
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
                maxUsage: null,
                maxSpeed: null,
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
        const {data, loading, maxUsage, maxSpeed} = this.state;
        if (loading) {
            return (
                <div id="disk-io-chart" className="empty">
                    <LoadingSpinner/>
                </div>
            );
        } else if (data === null) {
            return (
                <div id="disk-io-chart" className="empty">
                    <p>{`No ${selectedMode} Disk I/O Data`}</p>
                </div>
            );
        } else {
            return (
                <div id="disk-io-chart">
                    <ShapeChart
                        title={"Disk IO"}
                        legend={true}
                        data={data}
                        xScaleType="time"
                        y0Domain={[0, maxUsage]}
                        y0MaxTickTextWidth={4}
                        y0TickFormat={d => roundWithMaxDecimals(d, 1) + "%"}
                        y0Title="Usage"
                        y1Domain={[0, maxSpeed]}
                        y1MaxTickTextWidth={9}
                        y1TickFormat={this._formatY1AxisLabel}
                        y1Title="Speed"
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
        this.request = Api.getDiskIoChart(this.state.selectedHost, this.state.selectedMode, this._onDataFetched);
    }

    _onDataFetched(data, error) {
        this.request = null;
        this.setState({loading: false});
        if (!error && data["data"]) {
            this.setState({
                data: data["data"],
                maxUsage: data["maxUsage"],
                maxSpeed: data["maxSpeed"],
            });
        }
    }

    _formatY1AxisLabel = d => {
        if (d === 0) {
            return "0";
        } else {
            const units = ["", "k", "M", "G", "T", "P", "E", "Z", "Y"];
            let unit = 0;
            let val = d;

            while (val >= 10000) {
                unit++;
                val = Math.round(val / 1000);
            }

            return `${val} ${units[Math.min(units.length - 1, unit)]}o/s`;
        }
    };

}

DiskIoChart.propTypes = {
    selectedHost: PropTypes.string.isRequired,
    selectedMode: PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"]).isRequired,
};

export default DiskIoChart;
