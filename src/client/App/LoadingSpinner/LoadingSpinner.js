import PropTypes  from "prop-types";
import React      from "react";
import { Loader } from "react-feather";
import "./LoadingSpinner.sass";

const LoadingSpinner = (size) => (
    <Loader className={`${size} loading-spinner`}/>
);

LoadingSpinner.propTypes = {
    size: PropTypes.oneOf(["medium", "big"]),
};

LoadingSpinner.defaultProps = {
    size: "medium",
};

export default LoadingSpinner;
