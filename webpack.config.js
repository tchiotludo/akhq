const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

/**********************************************************************************************************************\
 * Vars
 **********************************************************************************************************************/
const isDev = (process.env.NODE_ENV === 'dev');
const srcDirectory = path.join(__dirname, 'assets');
const dstDirectory = path.join(__dirname, 'public/static');

/**********************************************************************************************************************\
 * Base
 **********************************************************************************************************************/
module.exports = {
    devtool: 'source-map',
    entry: {
        vendor: [
            'jquery',
            'popper.js',
            'bootstrap',
            'sweetalert2',
            'turbolinks',
            path.join(srcDirectory, 'css/vendor.scss')
        ],
        main: [
            path.join(srcDirectory, 'js/main.js'),
            path.join(srcDirectory, 'css/main.scss'),
            path.join(srcDirectory, 'css/assets.scss'),
        ],
        assets: [
            path.join(srcDirectory, 'css/assets.scss'),
        ],

    },
    output: {
        path: dstDirectory,
        filename: 'js/[name].js',
        publicPath: '/static/',
    },
    resolve: {
        modules: [
            path.resolve('node_modules'),
            path.join(srcDirectory, 'js'),
            path.join(srcDirectory, 'css')
        ]
    },
    plugins: [
        new CleanWebpackPlugin([dstDirectory]),
        new webpack.DefinePlugin({
            isDev: isDev
        }),
        new ExtractTextPlugin({
            filename: 'css/[name].css'
        }),
    ],
    optimization: {
        splitChunks: {
            cacheGroups: {
                commons: {
                    test: /[\\/]node_modules[\\/]/,
                    name: 'vendor',
                    chunks: 'all'
                }
            }
        }
    },
    module: {
        rules: [
            // Sass
            {
                test: /\.scss$/,
                loader: ExtractTextPlugin.extract({
                    fallback: 'style-loader',
                    use: [
                        {
                            loader: 'css-loader',
                            options: {
                                minimize: !isDev,
                                sourceMap: true
                            }
                        },
                        {
                            loader: 'sass-loader',
                            options: {
                                outputStyle: 'expanded',
                                sourceMap: true,
                                includePaths: [path.join(srcDirectory, 'css')]
                            }
                        }
                    ]
                })
            },
            // Images
            {
                test: /\.(jpe?g|png|gif|svg)$/,
                exclude: [
                    /\/fonts\//
                ],
                loader: 'file-loader',
                options: {
                    name: 'img/[name].[ext]'
                }
            },
            // Fonts
            {
                test: /\.(eot|svg|ttf|woff|woff2)(\?v=\d+\.\d+\.\d+)?$/,
                include: [
                    /\/fonts\//
                ],
                loader: 'file-loader?name=font/[name].[ext]'
            }
        ]
    }
};

/**********************************************************************************************************************\
 * Dev
 **********************************************************************************************************************/
if (isDev) {
    module.exports = merge(module.exports, {
        output: {
            pathinfo: true,
        },
        devServer: {
            index: '',
            publicPath: '/static/',
            host: '0.0.0.0',
            proxy: {
                context: () => true,
                '/': 'http://localhost:8080'
            }
        }
    });
}
