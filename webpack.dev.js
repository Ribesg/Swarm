const merge = require("webpack-merge");
const common = require("./webpack.common.js");

const configuration = merge(common(false), {});

module.exports = configuration;
