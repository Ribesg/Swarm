import React        from "react";
import autoBind     from "react-autobind";
import sizeMe       from "react-sizeme";
import ChartAxis    from "./ChartAxis";
import ChartClip    from "./ChartClip";
import ChartLegend  from "./ChartLegend";
import ChartMouse   from "./ChartMouse";
import ChartRoot    from "./ChartRoot";
import ChartShape   from "./ChartShape";
import ChartTitle   from "./ChartTitle";
import ChartTooltip from "./ChartTooltip";
import { newScale } from "./ChartUtils";
import d3           from "./D3";
import PropTypes    from "./PropTypes";
import {
    nullShapesData,
    parseShapesData,
}                   from "./ShapeChartUtils";

class ShapeChart extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this._randomClipId = "contentClip" + Math.floor(Math.random() * 900000 + 100000);
        this._anyPropChanged = true;
        this.state = Object.assign({mouseData: null}, parseShapesData(props.data));
    }

    componentWillReceiveProps(props) {
        let anyPropChanged = false;
        if (this.props.data !== props.data) {
            anyPropChanged = true;
            if (props.data) {
                this.setState(parseShapesData(props.data));
            } else if (this.state.shapes !== null) {
                this.setState(nullShapesData);
            }
        }
        if (!anyPropChanged) {
            const keys = Object.keys(ShapeChart.propTypes);
            for (let i = 0; i < keys.length; i++) {
                const key = keys[i];
                if (this.props[key] !== props[key]) {
                    anyPropChanged = true;
                    break;
                }
            }
        }
        this._anyPropChanged = anyPropChanged;
    }

    render() {
        const {mouseData, shapes} = this.state;
        const {size, title, legend, xScaleType, xTitle, y0Title, y1Title} = this.props;
        const {xTickFormat, y0TickFormat, y1TickFormat} = this.props;

        const xDomain = this.props.xDomain || this.state.xDomain;
        const y0Domain = this.props.y0Domain || this.state.y0Domain;
        const y1Domain = this.props.y1Domain || this.state.y1Domain;

        const titleMargin = title ? ChartTitle.fontSize : 0;
        const legendSize = legend ? ChartLegend.legendSize(shapes, "HORIZONTAL") : {width: 0, height: 0};

        const chartMargins = 5;

        const noMargins = {top: 0, bottom: 0, left: 0, right: 0};
        const xAxisMargins = xDomain ? ChartAxis.axisMargins(this.props.xMaxTickTextWidth, "bottom", xTitle) : noMargins;
        const y0AxisMargins = y0Domain ? ChartAxis.axisMargins(this.props.y0MaxTickTextWidth, "left", y0Title) : noMargins;
        const y1AxisMargins = y1Domain ? ChartAxis.axisMargins(this.props.y1MaxTickTextWidth, "right", y1Title) : noMargins;
        const axesMargin = name => d3.max([xAxisMargins, y0AxisMargins, y1AxisMargins], m => m[name]);
        const axesMargins = {
            top: axesMargin("top"),
            bottom: axesMargin("bottom"),
            left: axesMargin("left"),
            right: axesMargin("right"),
        };

        const contentMargins = {
            top: titleMargin + legendSize.height + axesMargins.top + chartMargins,
            bottom: axesMargins.bottom + chartMargins,
            left: axesMargins.left + chartMargins,
            right: axesMargins.right + chartMargins,
        };
        const contentSize = {
            width: size.width - contentMargins.left - contentMargins.right,
            height: size.height - contentMargins.top - contentMargins.bottom,
        };
        const contentTranslate = {
            x: contentMargins.left,
            y: contentMargins.top,
        };

        const xScale = newScale(xScaleType, xDomain, [0, contentSize.width]);
        let y0Scale = null, y1Scale = null;
        if (y0Domain) {
            y0Scale = newScale(this.props.y0ScaleType, y0Domain, [contentSize.height, 0]);
        }
        if (y1Domain) {
            y1Scale = newScale(this.props.y1ScaleType, y1Domain, [contentSize.height, 0]);
        }

        if (this._anyPropChanged) {
            this._setupMouseHandler(contentSize, contentTranslate, shapes, xScale, y0Scale, y1Scale);
        }

        const children = [];
        this._renderClip(children, this._randomClipId, contentSize, contentTranslate);
        this._renderShapes(children, shapes, this._randomClipId, contentTranslate, xScale, xScaleType, y0Domain, y0Scale, y1Domain, y1Scale);
        this._renderXAxis(children, xScale, xTitle, xTickFormat, size, contentSize, contentMargins, axesMargins);
        this._renderY0Axis(children, y0Scale, y0Title, y0TickFormat, size, contentSize, contentMargins, axesMargins);
        this._renderY1Axis(children, y1Scale, y1Title, y1TickFormat, size, contentSize, contentMargins, axesMargins);
        this._renderLegend(children, legend, shapes, size, legendSize, contentMargins);
        this._renderTitle(children, title, size, titleMargin, chartMargins);
        this._renderMouseLine(children, contentSize, contentTranslate, mouseData);

        let xTooltipFormat = xTickFormat,
            y0TooltipFormat = y0TickFormat,
            y1TooltipFormat = y1TickFormat;
        if (!xTooltipFormat) {
            if (xScaleType === "time") {
                const diff = xDomain[1] - xDomain[0];
                let format = "%H:%M";
                if (diff % 60000 != 0) {
                    format += ":%S";
                }
                if (diff / 1000 / 60 / 60 > 24) {
                    format = "%Y-%m-%d " + format;
                }
                const d3Format = d3.timeFormat(format);
                xTooltipFormat = x => d3Format(new Date(x));
            } else {
                xTooltipFormat = x => "" + x;
            }
        }
        if (!y0TooltipFormat) {
            y0TooltipFormat = v => "" + v;
        }
        if (!y1TooltipFormat) {
            y1TooltipFormat = v => "" + v;
        }
        const tooltip = this._mouseDataToTooltip(contentSize, contentMargins, mouseData, xTooltipFormat, y0TooltipFormat, y1TooltipFormat);
        return <ChartRoot size={size} onMouse={this._onMouseMove} tooltip={tooltip}>{children}</ChartRoot>;
    }

    _renderClip = (children, clipId, size, translate) =>
        children.push(
            <ChartClip
                key="clip"
                id={clipId}
                size={size}
                translate={translate}
            />,
        );

    _renderShapes = (children, shapes, clipId, translate, xScale, xScaleType, y0Domain, y0Scale, y1Domain, y1Scale) =>
        shapes.forEach((shape, i) =>
            children.push(
                <ChartShape
                    key={`shape${i}`}
                    clipId={clipId}
                    color={shape.color}
                    curve={shape.curve}
                    fillGaps={shape.fillGaps}
                    points={shape.points}
                    shape={shape.shape}
                    translate={translate}
                    xScale={xScale}
                    xScaleType={xScaleType}
                    yDomain={shape.side === "right" ? y1Domain : y0Domain}
                    yScale={shape.side === "right" ? y1Scale : y0Scale}
                />,
            ));

    _renderAxis = (children, key, position, scale, title, tickFormat, chartSize, size, translate) =>
        children.push(
            <ChartAxis
                key={key}
                position={position}
                scale={scale}
                title={title}
                chartSize={chartSize}
                tickFormat={tickFormat}
                size={size}
                translate={translate}
            />,
        );

    _renderXAxis = (children, scale, title, tickFormat, chartSize, contentSize, contentMargins, axesMargins) => {
        const size = {
            width: contentSize.width,
            height: axesMargins.top,
        };
        const translate = {
            x: contentMargins.left,
            y: contentMargins.top + contentSize.height,
        };
        this._renderAxis(children, "xAxis", "bottom", scale, title, tickFormat, chartSize, size, translate);
    };

    _renderY0Axis = (children, scale, title, tickFormat, chartSize, contentSize, contentMargins, axesMargins) => {
        if (!scale) {
            return;
        }
        const size = {
            width: axesMargins.left,
            height: contentSize.height,
        };
        const translate = {
            x: contentMargins.left,
            y: contentMargins.top,
        };
        this._renderAxis(children, "y0Axis", "left", scale, title, tickFormat, chartSize, size, translate);
    };

    _renderY1Axis = (children, scale, title, tickFormat, chartSize, contentSize, contentMargins, axesMargins) => {
        if (!scale) {
            return;
        }
        const size = {
            width: axesMargins.right,
            height: contentSize.height,
        };
        const translate = {
            x: contentMargins.left + contentSize.width,
            y: contentMargins.top,
        };
        this._renderAxis(children, "y1Axis", "right", scale, title, tickFormat, chartSize, size, translate);
    };

    _renderLegend = (children, legend, data, chartSize, legendSize, contentMargins) => {
        if (legend) {
            children.push(
                <ChartLegend
                    key="legend"
                    chartSize={chartSize}
                    data={data}
                    mode="HORIZONTAL"
                    translate={{
                        x: contentMargins.left,
                        y: contentMargins.top - legendSize.height - 3,
                    }}
                />,
            );
        }
    };

    _renderTitle = (children, title, chartSize, titleMargin, chartMargins) => {
        if (title) {
            children.push(
                <ChartTitle
                    key="title"
                    title={title}
                    translate={{
                        x: chartSize.width / 2,
                        y: titleMargin + chartMargins,
                    }}
                />,
            );
        }
    };

    _setupMouseHandler = (contentSize, contentTranslate, shapes, xScale, y0Scale, y1Scale) => {
        this._mouseHandler = new ChartMouse({
            chartData: shapes,
            contentSize: contentSize,
            contentTranslate: contentTranslate,
            mode: "1d",
            xScale: xScale,
            y0Scale: y0Scale,
            y1Scale: y1Scale,
        });
    };

    _onMouseMove = pos => {
        const md = this._mouseHandler.getMouseData(pos);
        this.setState({mouseData: md});
    };

    _renderMouseLine = (children, contentSize, contentTranslate, mouseData) => {
        if (mouseData != null) {
            children.push(
                <line
                    key="mouseLine"
                    x1={contentTranslate.x + mouseData.xPos}
                    y1={contentTranslate.y}
                    x2={contentTranslate.x + mouseData.xPos}
                    y2={contentTranslate.y + contentSize.height}
                    stroke="#777"
                />,
            );
        }
    };

    _mouseDataToTooltip = (contentSize, contentMargins, mouseData, xTickFormat, y0TickFormat, y1TickFormat) => {
        if (!mouseData) {
            return null;
        }
        const margin = 5;
        const position = {top: contentMargins.top + margin};
        if (mouseData.xPos > contentSize.width / 2) {
            position.right = contentMargins.right + contentSize.width - mouseData.xPos + margin;
        } else {
            position.left = contentMargins.left + mouseData.xPos + margin;
        }
        const title = xTickFormat(mouseData.x);
        const lines = mouseData.data.filter(d => d != null).map(d => ({
            color: d.color,
            legend: d.legend,
            value: d.side === "right" ? y1TickFormat(d.y) : y0TickFormat(d.y),
        }));
        return (
            <ChartTooltip
                lines={lines}
                position={position}
                title={title}
            />
        );
    };

}

