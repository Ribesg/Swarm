import React     from "react";
import AutoBind  from "react-autobind";
import {
    chartTitleFontFamily,
    lineHeight,
}                from "./ChartConstants";
import PropTypes from "./PropTypes";

class ChartTitle extends React.PureComponent {

    static fontSize = 20;

    constructor(props) {
        super(props);
        AutoBind(this);
    }

    render() {
        const {x, y} = Object.assign({x: 0, y: 0}, this.props.translate);
        const {anchor, title} = this.props;
        const style = {
            cursor: "default",
            font: `${ChartTitle.fontSize}px/${lineHeight} ${chartTitleFontFamily}`,
        };
        return <text className="title" x={x} y={y} textAnchor={anchor} style={style}>{title}</text>;
    }

}

ChartTitle.propTypes = {
    anchor: PropTypes.oneOf(["start", "middle", "end"]),
    title: PropTypes.string.isRequired,
    translate: PropTypes.translate,
};

ChartTitle.defaultProps = {
    anchor: "middle",
    translate: null,
};

export default ChartTitle;
