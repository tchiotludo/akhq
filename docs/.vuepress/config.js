const { path } = require('@vuepress/utils')
const sidebar = require("./sidebar");

module.exports = {
  lang: 'en-US',
  title: 'AKHQ',
  description: 'Get all the insight of your Apache Kafka clusters, see topics, browse data inside topics, see consumer groups and their lag, manage your schema registry, see and manage your Kafka Connect cluster status, and more...',
  plugins: [
    '@vuepress/search',
    ['@vuepress/google-analytics', {'id': 'UA-56021-10'}],
    [
      '@vuepress/register-components',
      {
        componentsDir: path.resolve(__dirname, './components'),
      },
    ],
  ],
  themeConfig: {
    logo: '/assets/images/logo/akhqio_logo_yellow_white.svg',
    darkMode: false,
    repo: 'tchiotludo/akhq',
    repoLabel: 'GitHub',
    docsRepo: 'tchiotludo/akhq',
    docsDir: 'docs',
    docsBranch: 'dev',
    smoothScroll: true,
    navbar: [
      {text: 'Documentation', link: '/docs/'},
    ],
    sidebar: [
      '/docs/README.md',
      '/docs/installation.md',
      {
        text: 'Configurations',
        link: '/docs/configuration/README.md',
        children: [
          '/docs/configuration/brokers.md',
          {
            text: 'Schema Registry',
            children: [
              '/docs/configuration/schema-registry/tibco.md',
              '/docs/configuration/schema-registry/schema-references.md',
            ]
          },
          {
            text: 'Authentifications',
            link: '/docs/configuration/authentifications/README.md',
            children: [
              '/docs/configuration/authentifications/groups.md',
              '/docs/configuration/authentifications/jwt.md',
              '/docs/configuration/authentifications/basic-auth.md',
              '/docs/configuration/authentifications/oidc.md',
              '/docs/configuration/authentifications/ldap.md',
              '/docs/configuration/authentifications/header.md',
              '/docs/configuration/authentifications/external.md',

            ]
          },
          '/docs/configuration/docker.md',
          '/docs/configuration/akhq.md',
          '/docs/configuration/avro.md',
          '/docs/configuration/protobuf.md',
          '/docs/configuration/others.md',
        ]
      },
      '/docs/debug.md',
      '/docs/api.md',
      '/docs/dev.md',
    ]
  },
}