ShapeChart.propTypes = {
    legend: PropTypes.bool,
    data: PropTypes.arrayOf(
        PropTypes.shape({
            color: PropTypes.string.isRequired,
            curve: PropTypes.oneOf(["Linear", "MonotoneX", "CatmullRom"]),
            legend: PropTypes.string,
            side: PropTypes.oneOf(["left", "right"]).isRequired,
            shape: PropTypes.oneOf(["area", "line", "symbol"]).isRequired,
            stackId: PropTypes.string.isNullableRequired,
            points: PropTypes.arrayOf(
                PropTypes.shape({
                    x: PropTypes.number.isRequired,
                    y: PropTypes.number.isNullableRequired,
                }).isRequired,
            ).isRequired,
        }).isRequired,
    ).isRequired,
    size: PropTypes.size,
    title: PropTypes.string,
    xDomain: PropTypes.chartDomain,
    xMaxTickTextWidth: PropTypes.positiveNumber,
    xScaleType: PropTypes.oneOf(["time", "linear"]),
    xTickFormat: PropTypes.func,
    xTitle: PropTypes.string,
    y0Domain: PropTypes.chartDomain,
    y0MaxTickTextWidth: PropTypes.positiveNumber,
    y0ScaleType: PropTypes.oneOf(["linear"]),
    y0TickFormat: PropTypes.func,
    y0Title: PropTypes.string,
    y1Domain: PropTypes.chartDomain,
    y1MaxTickTextWidth: PropTypes.positiveNumber,
    y1ScaleType: PropTypes.oneOf(["linear"]),
    y1TickFormat: PropTypes.func,
    y1Title: PropTypes.string,
};

ShapeChart.defaultProps = {
    legend: false,
    title: null,
    xDomain: null,
    xMaxTickTextWidth: 7,
    xScaleType: "linear",
    xTickFormat: null,
    xTitle: null,
    y0Domain: null,
    y0MaxTickTextWidth: 7,
    y0ScaleType: "linear",
    y0TickFormat: null,
    y0Title: null,
    y1Domain: null,
    y1MaxTickTextWidth: 7,
    y1ScaleType: "linear",
    y1TickFormat: null,
    y1Title: null,
};

export default sizeMe({monitorHeight: true})(ShapeChart);
