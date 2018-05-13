import PropTypes from "prop-types";
import React     from "react";
import autoBind  from "react-autobind";
import "./BaseSearchSelect.sass";

class BaseSearchSelect extends React.PureComponent {

    constructor(props) {
        super(props);
        autoBind(this);
        this.state = {
            focused: false,
            query: "",
        };
        this._inputRef = React.createRef();
    }

    componentDidUpdate() {
        if (this._inputRef.current) {
            this._inputRef.current.focus();
        }
    }

    render() {
        const {focused, query} = this.state;
        const {optionsProvider, placeholder, searchable, selectedValue, size, valueRenderer} = this.props;
        const content = [];
        if (focused) {
            content.push(
                <input
                    key="input"
                    ref={this._inputRef}
                    type="text"
                    value={query}
                    readOnly={!searchable}
                    onChange={this._onInputChange}
                    onBlur={this._onInputBlur}
                />,
            );
            const {topLine, values, bottomLine} = optionsProvider(query);
            content.push(this._renderDropdown(size, topLine, values, valueRenderer, bottomLine));
        } else {
            if (selectedValue) {
                content.push(valueRenderer("value", "value", selectedValue));
            } else {
                content.push(
                    <span key="placeholder" className="placeholder">{placeholder}</span>,
                );
            }
        }
        const classNames = ["search-select"];
        if (focused) {
            classNames.push("focused");
        }
        return (
            <div
                className={classNames.join(" ")}
                style={{width: `${size}rem`}}
                onClick={this._onSelectClicked}
            >
                {content}
            </div>
        );
    }

    _renderDropdown = (size, topLine, values, valueRenderer, bottomLine) => {
        const content = [];
        if (topLine) {
            content.push(
                <span key="top" className="top-line">{topLine}</span>,
            );
        }
        if (values && values.length > 0) {
            values.forEach(value => {
                const key = JSON.stringify(value);
                content.push(this._renderDropdownOption(valueRenderer, key, "value", value));
            });
        }
        if (bottomLine) {
            content.push(
                <span key="bottom" className="bottom-line">{bottomLine}</span>,
            );
        }
        return <div key="dropdown" className="dropdown" style={{width: `${size}rem`}}>{content}</div>;
    };

    _renderDropdownOption = (valueRenderer, key, className, value) => (
        React.cloneElement(valueRenderer(key, className, value), {
            onMouseDown: e => e.preventDefault(), // Prevent the click from blurring the input
            onClick: () => this._onOptionClick(value),
        })
    );

    _onInputChange = event => {
        this.setState({query: event.target.value});
    };

    _onInputBlur = () => {
        this.setState({focused: false});
    };

    _onOptionClick = value => {
        this.setState({focused: false, query: ""});
        this.props.onValueSelected(value);
    };

    _onSelectClicked = () => {
        if (!this.state.focused) {
            this.setState({focused: true});
        }
    };

}

BaseSearchSelect.propTypes = {

    /**
     * Called when a value is selected.
     * The argument passed is the value as returned by the optionsProvider.
     */
    onValueSelected: PropTypes.func.isRequired,

    /**
     * Provides options to the search dropdown.
     *
     * Takes one parameter:
     * - the search query, a string
     *
     * Returns an object with two results:
     * - 'values', a potentially empty but non-null array of values of whatever type
     * - 'topLine', a string to put at the top of the values list
     * - 'bottomLine', a string to put at the bottom of the values list
     */
    optionsProvider: PropTypes.func.isRequired,

    /**
     * The placeholder shown when the search select isn't focused and no value is selected.
     */
    placeholder: PropTypes.string,

    /**
     * If the select is searchable.
     */
    searchable: PropTypes.bool,

    /**
     * The currently selected value, may be null.
     * Should match the format of the "values" returned by the optionsProvider.
     */
    selectedValue: PropTypes.any,

    /**
     * Width of the select in rem.
     */
    size: PropTypes.number,

    /**
     * A function rendering a value as a single React Element.
     *
     * Takes 4 parameters:
     * - the key the returned Element should have
     * - the className the returned Element should have (or at least match)
     * - the value to render, as provided by the optionsProvider function
     *
     * Returns a React Element with the 'key' and 'className' props correctly set.
     */
    valueRenderer: PropTypes.func,

};

BaseSearchSelect.defaultProps = {
    placeholder: "",
    searchable: true,
    selectedValue: null,
    size: 10,
    valueLabel: (value) => value,
    valueRenderer: (key, className, value) => (
        <span key={key} className={"default " + className}>{value}</span>
    ),
};

export default BaseSearchSelect;
