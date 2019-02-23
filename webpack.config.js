const path = require('path');
const webpack = require('webpack');
const merge = require('webpack-merge');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');

/**********************************************************************************************************************\
 * Vars
 **********************************************************************************************************************/
const srcDirectory = path.join(__dirname, 'assets');
const dstDirectory = path.join(__dirname, 'src/main/resources/static');

/**********************************************************************************************************************\
 * Base
 **********************************************************************************************************************/
module.exports = (env, argv) => {
    let config = {
        devtool: 'source-map',
        entry: {
            vendor: [
                'jquery',
                "jquery-ui/ui/widget",
                'popper.js',
                'bootstrap',
                'sweetalert2/dist/sweetalert2.js',
                'turbolinks',
                'moment',
                'imports-loader?jQuery=>jQuery!tempusdominus-bootstrap-4',
                'urijs',
                'highlight.js',
                'bytes',
                'humanize-duration',
                'ace-builds',
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
            publicPath: '../',
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
            new ExtractTextPlugin({
                filename: 'css/[name].css'
            }),
            new webpack.ProvidePlugin({
                "$":"jquery",
                "jQuery":"jquery",
                "window.jQuery":"jquery",
                "moment":"moment",
                "window.moment":"moment"
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
                // jQuery
                {
                    test: require.resolve('jquery'),
                    use: [
                        {
                            loader: 'expose-loader',
                            options: 'jQuery'
                        },
                        {
                            loader: 'expose-loader',
                            options: '$'
                        }
                    ]
                },
                // jQuery
                {
                    test: require.resolve('moment'),
                    use: [
                        {
                            loader: 'expose-loader',
                            options: 'moment'
                        }
                    ]
                },
                // Sass
                {
                    test: /\.scss$/,
                    loader: ExtractTextPlugin.extract({
                        fallback: 'style-loader',
                        use: [
                            {
                                loader: 'css-loader',
                                options: {
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
                    loader: 'file-loader?name=font/[name].[ext]',
                }
            ]
        }
    };

    /**********************************************************************************************************************\
     * Dev
     **********************************************************************************************************************/
    if (argv.mode === 'development') {
        config = merge(config, {
            devServer: {
                host: '0.0.0.0',
                disableHostCheck: true,
                port: 8081,
                overlay: true,
                publicPath: '/static/',
                proxy: {
                    context: () => true,
                    '/':  {
                        target: 'http://kafkahq:8080',
                    },
                    '/livereload': {
                        target: 'http://kafkahq:8080',
                        ws: true,
                    }
                }
            }
        });
    }

    return config;
};
