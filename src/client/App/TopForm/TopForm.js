import PropTypes from "prop-types";
import React     from "react";
import Select    from "react-select";
import "./TopForm.sass";

const dataTransform = data => data ? data.map(d => ({value: d, label: d})) : [];

const TopForm = ({hosts, modes, onHostSelected, onModeSelected, selectedHost, selectedMode}) => (
    <div id="top-form">
        <h1>Swarm</h1>
        <div className="spacer"/>
        <div className="host select">
            <span>Host</span>
            <Select
                options={dataTransform(hosts)}
                value={selectedHost}
                onChange={d => onHostSelected(d.value)}
                clearable={false}
            />
        </div>
        <div className="mode select">
            <span>Mode</span>
            <Select
                options={dataTransform(modes)}
                value={selectedMode}
                onChange={d => onModeSelected(d.value)}
                clearable={false}
                searchable={false}
            />
        </div>
    </div>
);

TopForm.propTypes = {
    hosts: PropTypes.arrayOf(PropTypes.string),
    modes: PropTypes.arrayOf(PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"])).isRequired,
    onHostSelected: PropTypes.func.isRequired,
    onModeSelected: PropTypes.func.isRequired,
    selectedHost: PropTypes.string,
    selectedMode: PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"]),
};

TopForm.defaultProps = {
    hosts: null,
    selectedHost: null,
};

export default TopForm;
