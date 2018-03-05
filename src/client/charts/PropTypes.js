import PropTypes from "prop-types";

/**
 * Extends the provided predicate to create a new PropType entry.
 *
 * The following functions are added to the provided predicate:
 * - isRequired: ensures a non-null value is passed
 * - isNullableRequired: ensures a value is passed, allows null
 *
 * @param {function({}, string, string)} predicate - the predicate to extend
 *
 * @returns {function({}, string, string)} the PropTypes entry
 */
// noinspection EqualityComparisonWithCoercionJS
const PropType = (predicate) => {

    const propType = (props, propName, componentName) => {
        if (props[propName] == null) {
            return null;
        }
        return predicate(props, propName, componentName);
    };

    propType.isRequired = (props, propName, componentName) => {
        if (props[propName] == null) {
            return new Error(`Required prop '${propName}' was not specified or null in component '${componentName}'`);
        }
        return predicate(props, propName, componentName);
    };

    propType.isNullableRequired = (props, propName, componentName) => {
        if (props[propName] === null) {
            return null;
        }
        if (props[propName] === undefined) {
            return new Error(`Required prop '${propName}' was not specified in component '${componentName}'`);
        }
        return predicate(props, propName, componentName);
    };

    return propType;
};

/**
 * Standard error message builder.
 *
 * @param {string} propName      - The name of the property in error
 * @param {string} componentName - The name of the component on which a property is in error
 *
 * @returns the message
 */
const error = (propName, componentName) =>
    `Invalid prop '${propName}' supplied to component '${componentName}': `;

/**
 * Checks that the property is a number.
 */
PropTypes.number = PropType((props, propName, componentName) => {
    const value = props[propName];
    if (typeof value !== "number") {
        return new Error(error(propName, componentName) + "not a number");
    }
    return null;
});

/**
 * Checks that the property is an array of two numbers, the first one being smaller than or equal to the second one.
 */
PropTypes.chartDomain = PropType((props, propName, componentName) => {
    const value = props[propName];
    if (!Array.isArray(value)) {
        return new Error(error(propName, componentName) + "not an array");
    }
    if (value.length !== 2) {
        return new Error(error(propName, componentName) + "invalid length: " + value.length);
    }
    if (!value.every(e => typeof e === "number")) {
        return new Error(error(propName, componentName) + `not an array of numbers (${JSON.stringify(value)})`);
    }
    if (value[0] >= value[1]) {
        return new Error(error(propName, componentName) + `domain with negative or null length (${JSON.stringify(value)})`);
    }
    return null;
});

/**
 * Checks that the property is a number and matches the provided predicate.
 *
 * @param {function(number):boolean} predicate
 */
PropTypes.restrictedNumber = (predicate) => {
    if (typeof predicate !== "function") {
        throw new Error("Invalid or missing parameter 'predicate' for PropType 'restrictedNumber': " + predicate);
    }
    return PropType((props, propName, componentName) => {
        const numberCheckResult = PropTypes.number(props, propName, componentName);
        if (numberCheckResult != null) {
            return numberCheckResult;
        }
        if (!predicate(props[propName])) {
            return new Error(error(propName, componentName) + "predicate failed");
        }
        return null;
    });
};

PropTypes.arrayOfNumbers = (n) =>
    PropType((props, propName, componentName) => {
        const value = props[propName];
        if (!Array.isArray(value)) {
            return new Error(error(propName, componentName) + "not an array");
        }
        if (value.length !== n) {
            return new Error(error(propName, componentName) + "invalid length: " + value.length);
        }
        if (!value.every(e => typeof e === "number")) {
            return new Error(error(propName, componentName) + "not an array of numbers");
        }
        return null;
    });

/**
 * Checks that the property is a positive number.
 */
PropTypes.positiveNumber = PropTypes.restrictedNumber(n => n >= 0);

/**
 * Checks that the property is what should be set by react-sizeme.
 */
PropTypes.size = PropTypes.shape({
    width: PropTypes.positiveNumber.isRequired,
    height: PropTypes.positiveNumber.isRequired,
}).isRequired;

PropTypes.translate = PropTypes.shape({
    x: PropTypes.number,
    y: PropTypes.number,
});

export default PropTypes;
