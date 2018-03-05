import PropTypes from "prop-types";
import React     from "react";

const wrapperStyle = {
    cursor: "default",
    padding: ".25em",
};

const tableStyle = {
    width: "100%",
    height: "100%",
    borderCollapse: "collapse",
};

const border = "1px solid #999";

const bodyTrStyle = {
    borderTop: border,
};

const baseCellStyle = {
    padding: ".5em",
};

const cellStyle = i =>
    Object.assign({}, baseCellStyle, i === 0 ? null : {borderLeft: border});

const Table = ({columns, data}) => {
    const headers = columns.map((c, i) =>
        <th key={c.legend} style={cellStyle(i)}>{c.legend}</th>,
    );
    const rows = data.map((d, i) => {
        const cells = columns.map((c, j) =>
            <td key={j} style={cellStyle(j)}>{d[c.selector]}</td>,
        );
        return <tr key={i} style={bodyTrStyle}>{cells}</tr>;
    });
    return (
        <div style={wrapperStyle}>
            <table style={tableStyle}>
                <thead>
                    <tr>{headers}</tr>
                </thead>
                <tbody>
                    {rows}
                </tbody>
            </table>
        </div>
    );
};

Table.propTypes = {
    columns: PropTypes.arrayOf(
        PropTypes.shape({
            legend: PropTypes.string.isRequired,
            selector: PropTypes.string.isRequired,
        }).isRequired,
    ).isRequired,
    data: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
};

export default Table;
