import React        from "react";
import AutoBind     from "react-autobind";
import ReactFauxDom from "react-faux-dom";
import {
    legendFontFamily,
    lineHeight,
}                   from "./ChartConstants";
import d3           from "./D3";
import PropTypes    from "./PropTypes";
import TextMeasurer from "./TextMeasurer";

class ChartLegend extends React.PureComponent {

    static fontSize = 11;

    static boxSize = {
        width: ChartLegend.fontSize * lineHeight * 1.618,
        height: ChartLegend.fontSize * lineHeight,
    };

    static legendSize = (data, mode) => {
        const amount = data.length;
        const margin = ChartLegend.boxSize.width * .1;
        const dataTextWidth = data.map(d => TextMeasurer.getWidth(d.legend, legendFontFamily, ChartLegend.fontSize));
        if (mode === "HORIZONTAL") {
            const marginBetweenElements = margin * 5;
            const sumTextWidth = dataTextWidth.reduce((sum, width) => sum + width, 0);
            const width = sumTextWidth + amount * (ChartLegend.boxSize.width + margin) + (amount - 1) * marginBetweenElements;
            const height = ChartLegend.boxSize.height;
            return {width, height};
        } else if (mode === "VERTICAL") {
            const maxTextWidth = dataTextWidth.reduce((max, width) => Math.max(max, width), Number.MIN_VALUE);
            const width = ChartLegend.boxSize.width + margin + maxTextWidth;
            const height = amount * ChartLegend.boxSize.height + (amount - 1) * margin;
            return {width, height};
        } else {
            throw new Error(`Unknown mode: ${mode}`);
        }
    };

    constructor(props) {
        super(props);
        AutoBind(this);
    }

    render() {
        const {data, mode} = this.props;
        const {x, y} = Object.assign({x: 0, y: 0}, this.props.translate);
        const margin = ChartLegend.boxSize.width * .1;

        const fauxDom = ReactFauxDom.createElement("g");
        const group = this._createGroup(fauxDom, x, y);

        switch (mode) {
            case "HORIZONTAL":
                this._renderHorizontal(group, data, ChartLegend.fontSize, ChartLegend.boxSize, margin);
                break;
            case "VERTICAL":
                this._renderVertical(group, data, ChartLegend.fontSize, ChartLegend.boxSize, margin);
                break;
            default:
                throw new Error(`Unknown mode '${mode}'`);
        }

        return fauxDom.toReact();
    }

    _createGroup = (fauxDom, x, y) =>
        d3
            .select(fauxDom)
            .attr("transform", `translate(${x},${y})`);

    _renderHorizontal = (group, data, fontSize, boxSize, margin) => {
        const dataTextWidth = data.map(d => TextMeasurer.getWidth(d.legend, legendFontFamily, fontSize));
        const dataTextWidthSummer = (i) => dataTextWidth.slice(0, i).reduce((sum, width) => sum + width, 0);
        const marginBetweenElements = margin * 5;
        group
            .selectAll("rect")
            .data(data)
            .enter()
            .append("rect")
            .attr("x", (_, i) => dataTextWidthSummer(i) + i * (boxSize.width + margin + marginBetweenElements))
            .attr("y", 0)
            .attr("width", boxSize.width)
            .attr("height", boxSize.height)
            .attr("fill", d => d.color)
            .attr("stroke-width", 0);
        group
            .selectAll("text")
            .data(data)
            .enter()
            .append("text")
            .attr("x", (_, i) => dataTextWidthSummer(i) + i * (boxSize.width + margin + marginBetweenElements) + boxSize.width + margin)
            .attr("y", boxSize.height - (boxSize.height + 2 - fontSize) / 2)
            .attr("style", {
                cursor: "default",
                font: `${fontSize}px/${lineHeight} ${legendFontFamily}`,
            })
            .text(d => d.legend);
    };

    _renderVertical = (group, data, fontSize, boxSize, margin) => {
        group
            .selectAll("rect")
            .data(data)
            .enter()
            .append("rect")
            .attr("x", 0)
            .attr("y", (_, i) => i * (boxSize.height + margin))
            .attr("width", boxSize.width)
            .attr("height", boxSize.height)
            .attr("fill", d => d.color);
        group
            .selectAll("text")
            .data(data)
            .enter()
            .append("text")
            .attr("x", boxSize.width + margin)
            .attr("y", (_, i) => boxSize.height + i * (boxSize.height + margin) - (boxSize.height - fontSize) / 2)
            .attr("style", {
                cursor: "default",
                font: `${fontSize}px/${lineHeight} ${legendFontFamily}`,
            })
            .text(d => d.legend);
    };

}

ChartLegend.propTypes = {
    chartSize: PropTypes.size,
    data: PropTypes.arrayOf(
        PropTypes.shape({
            color: PropTypes.string.isRequired,
            legend: PropTypes.string.isRequired,
        }).isRequired,
    ).isRequired,
    mode: PropTypes.oneOf(["HORIZONTAL", "VERTICAL"]).isRequired,
    translate: PropTypes.translate,
};

ChartLegend.defaultProps = {
    translate: {x: 0, y: 0},
};

export default ChartLegend;
