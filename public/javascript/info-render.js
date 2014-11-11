function renderInfoJs(info, renderDev) {
    var value = renderInfo(info, true);
    if (!renderDev) {
        var div = $("<div>").html(value);
        div.find('.dev-section').remove();
        value = div.html();
    }
    return value;
}

function renderInfo(info, directHighlight) {
    var renderer = new marked.Renderer();
    renderer.image = function (href, title, text) {
        if (title) {
            var size = title.split(/[xх]/); // russian + english
            if (size[1]) {
                size = 'width="' + size[0] + '" height="' + size[1] + '"';
            } else {
                size = 'width="' + size[0] + '"';
            }
            return '<a href="' + href + '" title="' + text + '"><img src="' + href + '" alt="' + text + '"' + size + '/></a>';
        } else {
            return '<img src="' + href + '" alt="' + text + '">';
        }
    };

    var inDev = false;
    renderer.heading = function (text, level, raw) {
        var html = '';
        if (inDev !== false && inDev >= level) {
            // Close it
            html += "</div>";
            inDev = false;
        }

        if (text.indexOf('(dev)') != -1 || text.indexOf('(дев)') != -1) {
            if (inDev === false) {
                html += '<div class="dev-section">';
                text += ' <span class="badge badge-important">dev</span>';
                inDev = level;
            }
        }
        html += '<h'
        + level
        + ' id="'
        + this.options.headerPrefix
        + raw.toLowerCase().replace(/[^\w]+/g, '-')
        + '">'
        + text
        + '</h'
        + level
        + '>\n';
        return html;
    };

    if (directHighlight) {
        marked.setOptions({
            highlight: function (code, lang) {
                try {
                    return hljs.highlight(lang, code, true).value;
                } catch (e) {
                    return code;
                }
            }
        });
    }

    var value = marked(info, {renderer: renderer});
    if (inDev) {
        value += "</div>";
    }


    return value;
}

