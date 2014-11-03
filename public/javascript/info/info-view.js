$(function() {
    var $info = $('#info');
    $info.html(renderInfo($info.html(), $info.attr('data-view-internal') == 'true'))
});