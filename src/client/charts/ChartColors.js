import TinyGradient from "tinygradient";
import d3           from "./D3";

const definitions = {
    COLD: {
        colors: ["#DCEDC8", "#42B3D5", "#1A237E"],
        positions: [0.25, 0.45, 0.75],
    },
    HOT: {
        colors: ["#FEEB65", "#E4521B", "#4D342F"],
        positions: [0.30, 0.45, 0.85],
    },
    NEON: {
        colors: ["#FFECB3", "#E85285", "#6A1B9A"],
        positions: [0.20, 0.45, 0.65],
    },
};

const gradients = Object.keys(definitions).reduce((gradients, name) => {
    const {colors, positions} = definitions[name];
    const points = [];
    points.push({color: "#FFFFFF", pos: 0});
    Object.keys(colors).forEach(i =>
        points.push({color: colors[i], pos: positions[i]}),
    );
    points.push({color: "#000000", pos: 1});
    gradients[name] = TinyGradient(points);
    return gradients;
}, {});

const getColors = (gradientName, amount) => {
    const gradient = gradients[gradientName];
    if (gradient) {
        if (amount > 5) {
            return gradient.rgb(amount + 3).slice(2, amount + 2).map(tc => tc.toHexString());
        } else {
            return gradient.rgb(amount + 2).slice(1, amount + 1).map(tc => tc.toHexString());
        }
    } else {
        throw new Error(`Unknown gradient: ${gradientName}`);
    }
};

const getColorScale = (gradientName, amount) =>
    d3.scaleOrdinal(getColors(gradientName, amount));

export {
    getColors,
    getColorScale,
};
