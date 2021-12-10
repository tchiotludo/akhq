const sortBy = require("lodash.sortby");
const glob = require("glob");
const MarkdownIt = require("markdown-it");
const markdownMeta = require("markdown-it-meta");
const markdownTitle = require("markdown-it-title");
const fs = require("fs");
const path = require("path");
const {titleize} = require("inflection");

function isDirectory(source) {
    return fs.lstatSync(source).isDirectory();
}

function findDirectories(source) {
    return fs.readdirSync(source).filter(name => !(name === ".vuepress") &&
        isDirectory(path.join(source, name)));
}

function normalizeDirectory(dir) {
    dir = path.normalize(dir);
    return dir.endsWith(path.sep) ? dir.slice(0, -1) : dir;
}

function getName(filePath) {
    let name = filePath.split(path.sep).pop();
    const argsIndex = name.lastIndexOf("--");
    if (argsIndex > -1) {
        name = name.substring(0, argsIndex);
    }

    return titleize(name.replace("-", " "));
}

function getChildren(rootDir, rootUrl, subDir, recursive = false) {
    const pattern = recursive ? "/**/*.md" : "/*.md";
    let files = glob.sync(rootDir + (subDir ? `/${subDir}` : "") + pattern).map(currentPath => {
        // Instantiate MarkdownIt
        let md = new MarkdownIt();
        md.use(markdownMeta);
        md.use(markdownTitle);

        // Get the order value
        let file = fs.readFileSync(currentPath, "utf8");
        const extract = {};
        md.render(file, extract);

        // Remove "rootDir" and ".md"
        currentPath = currentPath.slice(rootDir.length, -3);

        let readme = 1;
        // Remove "README", making it the de facto index page
        if (currentPath.toLowerCase().endsWith("readme")) {
            currentPath = currentPath.slice(0, -6);
            readme = 0
        }

        return {
            link: rootUrl + currentPath,
            text: md.meta.title || extract.title,
            readme: readme,
            order: md.meta.order
        };
    });

    // Return the ordered list of files, sort by 'order' then 'path'
    return sortBy(files, ["readme", "order", "path"]);
}

function build(baseDir, rootUrl, maxLevel, relativeDir = "", currentLevel = 1) {
    const childs = getChildren(baseDir, rootUrl, relativeDir, currentLevel > maxLevel);

    if (currentLevel <= maxLevel) {
        findDirectories(path.join(baseDir, relativeDir))
            .forEach(subDir => {
                const children = build(
                    baseDir,
                    rootUrl,
                    maxLevel,
                    path.join(relativeDir, subDir),
                    currentLevel + 1
                );

                let readme = children.filter((current) => {
                        return current.readme === 1 && current.path &&
                            current.path === path.join(rootUrl, relativeDir, subDir) + "/";
                    }
                );

                if (readme.length === 1) {
                    children.splice(children.indexOf(readme[0]), 1);
                }

                if (children.length > 0 || readme.length === 1) {
                    const insertPosition = childs.length;
                    let node = readme.length === 1 ? readme[0] : {text: getName(subDir)};

                    if (children.length > 0) {
                        node.children = children;
                    }

                    childs.splice(insertPosition, 0, node);
                }
            });
    }

    return sortBy(childs, ["readme", "order", "path"]);
}

function sidebar(rootDir, rootUrl, maxLevel = 3) {
    rootDir = normalizeDirectory(rootDir);
    rootUrl = rootUrl.endsWith("/") ? rootUrl.slice(0, -1) : rootUrl;

  let build1 = build(rootDir, rootUrl, maxLevel);
  console.log(JSON.stringify(build1, null, 2))
  return build1;
}

module.exports = sidebar;
