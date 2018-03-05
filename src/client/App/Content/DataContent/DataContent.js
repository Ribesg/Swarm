import PropTypes      from "prop-types";
import React          from "react";
import CpuChart       from "./CpuChart/CpuChart";
import "./DataContent.sass";
import DiskIoChart    from "./DiskIoChart/DiskIoChart";
import DiskSpaceTable from "./DiskSpaceTable/DiskSpaceTable";
import NetChart       from "./NetChart/NetChart";
import RamChart       from "./RamChart/RamChart";

const DataContent = ({selectedHost, selectedMode}) => (
    <div id="content" className="data">
        <div className="top container">
            <CpuChart selectedHost={selectedHost} selectedMode={selectedMode}/>
            <RamChart selectedHost={selectedHost} selectedMode={selectedMode}/>
        </div>
        <div className="bottom container">
            <DiskIoChart selectedHost={selectedHost} selectedMode={selectedMode}/>
            <div className="bottom-right container">
                <DiskSpaceTable selectedHost={selectedHost}/>
                <NetChart selectedHost={selectedHost} selectedMode={selectedMode}/>
            </div>
        </div>
    </div>
);

DataContent.propTypes = {
    selectedHost: PropTypes.string.isRequired,
    selectedMode: PropTypes.string.isRequired,
};

export default DataContent;
