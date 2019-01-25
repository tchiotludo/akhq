import $ from "jquery";
import "../widget";

$.widget("khq.update-consumer-group-offsets", $.khq.widget, {
    _create: function () {
        let self = this;
        let datetime = this.element.find('.khq-datetime');

        datetime
            .find('.input-group button')
            .on('click', function () {
                self._offsetByDatetime($(this).closest('.input-group').find('input'));
            });

        datetime
            .find('input')
            .on('keypress', function (e) {
                if (e.which === 13) {
                    self._offsetByDatetime($(this));
                }
            });

        self.element
            .find('.khq-first-offsets')
            .on('click', function() {
                self.element
                    .find('input[name^="offset"]')
                    .each(function(key, value) {
                        $(value).val($(value).attr('min'));
                    });
            });

        self.element
            .find('.khq-last-offsets')
            .on('click', function() {
                self.element
                    .find('input[name^="offset"]')
                    .each(function(key, value) {
                        $(value).val($(value).attr('max'));
                    })
            });

        self.element
            .find('.khq-datetime-offsets')
            .on('click', function(e) {
                e.preventDefault();
                e.stopImmediatePropagation();

                let toggle = $(this).closest('.btn-group').find('.dropdown-toggle');
                let datetime = $(this).parent().find('.khq-datetime');

                datetime.toggleClass('d-none');
                toggle.dropdown('update');
            });
    },

    _offsetByDatetime: function(input) {
        let element = this.element;
        let href = this._url()
            .pathname(this._url().path() + "/start")
            .setSearch(input.attr('name'), input.val());

        $.ajax({
            type: "GET",
            url: href,
            dataType: "json"
        })
            .done(function (response)
            {
                $.each(response, function (key, offset) {
                    element
                        .find('input[name="offset\[' + offset.topic + '\]\[' + offset.partition + '\]"]')
                        .val(offset.offset);
                });
            })
        ;

    }
});
