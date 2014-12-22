$(function() {
    var $info = $('#info');
    $info.magnificPopup({
        type: 'image',
        delegate: 'a.image',
        gallery: {enabled: true}
    });

    var nav = '';
    var levelStack = [];
    var currentLevel = 0;

    function closeLevels(level) {
        if (level > currentLevel) {
            var stacked = currentLevel == 0 ? ' nav-stacked' : '';
            levelStack.push(currentLevel);
            currentLevel = level;
            nav += '\n\t<ul class="nav' + stacked + '">';
        } else if (level == currentLevel) {
            nav += '</li>\n';
        } else {
            while (1) {
                var prevLevel = levelStack.pop();

                while (currentLevel-- > prevLevel) {
                    nav += '</li>\n';
                    nav += '\t</ul>\n';
                }

                if (currentLevel <= level) {
                    nav += '</li>\n';
                    levelStack.push(currentLevel);
                    currentLevel = level;
                    break;
                }
            }
        }
    }

    $info.find('*').each(function () {
        var result = this.tagName.match(/[hH](\d)/);
        if (!result) return;

        var level = parseInt(result[1], 10);
        closeLevels(level);
        nav += '<li><a href="#' + $(this).attr('data-target') + '">' + $(this).text() + '</a>';
    });
    closeLevels(0);

    $('#sidebar').html(nav);

    $('body').scrollspy({target: '#sidebar'});

    function stickyUpdate() {
        var window_top = $(window).scrollTop();
        var div_top = $('#sidebar-container').offset().top;
        if (window_top > div_top) {
            $('#sidebar').addClass('stick');
        } else {
            $('#sidebar').removeClass('stick');
        }
    }

    $(function () {
        $(window).scroll(stickyUpdate);
        stickyUpdate();
    });

    hljs.initHighlightingOnLoad();
});