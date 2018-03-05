import Api        from "App/Api";
import ShapeChart from "charts/ShapeChart";
import PropTypes  from "prop-types";
import React      from "react";
import autoBind   from "react-autobind";
import "./RamChart.sass";

class RamChart extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this.state = {
            selectedHost: props.selectedHost,
            selectedMode: props.selectedMode,
            data: null,
            loading: true,
            maxRam: 0,
            maxSwap: 0,
        };
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
                maxRam: 0,
                maxSwap: 0,
            }, this._fetchDataNowAndResetTimer);
        }
    }

    componentWillUnmount() {
        clearInterval(this.timer);
        this.timer = null;
        this.request.abort();
        this.request = null;
    }

    render() {
        const {selectedMode} = this.props;
        if (this.state.data === null) {
            return (
                <div id="ram-chart" className="empty">
                    <p>{this.state.loading ? "Loading..." : `No ${selectedMode} RAM Data`}</p>
                </div>
            );
        } else {
            return (
                <div id="ram-chart">
                    <ShapeChart
                        title={this.state.maxSwap > 0 ? "RAM & SWAP" : "RAM"}
                        legend={true}
                        data={this.state.data}
                        xScaleType="time"
                        y0Domain={[0, this.state.maxRam]}
                        y0MaxTickTextWidth={7}
                        y0TickFormat={this._formatYAxisLabel}
                        y0Title="RAM Used"
                        y1Domain={this.state.maxSwap > 0 ? [0, this.state.maxSwap] : null}
                        y1MaxTickTextWidth={7}
                        y1TickFormat={this._formatYAxisLabel}
                        y1Title="SWAP Used"
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
        this.request = Api.getRamChart(this.state.selectedHost, this.state.selectedMode, this._onDataFetched);
    }

    _onDataFetched(data, error) {
        this.request = null;
        this.setState({loading: false});
        if (!error && data["data"]) {
            this.setState({
                data: data["data"],
                maxRam: data["maxRam"],
                maxSwap: data["maxSwap"],
            });
        }
    }

    _formatYAxisLabel = d => {
        if (d === 0) {
            return "0";
        } else {
            const units = ["k", "M", "G", "T", "P", "E", "Z", "Y"];
            let unit = 0;
            let val = d;

            while (val >= 10000) {
                unit++;
                val = Math.round(val / 1000);
            }

            return `${val} ${units[Math.min(units.length - 1, unit)]}o`;
        }
    };

}

RamChart.propTypes = {
    selectedHost: PropTypes.string.isRequired,
    selectedMode: PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"]).isRequired,
};

export default RamChart;
