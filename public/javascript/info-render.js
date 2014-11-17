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
            return '<a href="' + href + '" title="' + text + '" class="image"><img src="' + href + '" alt="' + text + '"' + size + '/></a>';
        } else {
            return '<img src="' + href + '" alt="' + text + '">';
        }
    };

    // Цельнотянуто с marked + правки
    renderer.link = function(href, title, text) {
        if (this.options.sanitize) {
            try {
                var prot = decodeURIComponent(unescape(href))
                    .replace(/[^\w:]/g, '')
                    .toLowerCase();
            } catch (e) {
                return '';
            }
            if (prot.indexOf('javascript:') === 0) {
                return '';
            }
        }
        var out = '<a href="' + href + '"';

        if (href.match(/^http(s)?:/) || title.indexOf('(blank)') != -1) {
            out += ' target="_blank"';
        }

        if (title) {
            out += ' title="' + title + '"';
        }
        out += '>' + text + '</a>';
        return out;
    };

    var openHeaders = {};

    function cleanupHeaders(level, html) {
        for (var openLevel in openHeaders) {
            if (openHeaders.hasOwnProperty(openLevel) && openHeaders[openLevel] && openLevel >= level) {
                html += '</div>';
                openHeaders[openLevel] = false;
            }
        }
        return html;
    }

    renderer.heading = function (text, level, raw) {
        var anchorName = transliterate(text).replace(/[^\w\d]/g, '-');
        var className = '';
        var html = cleanupHeaders(level, '');
        openHeaders[level] = true;

        if (text.indexOf('(dev)') != -1 || text.indexOf('(дев)') != -1) {
            className = 'dev-section';
            text += ' <span class="badge badge-important">dev</span>';
        }
        html += '<div class="' + className + '" id="' + anchorName + '">' +
        '<a name="' + anchorName + '"></a>' +
        '<h' + level + ' id="' + this.options.headerPrefix + '-' + anchorName + '" data-target="' + anchorName + '">'
        + text
        + '</h' + level + '>\n';
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
    value = cleanupHeaders(0, value);

    return value;
}

function transliterate(word) {
    var A = {};
    var result = '';

    A["Ё"] = "YO";
    A["Й"] = "I";
    A["Ц"] = "TS";
    A["У"] = "U";
    A["К"] = "K";
    A["Е"] = "E";
    A["Н"] = "N";
    A["Г"] = "G";
    A["Ш"] = "SH";
    A["Щ"] = "SCH";
    A["З"] = "Z";
    A["Х"] = "H";
    A["Ъ"] = "'";
    A["ё"] = "yo";
    A["й"] = "i";
    A["ц"] = "ts";
    A["у"] = "u";
    A["к"] = "k";
    A["е"] = "e";
    A["н"] = "n";
    A["г"] = "g";
    A["ш"] = "sh";
    A["щ"] = "sch";
    A["з"] = "z";
    A["х"] = "h";
    A["ъ"] = "'";
    A["Ф"] = "F";
    A["Ы"] = "I";
    A["В"] = "V";
    A["А"] = "A";
    A["П"] = "P";
    A["Р"] = "R";
    A["О"] = "O";
    A["Л"] = "L";
    A["Д"] = "D";
    A["Ж"] = "ZH";
    A["Э"] = "E";
    A["ф"] = "f";
    A["ы"] = "i";
    A["в"] = "v";
    A["а"] = "a";
    A["п"] = "p";
    A["р"] = "r";
    A["о"] = "o";
    A["л"] = "l";
    A["д"] = "d";
    A["ж"] = "zh";
    A["э"] = "e";
    A["Я"] = "YA";
    A["Ч"] = "CH";
    A["С"] = "S";
    A["М"] = "M";
    A["И"] = "I";
    A["Т"] = "T";
    A["Ь"] = "'";
    A["Б"] = "B";
    A["Ю"] = "YU";
    A["я"] = "ya";
    A["ч"] = "ch";
    A["с"] = "s";
    A["м"] = "m";
    A["и"] = "i";
    A["т"] = "t";
    A["ь"] = "'";
    A["б"] = "b";
    A["ю"] = "yu";

    for (var i = 0; i < word.length; i++) {
        var c = word.charAt(i);

        result += A[c] || c;
    }

    return result;
}