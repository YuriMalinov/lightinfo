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

    function updatePreview() {
        $preview.html(marked($text.val()));
        $preview.height($text.height())
    }

});

var InfoEdit = angular.module('InfoEdit', []);
InfoEdit.controller('InfoEditController', function () {
    this.showCommon = window.angularData.infoId == 0;
});



$(function () {


document.onkeydown = function(e) {return on_keyboard_action(e); }
document.onkeyup = function(e) {return on_keyboardup_action(e); }

var canvas = document.getElementById("cc");
var ctx = canvas.getContext("2d");
var ctrl_pressed = false;

function on_keyboard_action(event){
    k = event.keyCode;
    //ctrl
    if(k==17){
        if(ctrl_pressed == false)
            ctrl_pressed = true;
        if (!window.Clipboard)
            pasteCatcher.focus();
    }
}
function on_keyboardup_action(event){
    //ctrl
    if(k==17)
        ctrl_pressed = false;
}


//=== Clipboard ================================================================

//firefox
var pasteCatcher;
if (!window.Clipboard){
    pasteCatcher = document.createElement("div");
    pasteCatcher.setAttribute("id", "paste_ff");
    pasteCatcher.setAttribute("contenteditable", "");
    pasteCatcher.style.cssText = 'opacity:0;position:fixed;top:0px;left:0px;';
    pasteCatcher.style.marginLeft = "-20px";
    document.body.appendChild(pasteCatcher);
    pasteCatcher.focus();
    document.addEventListener("click", function(){
        //pasteCatcher.focus();
    });
    document.getElementById('paste_ff').addEventListener('DOMSubtreeModified',function(){
        if(pasteCatcher.children.length == 1){
            img = pasteCatcher.firstElementChild.src;

            var img2 = new Image();
            img2.onload = function(){
                ctx.drawImage(img2, 0, 0);
            }
            img2.src = img;
            //ctx.drawImage(img, 0, 0);


            //ctx.drawImage(img, 0, 0);
            pasteCatcher.innerHTML = '';
        }
    },false);
}
//chrome
window.addEventListener("paste", pasteHandler);
function pasteHandler(e){
    if(e.clipboardData) {
        var items = e.clipboardData.items;
        if (items){
            for (var i = 0; i < items.length; i++) {
                if (items[i].type.indexOf("image") !== -1) {
                    var blob = items[i].getAsFile();
                    var URLObj = window.URL || window.webkitURL;
                    var source = URLObj.createObjectURL(blob);
                    paste_createImage(source);
                }
            }
        }
        // If we can't handle clipboard data directly (Firefox),
        // we need to read what was pasted from the contenteditable element
        else{
        }
    }
    else{
        setTimeout(paste_check_Input, 1);
    }
}
function paste_check_Input(){
    var child = pasteCatcher.childNodes[0];
    pasteCatcher.innerHTML = "";
    if (child){
        if (cild.tagName === "IMG"){
            paste_createImage(child.src);
        }
    }
}
function paste_createImage(source){
    var pastedImage = new Image();
    pastedImage.onload = function() {
        ctx.drawImage(pastedImage, 0, 0);
    }
    pastedImage.src = source;
}

//=== /Clipboard ===============================================================
});