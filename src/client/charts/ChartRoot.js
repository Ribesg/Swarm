import React     from "react";
import AutoBind  from "react-autobind";
import ReactDOM  from "react-dom";
import PropTypes from "./PropTypes";

class ChartRoot extends React.PureComponent {

    constructor(props) {
        super(props);
        AutoBind(this);
    }

    render() {
        const wrappingDivStyle = Object.assign({}, ChartRoot.defaultProps.divStyle, this.props.divStyle);
        const {children, onMouse, size, tooltip} = this.props;
        return (
            <div style={wrappingDivStyle}>
                <svg
                    baseProfile="full"
                    version="1.1"
                    viewBox={`0 0 ${size.width} ${size.height}`}
                    xmlns="http://www.w3.org/2000/svg"
                    onMouseEnter={onMouse ? this._onMouseEnterOrMove : null}
                    onMouseMove={onMouse ? this._onMouseEnterOrMove : null}
                    onMouseLeave={onMouse ? this._onMouseLeave : null}
                    style={{
                        width: "100%",
                        height: "100%",
                        flex: "1 1 auto",
                    }}
                >
                    {children}
                </svg>
                {tooltip}
            </div>
        );
    }

    _getMousePosition(event) {
        const bounds = ReactDOM.findDOMNode(this).getBoundingClientRect();
        return [event.pageX - bounds.left, event.pageY - bounds.top];
    }

    _onMouseEnterOrMove(event) {
        this.props.onMouse(this._getMousePosition(event));
    }

    _onMouseLeave() {
        this.props.onMouse(null);
    }

}

ChartRoot.propTypes = {
    divStyle: PropTypes.object,
    onMouse: PropTypes.func,
    size: PropTypes.size,
    tooltip: PropTypes.element,
};

ChartRoot.defaultProps = {
    divStyle: {
        position: "relative",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
    },
    tooltip: null,
};

export default ChartRoot;
