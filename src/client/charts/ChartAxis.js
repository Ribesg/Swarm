import React        from "react";
import AutoBind     from "react-autobind";
import ReactFauxDom from "react-faux-dom";
import {
    axisTickFontFamily,
    axisTitleFontFamily,
    lineHeight,
}                   from "./ChartConstants";
import d3           from "./D3";
import PropTypes    from "./PropTypes";
import TextMeasurer from "./TextMeasurer";

class ChartAxis extends React.PureComponent {

    static tickFontSize = 10;

    static tickTextHeight = ChartAxis.tickFontSize * .71 + 9;

    static titleFontSize = 14;

    static axisMargins = (maxTickTextWidth, position, title) => {
        const maxTickText = d3.range(maxTickTextWidth).map(() => "M").join("");
        const width = TextMeasurer.getWidth(maxTickText, axisTickFontFamily, ChartAxis.tickFontSize);
        const height = ChartAxis.tickTextHeight;
        const titleHeight = title ? ChartAxis.titleFontSize * lineHeight + 3 : 0;
        switch (position) {
            case "top":
                return {
                    top: height + titleHeight + 1,
                    bottom: 0,
                    left: width / 2,
                    right: width / 2,
                };
            case "bottom":
                return {
                    top: 0,
                    bottom: height + titleHeight + 1,
                    left: width / 2,
                    right: width / 2,
                };
            case "left":
                return {
                    top: height / 2,
                    bottom: height / 2,
                    left: width + 9 + titleHeight,
                    right: 0,
                };
            case "right":
                return {
                    top: height / 2,
                    bottom: height / 2,
                    left: 0,
                    right: width + 9 + titleHeight,
                };
            default:
                throw new Error(`Unknown position: ${position}`);
        }
    };

    constructor(props) {
        super(props);
        AutoBind(this);
    }

    render() {
        const {position, scale, size, tickFormat, title} = this.props;
        const {x, y} = Object.assign({x: 0, y: 0}, this.props.translate);

        const ticks = this._getTicks(position, size);
        const axis = this._createAxis(position, scale, tickFormat, ticks);

        const fauxDom = ReactFauxDom.createElement("g");
        const group = this._createGroup(fauxDom, x, y, ChartAxis.tickFontSize);

        this._renderAxis(axis, group);
        this._renderTitle(group, position, size, title, ChartAxis.titleFontSize);

        return fauxDom.toReact();
    }

    _getTicks = (position, size) => {
        switch (position) {
            case "top":
            case "bottom":
                return Math.round(size.width / 80);
            case "left":
            case "right":
                return Math.round(size.height / 40);
            default:
                throw new Error(`Unknown position: ${position}`);
        }
    };

    _createAxis = (position, scale, tickFormat, ticks) => {
        switch (position) {
            case "top":
                return d3
                    .axisTop(scale)
                    .tickFormat(tickFormat)
                    .ticks(ticks);
            case "bottom":
                return d3
                    .axisBottom(scale)
                    .tickFormat(tickFormat)
                    .ticks(ticks);
            case "left":
                return d3
                    .axisLeft(scale)
                    .tickFormat(tickFormat)
                    .ticks(ticks);
            case "right":
                return d3
                    .axisRight(scale)
                    .tickFormat(tickFormat)
                    .ticks(ticks);
            default:
                throw new Error(`Unknown position: ${position}`);
        }
    };

    _createGroup = (fauxDom, x, y, fontSize) => d3
        .select(fauxDom)
        .attr("style", {
            cursor: "default",
            font: `${fontSize}px/${lineHeight} ${axisTickFontFamily}`,
        })
        .attr("transform", `translate(${x},${y})`);

    _renderAxis = (axis, group) =>
        group.call(axis);

    _renderTitle = (group, position, size, title, titleFontSize) => {
        let x, y, r;
        switch (position) {
            case "top":
                x = size.width / 2;
                y = -size.height + titleFontSize * .85;
                r = 0;
                break;
            case "bottom":
                x = size.width / 2;
                y = size.height - titleFontSize * .15;
                r = 0;
                break;
            case "left":
                x = -size.height / 2;
                y = -size.width + titleFontSize * .85;
                r = -90;
                break;
            case "right":
                x = size.height / 2;
                y = -size.width + titleFontSize * .85;
                r = 90;
                break;
            default:
                throw new Error(`Unknown position: ${position}`);
        }
        group
            .append("text")
            .attr("text-anchor", "middle")
            .attr("x", x)
            .attr("y", y)
            .attr("transform", `rotate(${r})`)
            .attr("style", {
                cursor: "default",
                font: `${titleFontSize}px/${lineHeight} ${axisTitleFontFamily}`,
            })
            .attr("fill", "black")
            .text(title);
    };

}

ChartAxis.propTypes = {
    chartSize: PropTypes.size,
    position: PropTypes.oneOf(["top", "left", "bottom", "right"]).isRequired,
    scale: PropTypes.func.isRequired,
    size: PropTypes.size,
    tickFormat: PropTypes.func,
    title: PropTypes.string,
    translate: PropTypes.translate,
};

ChartAxis.defaultProps = {
    tickFormat: null,
    title: null,
    translate: null,
};

export default ChartAxis;
