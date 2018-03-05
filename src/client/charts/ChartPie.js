import React             from "react";
import AutoBind          from "react-autobind";
import ReactFauxDom      from "react-faux-dom";
import { getColorScale } from "./ChartColors";
import d3                from "./D3";
import PropTypes         from "./PropTypes";

class ChartPie extends React.PureComponent {

    constructor(props) {
        super(props);
        AutoBind(this);
    }

    render() {
        const {data, innerRadius, size} = this.props;
        const {width, height} = size;
        const radius = Math.min(width, height) / 2;
        const {x, y} = Object.assign({x: 0, y: 0}, this.props.translate);
        const {dx, dy} = {
            dx: x + width / 2,
            dy: y + height / 2,
        };

        const fauxDom = ReactFauxDom.createElement("g");
        const group = this._createGroup(fauxDom, dx, dy);
        const arc = this._createArc(radius, innerRadius);
        const pie = this._createPie();
        const colorScale = getColorScale(this.props.theme, data.length);

        this._drawPie(data, colorScale, group, arc, pie);

        return fauxDom.toReact();
    }

    _createGroup = (fauxDom, x, y) =>
        d3
            .select(fauxDom)
            .attr("class", "pie")
            .attr("transform", `translate(${x},${y})`);

    _createArc = (radius, innerRadius) =>
        d3
            .arc()
            .outerRadius(radius)
            .innerRadius(radius * innerRadius)
            .padAngle(.02)
            .padRadius(radius)
            .cornerRadius(radius / 25);

    _createPie = () =>
        d3
            .pie()
            .sort(null)
            .value(d => d.value);

    _drawPie = (data, colorScale, group, arc, pie) =>
        group
            .selectAll("path.chunk")
            .data(pie(data))
            .enter()
            .append("path")
            .attr("class", d => `${d.data.id} arc`)
            .attr("fill", (d, i) => colorScale(i))
            .attr("d", arc);

}

ChartPie.propTypes = {
    data: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.string.isRequired,
            value: PropTypes.number.isRequired,
        }).isRequired,
    ).isRequired,
    innerRadius: PropTypes.restrictedNumber(n => n >= 0 && n <= 1).isRequired,
    size: PropTypes.size,
    theme: PropTypes.oneOf(["COLD", "HOT", "NEON"]).isRequired,
    translate: PropTypes.translate,
};

ChartPie.defaultProps = {
    translate: null,
};

export default ChartPie;
