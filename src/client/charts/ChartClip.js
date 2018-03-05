import React     from "react";
import PropTypes from "./PropTypes";

const ChartClip = ({id, size}) => (
    <defs>
        <clipPath id={id}>
            <rect y={-1} width={size.width} height={size.height + 1}/>
        </clipPath>
    </defs>
);

ChartClip.propTypes = {
    id: PropTypes.string.isRequired,
    size: PropTypes.size,
};

export default ChartClip;
