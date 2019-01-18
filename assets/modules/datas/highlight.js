import $ from  "jquery";
import "../widget";
import hljs from "highlight.js";

$.widget("khq.data-highlight", $.khq.widget, {
    _create: function () {
        this.element
            .find('code')
            .on('click', function() {
                let code = $(this);

                if (!code.closest('tr').hasClass("khq-data-highlight-row")) {
                    code.data('raw', code.html());

                    try {
                        let json = JSON.parse(code.html());
                        code.html(JSON.stringify(json, null, 2));
                    } catch (e) {}

                    hljs.highlightBlock(code[0]);

                    code.closest('tr').addClass("khq-data-highlight-row");
                    code.closest('td').find('button.close').toggleClass('d-none');
                }
            })
            .closest('td')
            .find('button.close')
            .on('click', function() {
                $(this).toggleClass('d-none');

                let code = $(this).parent().find('code');
                $(this).parent().find('code')
                    .removeClass()
                    .html(code.data('raw'))
                    .closest('tr')
                    .removeClass("khq-data-highlight-row")
            });
    }
});
