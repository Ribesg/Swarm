import React    from "react";
import AutoBind from "react-autobind";

class ChartMouse {

    constructor(props) {
        // TODO Check that props contains everything needed
        this.props = props;
        AutoBind(this);
        this._setupData();
    }

    getMouseData = (mouse) => {
        const {contentSize, contentTranslate, mode} = this.props;
        if (mouse) {
            const x = mouse[0] - contentTranslate.x;
            const y = mouse[1] - contentTranslate.y;
            if (x < 0 || x > contentSize.width || y < 0 || y > contentSize.height) {
                return null;
            }
            switch (mode) {
                case "1d":
                    return this._findInXArray(x);
                case "2d":
                    throw new Error("Not Implemented"); // TODO
                default:
                    throw new Error(`Unknown mode ${mode}`);
            }
        }
        return null;
    };

    _setupData = () => {
        const mode = this.props.mode;
        this._xArray = null;
        this._voronoi = null;
        switch (mode) {
            case "1d":
                this._setupXArray();
                break;
            case "2d":
                this._setupVoronoi();
                break;
            default:
                throw new Error(`Unknown mode ${mode}`);
        }
    };

    _setupXArray() {
        const props = this.props;
        const data = props.chartData;
        const xValues = this._getAllXValues(data);
        const xArray = [];
        xValues.forEach(x => {
            const xValueObject = {
                x: x,
                xPos: props.xScale(x),
                data: [],
            };
            data.forEach(shape => {
                const point = shape.points.find(p => p.x == x);
                const hasY = point && typeof point.y === "number" && (typeof point.y0 !== "number" || point.y - point.y0 > 0);
                xValueObject.data.push(!hasY ? null : {
                    color: shape.color,
                    legend: shape.legend,
                    side: shape.side,
                    y: typeof point.yValue === "number" ? point.yValue : point.y,
                });
            });
            xArray.push(xValueObject);
        });
        this._xArray = xArray;
    }

    _setupVoronoi() {
        throw new Error("Not Implemented");
    }

    _findInXArray(x) {
        const xValue = this.props.xScale.invert(x);
        const firstHighIndex = this._xArray.findIndex(e => e.x > xValue);
        if (firstHighIndex <= 0) {
            return null;
        } else {
            const a = this._xArray[firstHighIndex - 1];
            const b = this._xArray[firstHighIndex];
            return xValue - a.x < b.x - xValue ? a : b;
        }
    }

    _getAllXValues = (data) => data
        .map(shape => shape.points)
        .reduce((res, points) => res.concat(points.map(p => +p.x)), [])
        .filter((v, i, a) => a.indexOf(v) === i) // .distinct()
        .sort((a, b) => a - b); // ascending

}

export default ChartMouse;
