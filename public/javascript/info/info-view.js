$(function() {
    var $info = $('#info');
    $info.magnificPopup({
        type: 'image',
        delegate: 'a',
        gallery: {enabled: true}
    });
});