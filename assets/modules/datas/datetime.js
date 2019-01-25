import $ from  "jquery";
import turbolinks from "turbolinks";
import 'moment';
import 'tempusdominus-bootstrap-4';
import "../widget";
import "../forms/datetime";

$.widget("khq.data-datetime", $.khq.datetime, {
    _create: function () {
        this._super();

        const self = this;
        const element = this.element;

        element
            .find('.input-group button')
            .on('click', function () {
                self._offsetByDatetime($(this).closest('.input-group').find('input'));
            });

        element
            .find('input')
            .on('keypress', function (e) {
                if (e.which === 13) {
                    self._offsetByDatetime($(this));
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
