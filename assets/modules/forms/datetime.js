import $ from "jquery";
import 'moment';
import 'tempusdominus-bootstrap-4';
import "../widget";

$.widget("khq.datetime", $.khq.widget, {
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

    },
});
