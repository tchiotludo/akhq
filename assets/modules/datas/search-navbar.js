import $ from  "jquery";
import "../widget";
import turbolinks from "turbolinks";

$.widget("khq.search-navbar", $.khq.widget, {
    _value: null,

    _create: function () {
        let self = this;
        self._value = this.element.find('input').val();

        this.element.find('.btn').on('click', function() {
            self._offsetByDatetime($(this).closest('.dropdown-menu').find('input'));
        });

        this.element.find('input').on('keypress', function(e) {
            if (e.which === 13) {
                self._offsetByDatetime($(this).closest('.dropdown-menu').find('input'));
            }
        });
    },

    _offsetByDatetime: function(input) {
        let url = this._url();

        if (input.val()) {
            url.setSearch(input.attr('name'), input.val());
        } else {
            url.removeSearch(input.attr('name'));
        }

        turbolinks.visit(url);
    }
});
