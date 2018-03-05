const CleanWebpackPlugin = require("clean-webpack-plugin");
const ExtractTextPlugin = require("extract-text-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");

const path = require("path");
const nodeModulesDir = path.resolve(__dirname, "node_modules");
const clientDir = path.resolve(__dirname, "src/client");
const outputDir = path.resolve(__dirname, "src/server/resources/static");

const configuration = prod => ({
    entry: path.resolve(clientDir, "index.js"),
    output: {
        path: outputDir,
        publicPath: "/assets/",
        filename: "index.[hash].js",
    },
    resolve: {
        modules: [nodeModulesDir, clientDir],
        extensions: [".js", ".jsx", ".sass", ".css"],
    },
    module: {
        loaders: [
            {
                test: /\.js$/,
                include: clientDir,
                loader: "babel-loader",
            },
            {
                test: /\.sass$/,
                include: clientDir,
                loader: ExtractTextPlugin.extract(`css-loader?minimize=${prod}!postcss-loader!sass-loader`),
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract(`css-loader?minimize=${prod}`),
            },
        ],
    },
    plugins: [
        new CleanWebpackPlugin([
            "index.html",
            "index.*.css",
            "index.*.js",
        ], {
            root: outputDir,
            verbose: false,
        }),
        new ExtractTextPlugin("index.[hash].css", {
            allChunks: true,
        }),
        new HtmlWebpackPlugin({
            template: path.resolve(clientDir, "index.html"),
        }),
    ],
    stats: {
        children: false,
    },
});

module.exports = configuration;
