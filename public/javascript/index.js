$(function() {
    $('.search-box').keyup(function() {
        var search = $(this).val().toLocaleLowerCase();
        $('.page-item').each(function() {
            var $e = $(this);
            var show = $e.attr('data-keywords').toLocaleLowerCase().indexOf(search) != -1 ||
                $e.attr('data-name').toLocaleLowerCase().indexOf(search) != -1;

            var hidden = $e.attr('data-hidden');

            if (show && hidden) {
                $e.fadeIn();
                $e.attr('data-hidden', 0);
            } else {
                $e.fadeOut();
                $e.attr('data-hidden', 1);
            }
        });
    });
});