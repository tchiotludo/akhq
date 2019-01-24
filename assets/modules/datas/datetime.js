import $ from  "jquery";
import turbolinks from "turbolinks";
import 'moment';
import 'tempusdominus-bootstrap-4';
import "../widget";

$.widget("khq.data-datetime", $.khq.widget, {
    _create: function () {
        const self = this;
        const element = this.element;

        let datetimeTempusOptions = {
            inline: true,
            sideBySide: true,
            useCurrent: false,
        };

        if (element.find('input').val()) {
            datetimeTempusOptions.date = element.find('input').val();
        }

        element
            .find('.datetime-container')
            .on('change.datetimepicker', function (event) {
                if (event.date) {
                    element
                        .find('input')
                        .val(event.date.toISOString());
                }
            })
            .datetimepicker(datetimeTempusOptions);

        element
            .find('.input-group button')
            .on('click', function () {
                self._submit($(this).closest('.input-group').find('input'));
            });

        element
            .find('input')
            .on('keypress', function (e) {
                if (e.which === 13) {
                    self._submit($(this));
                }
            });
    },

    _submit: function(input) {
        let url = this._url();

        if (input.val()) {
            url.setSearch(input.attr('name'), input.val());
        } else {
            url.removeSearch(input.attr('name'));
        }

        turbolinks.visit(url);
    }
});
