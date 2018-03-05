const merge = require("webpack-merge");
const common = require("./webpack.common.js");
const DefinePlugin = require("webpack").DefinePlugin;
const UglifyJSPlugin = require("uglifyjs-webpack-plugin");

const configuration = merge(common(true), {
    plugins: [
        new DefinePlugin({
            "process.env.NODE_ENV": JSON.stringify("production")
        }),
        new UglifyJSPlugin({
            parallel: true
        })
    ]
});

module.exports = configuration;
