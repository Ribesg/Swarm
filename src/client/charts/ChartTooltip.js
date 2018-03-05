import React     from "react";
import autoBind  from "react-autobind";
import {
    lineHeight,
    tooltipFontFamily,
}                from "./ChartConstants";
import PropTypes from "./PropTypes";

class ChartTooltip extends React.PureComponent {

    static _titleFontSize = 14;

    static _lineFontSize = 11;

    static _baseDivStyle = {
        background: "white",
        border: "1px solid #CCC",
        borderRadius: 3,
        paddingTop: 5,
        paddingRight: 5,
        paddingBottom: 2,
        paddingLeft: 5,
        position: "absolute",
    };

    static _titleStyle = {
        font: `bold ${ChartTooltip._titleFontSize}px/${lineHeight} ${tooltipFontFamily}`,
        marginBottom: 5,
    };

    static _lineStyle = {
        font: `${ChartTooltip._lineFontSize}px/${lineHeight} ${tooltipFontFamily}`,
        marginTop: 1,
        marginBottom: 4,
    };

    constructor(props) {
        super(props);
        autoBind(this);
    }

    render() {
        const {alignValues, hideColors, hideLegends, lines, position, title} = this.props;

        this._checkPosition(position);
        const style = this._buildDivStyle(position);

        const children = [];

        if (title) {
            children.push(
                <p key="title" style={ChartTooltip._titleStyle}>{title}</p>,
            );
        }

        const columns = [];
        const columnsStyle = {display: "flex", flexDirection: "row"};

        if (!hideColors) {
            const rectSize = ChartTooltip._lineFontSize + 3;
            const rectKey = i => "rect" + i;
            const rectStyle = line => ({width: rectSize, height: rectSize, background: line.color, marginBottom: 2});
            const colors = lines.map((l, i) => <div key={rectKey(i)} style={rectStyle(l)}/>);
            columns.push(<div key="colors" style={{marginRight: 3}}>{colors}</div>);
        }

        if (!hideLegends) {
            const legendKey = i => "legend" + i;
            const legendStyle = ChartTooltip._lineStyle;
            const legends = lines.map((l, i) => <p key={legendKey(i)} style={legendStyle}>{`${l.legend}:`}</p>);
            columns.push(<div key="legends" style={{marginRight: 3}}>{legends}</div>);
        }

        const valuesStyle = {textAlign: alignValues};
        const valueKey = i => "value" + i;
        const valueStyle = ChartTooltip._lineStyle;
        const values = lines.map((l, i) => <p key={valueKey(i)} style={valueStyle}>{l.value}</p>);
        columns.push(<div key="values" style={valuesStyle}>{values}</div>);

        children.push(<div key="columns" style={columnsStyle}>{columns}</div>);

        return <div style={style}>{children}</div>;
    }

    _checkPosition = position => {
        const keys = Object.keys(position);
        if (keys.length !== 2) {
            throw new Error(`Invalid position: wrong amount of entries (${JSON.stringify(position)})`);
        }
        for (let i = 0; i < 2; i++) {
            const key = keys[i];
            if (typeof position[key] !== "number") {
                throw new Error(`Invalid position: value for key '${key}' is not a number (${JSON.stringify(position)})`);
            }
        }
    };

    _buildDivStyle = position =>
        Object.assign({}, ChartTooltip._baseDivStyle, position);

}

ChartTooltip.propTypes = {
    alignValues: PropTypes.oneOf(["left", "right"]),
    hideColors: PropTypes.bool,
    hideLegends: PropTypes.bool,
    lines: PropTypes.arrayOf(
        PropTypes.shape({
            color: PropTypes.string.isRequired,
            legend: PropTypes.string.isRequired,
            value: PropTypes.string.isRequired,
        }).isRequired,
    ).isRequired,
    position: PropTypes.shape({
        bottom: PropTypes.number,
        left: PropTypes.number,
        right: PropTypes.number,
        top: PropTypes.number,
    }).isRequired,
    title: PropTypes.string,
};

ChartTooltip.defaultProps = {
    alignValues: "right",
    hideColors: false,
    hideLegends: false,
    title: null,
};

export default ChartTooltip;
