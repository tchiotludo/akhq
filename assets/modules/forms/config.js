import $ from  "jquery";
import humanizeDuration from 'humanize-duration';
import bytes from 'bytes';
import "../widget";

$.widget("khq.form-config", $.khq.widget, {
    _create: function () {
        let self = this;

        let selectors = {
            "input[name$='.ms\]']": function(val, humanize) {
                humanize.html(humanizeDuration(val, { largest: 2 }));
            },
            "input[name$='.seconds\]']": function(val, humanize) {
                humanize.html(humanizeDuration(val * 1000, { largest: 2 }));
            },
            "input[name$='.bytes\]']": function(val, humanize) {
                humanize.html(bytes(parseInt(val)));
            },
            "input[name$='.size\]']": function(val, humanize) {
                humanize.html(bytes(parseInt(val)));
            }
        };

        $.each(selectors, function (selector, callback) {
            self.element.find(selector)
                .each(function (key, value) {
                    let input = $(value);
                    let humanize = input.closest('tr').find('.humanize');
                    if (humanize.length === 0) {
                        humanize = input.closest('.form-group').find('.form-text');
                    }

                    callback(input.val(), humanize);

                    input.on('keyup', function() {
                        callback(input.val(), humanize);
                    });
                });
        });
    }
});
