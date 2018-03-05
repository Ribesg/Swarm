import PropTypes    from "prop-types";
import React        from "react";
import "./Content.sass";
import DataContent  from "./DataContent/DataContent";
import EmptyContent from "./EmptyContent/EmptyContent";

const Content = ({selectedHost, selectedMode}) => {
    if (selectedHost == null) {
        return <EmptyContent/>;
    } else {
        return <DataContent selectedHost={selectedHost} selectedMode={selectedMode}/>;
    }
};

Content.propTypes = {
    selectedHost: PropTypes.string,
    selectedMode: PropTypes.string.isRequired,
};

Content.defaultProps = {
    selectedHost: null,
};

export default Content;
