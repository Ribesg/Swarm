import React        from "react";
import AutoBind     from "react-autobind";
import ReactFauxDom from "react-faux-dom";
import d3           from "./D3";
import PropTypes    from "./PropTypes";

class ChartShape extends React.PureComponent {

    constructor(props) {
        super(props);
        AutoBind(this);
    }

    render() {
        const {clipId, color, curve, points, shape, xScale, xScaleType, yDomain, yScale} = this.props;
        const {x, y} = Object.assign({x: 0, y: 0}, this.props.translate);

        const fauxDom = ReactFauxDom.createElement("g");
        const group = this._createGroup(fauxDom, x, y, clipId);

        if (shape === "symbol") {
            this._drawSymbols(group, color, points, xScale, xScaleType, yScale);
        } else {
            const shapeFun = this._createShape(curve, shape, xScale, xScaleType, yDomain, yScale);
            this._drawShape(group, color, points, shape, shapeFun);
        }

        return fauxDom.toReact();
    }

    _createGroup = (fauxDom, x, y, clipId) =>
        d3
            .select(fauxDom)
            .attr("clip-path", `url(#${clipId})`)
            .attr("transform", `translate(${x},${y})`);

    _curveStringToD3Curve = (curve) => {
        if (!curve) {
            curve = "CatmullRom";
        }
        switch (curve) {
            case "Linear":
                return d3.curveLinear;
            case "MonotoneX":
                return d3.curveMonotoneX;
            case "CatmullRom":
                return d3.curveCatmullRom;
            default:
                throw new Error(`Unsupported curve type "${curve}"`);
        }
    };

    _createShape = (curve, shape, xScale, xScaleType, yDomain, yScale) => {
        const xFun = xScaleType === "time" ?
            d => xScale(new Date(d.x)) :
            d => xScale(d.x);
        const curveFunction = this._curveStringToD3Curve(curve);
        switch (shape) {
            case "area":
                return d3
                    .area()
                    .curve(curveFunction)
                    .defined(d => d.y != null)
                    .x(xFun)
                    .y0(d => d.y0 == null ? yScale(yDomain[0]) : yScale(d.y0))
                    .y1(d => yScale(d.y));
            case "line":
                return d3
                    .line()
                    .curve(curveFunction)
                    .defined(d => d.y != null)
                    .x(xFun)
                    .y(d => yScale(d.y));
            default:
                throw new Error(`Unknown shape: ${shape}`);
        }
    };

    _drawShape = (group, color, points, shape, shapeFun) => {
        const selection = group
            .append("path")
            .datum(points)
            .attr("d", shapeFun);
        switch (shape) {
            case "area":
                selection
                    .attr("fill", color)
                    .attr("stroke-width", 0);
                break;
            case "line":
                selection
                    .attr("fill", "none")
                    .attr("stroke", color)
                    .attr("stroke-width", 2);
                break;
            default:
                throw new Error(`Unknown shape: ${shape}`);
        }
    };

    _drawSymbols = (group, color, points, xScale, xScaleType, yScale) => {
        const xFun = xScaleType === "time" ?
            d => xScale(new Date(d.x)) :
            d => xScale(d.x);
        const symbolPath = d3.symbol().size(24)();
        group
            .selectAll("path")
            .data(points)
            .enter()
            .append("path")
            .attr("d", symbolPath)
            .attr("transform", d => `translate(${xFun(d)},${yScale(d.y)})`)
            .attr("fill", color)
            .attr("stroke-width", 0);
    };

}

ChartShape.propTypes = {
    clipId: PropTypes.string,
    color: PropTypes.string,
    curve: PropTypes.oneOf(["Linear", "MonotoneX", "CatmullRom"]),
    points: PropTypes.arrayOf(
        PropTypes.shape({
            x: PropTypes.number.isRequired,
            y0: PropTypes.number,
            y: PropTypes.number.isNullableRequired,
        }).isRequired,
    ).isRequired,
    shape: PropTypes.oneOf(["area", "line", "symbol"]).isRequired,
    translate: PropTypes.translate,
    xScale: PropTypes.func.isRequired,
    xScaleType: PropTypes.oneOf(["time", "linear"]).isRequired,
    yDomain: PropTypes.chartDomain.isRequired,
    yScale: PropTypes.func,
};

ChartShape.defaultProps = {
    clipId: null,
    color: "black",
    curve: "CatmullRom",
    translate: null,
};

export default ChartShape;
