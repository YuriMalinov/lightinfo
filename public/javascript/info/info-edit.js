// JQuery goodies
$(function ($) {
    var timer;
    var $text = $('#text');
    var $preview = $('#preview');
    //noinspection JSUnresolvedVariable
    var infoId = window.angularData.infoId;

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

    $text.keydown(function(e) {
        if (e.ctrlKey && !e.shiftKey && !e.altKey && e.keyCode == 83) {
            // Save
            $.ajax({
                url: infoId == 0 ? "/info/create-save-ajax" : ("/info/edit-save-ajax?id=" + infoId),
                type: "post",
                data: $('#edit-form').serializeArray()
            }).done(function (result) {
                if (result.result == "OK") {
                    infoId = result.id;
                    $('#success-message').fadeIn();
                    setTimeout(function() {
                        $('#success-message').fadeOut();
                    }, 1000);
                } else {
                    var errors = '';
                    for (var name in result.errors) {
                        errors += "<br/>";
                        //noinspection JSUnfilteredForInLoop
                        errors += name + ": " + result.errors[name].join(", ");
                    }
                    $('#error-messages').html(errors);
                    $('#error-message').fadeIn();
                    // No fadeout
                }
            });
            e.preventDefault();
        }
    });

    $('#error-message').find('.close').click(function() {
        $('#error-message').fadeOut();
    });

    $preview.magnificPopup({
        type: 'image',
        delegate: 'a',
        gallery: {enabled: true}
    });

    function updatePreview() {
        $preview.html(renderInfoJs($text.val(), true));
        $preview.height($text.height())
    }

    window.addEventListener("paste", pasteHandler);
    function pasteHandler(e) {
        if (e.clipboardData) {
            var items = e.clipboardData.items;
            if (items) {
                for (var i = 0; i < items.length; i++) {
                    if (items[i].type.indexOf("image") !== -1) {
                        if (infoId == 0) {
                            window.alert("Чтобы вставить изображение нужно хотя бы 1 раз сохранить запись.");
                        }
                        var blob = items[i].getAsFile();
                        var form = new FormData();
                        form.append("data", blob);

                        $.ajax({
                            // TODO: use Play Framework JavaScript router
                            url: '/info/upload-image?infoId=' + infoId,
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


