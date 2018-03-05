import React       from "react";
import SizeMe      from "react-sizeme";
import ChartLegend from "./ChartLegend";
import ChartPie    from "./ChartPie";
import ChartRoot   from "./ChartRoot";
import ChartTitle  from "./ChartTitle";
import PropTypes   from "./PropTypes";

// TODO Update this to the latest changes in Chart* components
class CircularChart extends React.PureComponent {

    render() {
        const children = [];
        const {data, divStyle, margin, size, title, theme, type} = this.props;
        const minSize = Math.min(size.width, size.height);
        const actualMargin = margin * minSize;
        const pieInnerRadius = type === "PIE" ? .0667 : .667;
        const pieSize = {
            width: size.width - actualMargin * 2,
            height: size.height - actualMargin * 2,
        };
        const pieTranslate = {
            x: actualMargin,
            y: actualMargin,
        };
        if (title) {
            const titleHeight = ChartTitle.fontSizeFromChartSize(size);
            pieTranslate.y += titleHeight * 1.1;
            pieSize.height -= titleHeight * 1.1;
            const titleTranslate = {
                x: size.width / 2,
                y: pieTranslate.y / 2 + titleHeight / 2,
            };
            children.push(
                <ChartTitle
                    key="title"
                    chartSize={size}
                    title={title}
                    translate={titleTranslate}
                />,
            );
        }
        children.unshift(
            <ChartPie
                key="pie"
                data={data}
                innerRadius={pieInnerRadius}
                size={pieSize}
                theme={theme}
                translate={pieTranslate}
            />,
        );
        children.push(
            <ChartLegend
                key="legend"
                chartSize={size}
                data={data}
                mode={Math.random() < .5 ? "HORIZONTAL" : "VERTICAL"}
                theme={theme}
            />,
        );
        return <ChartRoot divStyle={divStyle} size={size}>{children}</ChartRoot>;
    }

}

CircularChart.propTypes = {
    data: PropTypes.arrayOf(
        PropTypes.shape({
            id: PropTypes.string.isRequired,
            legend: PropTypes.string,
            value: PropTypes.number.isRequired,
        }).isRequired,
    ).isRequired,
    divStyle: PropTypes.object,
    margin: PropTypes.restrictedNumber(n => n >= 0 && n <= .5),
    size: PropTypes.size,
    title: PropTypes.string,
    theme: PropTypes.oneOf(["COLD", "HOT", "NEON"]),
    type: PropTypes.oneOf(["DONUT", "PIE"]),
};

CircularChart.defaultProps = {
    divStyle: null,
    margin: .05,
    title: null,
    theme: "HOT",
    type: "DONUT",
};

export default SizeMe({monitorHeight: true})(CircularChart);
