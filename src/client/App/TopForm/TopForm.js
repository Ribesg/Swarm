import PropTypes        from "prop-types";
import React            from "react";
import { LogOut }       from "react-feather";
import BaseSearchSelect from "../SearchSelect/BaseSearchSelect";
import "./TopForm.sass";

const TopForm = ({hosts, modes, onHostSelected, onModeSelected, onLogout, selectedHost, selectedMode}) => (
    <div id="top-form">
        <h1>Swarm</h1>
        <div className="spacer"/>
        <div className="host select">
            <label>Host</label>
            <BaseSearchSelect
                onValueSelected={onHostSelected}
                optionsProvider={(query) => ({
                    values: hosts.filter(h => h.toLowerCase().indexOf(query.toLowerCase()) != -1),
                })}
                placeholder={"Select Host..."}
                selectedValue={selectedHost}
                size={12.5}
            />
        </div>
        <div className="mode select">
            <label>Mode</label>
            <BaseSearchSelect
                onValueSelected={onModeSelected}
                optionsProvider={() => ({
                    values: modes,
                })}
                searchable={false}
                selectedValue={selectedMode}
                size={5}
            />
        </div>
        <div className="logout" onClick={onLogout}>
            <LogOut/>
        </div>
    </div>
);

TopForm.propTypes = {
    hosts: PropTypes.arrayOf(PropTypes.string),
    modes: PropTypes.arrayOf(PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"])).isRequired,
    onHostSelected: PropTypes.func.isRequired,
    onModeSelected: PropTypes.func.isRequired,
    onLogout: PropTypes.func.isRequired,
    selectedHost: PropTypes.string,
    selectedMode: PropTypes.oneOf(["LIVE", "HOUR", "DAY", "WEEK"]),
};

TopForm.defaultProps = {
    hosts: null,
    selectedHost: null,
};

export default TopForm;
