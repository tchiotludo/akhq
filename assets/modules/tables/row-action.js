import $ from  "jquery";
import "../widget";
import turbolinks from "turbolinks";

$.widget("khq.row-action-main", $.khq.widget, {
    _create: function () {
        let element = this.element;

        element.parent()
            .addClass("khq-main-row-action")
            .on('dblclick', function () {
                turbolinks.visit(element.find('a').attr('href'));
            })
    }
});
