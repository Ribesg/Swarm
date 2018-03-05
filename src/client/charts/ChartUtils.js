import d3 from "./D3";

const roundWithMaxDecimals = (x, n) => {
    const multiplier = 10 ** n;
    return Math.round((x + 0.00001) * multiplier) / multiplier;
};

/**
 * Creates a new d3 scale of the provided type using the provided domain and range.
 *
 * @param {string}   type   - One of "linear" or "time"
 * @param {[number]} domain - The domain as an array of two numbers
 * @param {[number]} range  - The range as an array of two numbers
 *
 * @returns {function} The d3 scale function
 */
const newScale = (type, domain, range) => {
    if (!domain || domain[1] - domain[0] <= 0) {
        throw new Error(`Invalid domain: ${JSON.stringify(domain)}`);
    }
    if (!range) {
        throw new Error("Missing or null range");
    }
    switch (type) {
        case "linear":
            return d3
                .scaleLinear()
                .domain(domain)
                .range(range);
        case "time":
            return d3
                .scaleTime()
                .domain(domain.map(d => d instanceof Date ? d : new Date(d)))
                .range(range);
        default:
            throw new Error(`Unknown scale type '${type}'`);
    }
};

/**
 * Gets all distinct X values found in the provided array of points.
 *
 * @param {Object[]} points - An array of objects all containing a number value for the key 'x'.
 *
 * @returns {number[]} An array of all distinct X values found, sorted in ascending order
 */
const getXValues = points => points
    .map(p => +p.x)
    .filter((x, i, a) => a.indexOf(x) === i) // .distinct()
    .sort((a, b) => a - b); // ascending

export {
    getXValues,
    newScale,
    roundWithMaxDecimals,
};
