<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<!doctype html>
<html>
<head>
  <title>Api | AKHQ</title>
  <meta charset='utf-8'/>
  <link rel="shortcut icon" type="image/png" href="/static/img/icon_black.png" />
  <meta name='viewport' content='width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes'/>
  <link href="https://fonts.googleapis.com/css?family=Open+Sans:300,600&display=swap" rel="stylesheet">
  <script src='https://unpkg.com/rapidoc/dist/rapidoc-min.js'></script>
</head>
<body>
  <rapi-doc id='rapidoc'
            layout="row"
            sort-tags="true"
            sort-endpoints-by="method"
            show-header="false"
            theme="dark"
            header-color="#005f81"
            primary-color="#33b5e5"
            render-style="read"
            schema-style="table"
            regular-font='Open Sans'
  >
    <img src="${basePath}/static/img/logo.svg" slot="nav-logo" alt="logo" />

  </rapi-doc>
  <script>
      const rapidoc = document.getElementById('rapidoc');
      rapidoc.setAttribute('spec-url', '${basePath}/swagger/akhq.yml');
  </script>
</body>
</html>
