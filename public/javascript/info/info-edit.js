// JQuery goodies
$(function ($) {
    var timer;
    var $text = $('#text');
    var $preview = $('#preview');

    updatePreview();

    $('body').on('keydown', '#text', function () {
        if (timer) {
            clearTimeout(timer);
        }

        timer = setTimeout(updatePreview, 500);
    });

    // Ugly resize handling
    var textareaHeight = $text.height();
    $text.mouseup(function () {
        var h = $text.height();
        if (h == textareaHeight) return;

        textareaHeight = h;
        if (timer) {
            clearTimeout(timer);
        }

        timer = setTimeout(updatePreview, 500);
    });


    $preview.magnificPopup({
        type: 'image',
        delegate: 'a',
        gallery: {enabled: true}
    });

    function updatePreview() {
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

        var value = marked($text.val(), {renderer: renderer});
        if (inDev) {
            value += "</dev>";
        }

        $preview.html(value);
        $preview.height($text.height())
    }

    window.addEventListener("paste", pasteHandler);
    function pasteHandler(e) {
        if (e.clipboardData) {
            var items = e.clipboardData.items;
            if (items) {
                for (var i = 0; i < items.length; i++) {
                    if (items[i].type.indexOf("image") !== -1) {
                        var blob = items[i].getAsFile();
                        var form = new FormData();
                        form.append("data", blob);

                        $.ajax({
                            url: '/info/upload-image?infoId=' + window.angularData.infoId,
                            type: 'POST',
                            data: form,
                            processData: false,
                            contentType: false
                        }).done(function (result) {
                            $text.insertAtCaret("![Описание картинки](" + result + ")");
                            console.log(result);
                        });
                    }
                }
            }
        }
    }
});

$.fn.extend({
    insertAtCaret: function (myValue) {
        this.each(function () {
            if (document.selection) {
                this.focus();
                sel = document.selection.createRange();
                sel.text = myValue;
                this.focus();
            }
            else if (this.selectionStart || this.selectionStart == '0') {
                var startPos = this.selectionStart;
                var endPos = this.selectionEnd;
                var scrollTop = this.scrollTop;
                this.value = this.value.substring(0, startPos) + myValue + this.value.substring(endPos, this.value.length);
                this.focus();
                this.selectionStart = startPos + myValue.length;
                this.selectionEnd = startPos + myValue.length;
                this.scrollTop = scrollTop;
            } else {
                this.value += myValue;
                this.focus();
            }
        });
    }
});

var InfoEdit = angular.module('InfoEdit', []);
InfoEdit.controller('InfoEditController', function () {
    this.showCommon = window.angularData.infoId == 0;
});


