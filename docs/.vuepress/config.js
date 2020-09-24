const path = require("path");
const sidebar = require("./sidebar");

const descriptionAppend = "Kafka GUI for Apache Kafka.";
const description =  ($page) => {
    return $page.frontmatter.description !== undefined ? $page.frontmatter.description + " | " + descriptionAppend : $page.title + " | " + descriptionAppend;
}

module.exports = {
    title: 'AKHQ',
    plugins: {
        '@vuepress/back-to-top': {},
        '@vuepress/nprogress' : {},
        '@vuepress/last-updated': {},
        '@vuepress/google-analytics' : {'ga': ''},
        'vuepress-plugin-medium-zoom': {},
        'seo': {
            description:description,
            customMeta: (add, context) => {
                const {$page,} = context

                add('description', description($page))
            },
        }
    },
    themeConfig: {
        smoothScroll: true,
        nav: [
            {text: 'Documentation', link: '/docs/'},
        ],
        sidebar: {
            '/': [
                {
                    title: 'Documentation',
                    path: '/',
                    collapsable: false,
                    sidebarDepth: 1,
                    children: sidebar(`${__dirname}/`, '/docs/')
                }
            ]
        },
    }


};