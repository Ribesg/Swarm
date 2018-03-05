import d3 from "./D3";

const nullShapesData = {
    shapes: null,
    xDomain: null,
    y0Domain: null,
    y1Domain: null,
};

/**
 * Parses and reworks data passed to ShapeChart.
 *
 * @param {Object[]} data - The data as passed to the ShapeChart, matching the PropType
 */
const parseShapesData = data => {

    const resultData = {
        shapes: [],
        xDomains: [],
        y0Domains: [],
        y1Domains: [],
    };

    const stacksData = data.filter(d => d.stackId);
    const stackIds = stacksData.map(d => d.stackId).filter((id, i, a) => a.indexOf(id) === i);
    stackIds.forEach(stackId => {

        const stackData = stacksData.filter(d => d.stackId === stackId);
        _checkStackDataConsistency(stackData);
        const stackPointsObject = _computeStackPointsObjects(stackData);
        const allXValues = _computeAllXValues(stackPointsObject);
        const keys = _generateKeys(stacksData);
        const stackPoints = _computeStackPoints(allXValues, keys, stackPointsObject);
        _nullifyHoles(stackPoints, keys);
        const d3StackData = _computeD3StackData(keys, stackPoints);
        const stackShapes = _computeStackShapes(stackData, stackPoints, d3StackData, allXValues);
        stackShapes.forEach(shape => resultData.shapes.push(shape));

        resultData.xDomains.push([allXValues[0], allXValues[allXValues.length - 1]]);
        const yDomain = _computeYDomain(stackPoints, keys);
        if (stackData[0].side === "right") {
            resultData.y1Domains.push(yDomain);
        } else {
            resultData.y0Domains.push(yDomain);
        }

    });

    const basicShapes = data.filter(d => !d.stackId);
    basicShapes.forEach(shape => {
        resultData.shapes.push(shape);
        resultData.xDomains.push(d3.extent(shape.points, p => p.x));
        const yDomain = d3.extent(shape.points, p => p.y);
        if (shape.side === "right") {
            resultData.y1Domains.push(yDomain);
        } else {
            resultData.y0Domains.push(yDomain);
        }
    });

    const resultXDomain = _mergeDomains(resultData.xDomains);
    const resultY0Domain = _mergeDomains(resultData.y0Domains);
    const resultY1Domain = _mergeDomains(resultData.y1Domains);
    return {
        shapes: resultData.shapes,
        xDomain: resultXDomain[0] === undefined ? null : resultXDomain,
        y0Domain: resultY0Domain[0] === undefined ? null : resultY0Domain,
        y1Domain: resultY1Domain[0] === undefined ? null : resultY1Domain,
    };

};

const _checkStackDataConsistency = stackData => {
    const firstStackDatum = stackData[0];
    stackData.forEach(datum => {
        if (datum.curve !== firstStackDatum.curve) {
            throw new Error("Different 'curve' values in a single stack");
        }
        if (datum.side !== firstStackDatum.side) {
            throw new Error("Different 'side' values in a single stack");
        }
    });
};

const _computeStackPointsObjects = (stackData) =>
    stackData.map(stackShape =>
        stackShape.points.reduce((pointsObject, point) => {
            pointsObject[point.x] = point.y;
            return pointsObject;
        }, {}),
    );

const _computeAllXValues = (pointsObjects) =>
    pointsObjects
        .reduce((result, po) => result.concat(Object.keys(po)), [])
        .filter((v, i, a) => a.indexOf(v) === i) // .distinct()
        .map(v => +v)
        .sort((a, b) => a - b); // ascending

const key = (i) => `_${i}`;

const _generateKeys = (stackData) =>
    stackData.map((_, i) => key(i));

const _computeStackPoints = (allXValues, keys, pointsObjects) =>
    allXValues.map(x => {
        const result = {x: x};
        pointsObjects.forEach((pointsObject, i) => {
            result[keys[i]] = pointsObject[x];
        });
        return result;
    });

const _nullifyHoles = (stackPoints, keys) =>
    stackPoints.forEach(stackPoint => {
        if (keys.some(k => stackPoint[k] == null)) {
            stackPoint.isNull = true;
            keys.forEach(k => stackPoint[k] = null);
        }
    });

const _computeD3StackData = (keys, stackPoints) =>
    d3.stack().keys(keys)(stackPoints);

const _computeStackShapes = (stackData, stackPoints, d3StackData, allXValues) =>
    stackData.map((shape, shapeIndex) => ({
        color: shape.color,
        curve: shape.curve,
        legend: shape.legend,
        side: shape.side,
        shape: shape.shape,
        points: allXValues.map((x, xIndex) => {
            if (stackPoints[xIndex].isNull) {
                return {x: x, y: null};
            } else {
                const y = d3StackData[shapeIndex][xIndex];
                return {
                    x: x,
                    y0: y[0],
                    y: y[1],
                    yValue: stackPoints[xIndex][key(shapeIndex)],
                };
            }
        }),
    }));

const _computeYDomain = (stackPoints, keys) =>
    [0, d3.max(stackPoints, point => point.isNull ? null : keys.reduce((sum, key) => sum + point[key], 0))];

const _mergeDomains = ranges =>
    [d3.min(ranges, r => r[0]), d3.max(ranges, r => r[1])];

export {
    nullShapesData,
    parseShapesData,
};
