import $ from "jquery";
import 'moment';
import 'tempusdominus-bootstrap-4';
import "../widget";

$.widget("khq.datetime", $.khq.widget, {
    _create: function () {
        const self = this;
        const element = this.element;

        if (element.find('.datetime-container').length > 0) {
            this._inline(element);
        } else {
            this._hover(element);
        }

    },

    _onChange: function(event) {
        const element = this.element;

        if (event.date) {
            element
                .find('input')
                .val(event.date.toISOString());
        }
    },

    _inline: function (element) {
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
            .on('change.datetimepicker', $.proxy(this._onChange, this))
            .datetimepicker(datetimeTempusOptions);
    },

    _hover: function (element) {
        element
            .find('input')
            .on('change.datetimepicker', $.proxy(this._onChange, this))
            .datetimepicker({
                sideBySide: true,
                useCurrent: false,
            });
    },
});
