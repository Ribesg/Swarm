import {
    extent,
    max,
    merge,
    min,
    range,
    scan,
}                     from "d3-array";
import {
    axisBottom,
    axisLeft,
    axisRight,
    axisTop,
}                     from "d3-axis";
import {
    scaleLinear,
    scaleOrdinal,
    scaleTime,
}                     from "d3-scale";
import { select }     from "d3-selection";
import {
    arc,
    area,
    curveCatmullRom,
    curveLinear,
    curveMonotoneX,
    line,
    pie,
    stack,
    symbol,
}                     from "d3-shape";
import { timeFormat } from "d3-time-format";

const d3 = {
    arc,
    area,
    axisBottom,
    axisLeft,
    axisRight,
    axisTop,
    curveCatmullRom,
    curveLinear,
    curveMonotoneX,
    extent,
    line,
    max,
    merge,
    min,
    pie,
    range,
    scaleLinear,
    scaleOrdinal,
    scaleTime,
    scan,
    select,
    stack,
    symbol,
    timeFormat,
};

export default d3;
