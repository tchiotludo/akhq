import $ from  "jquery";
import swal from 'sweetalert2/dist/sweetalert2.js';
import turbolinks from "turbolinks";
import "../widget";

$.widget("khq.data-confirm", $.khq.widget, {
    options: {
        initSelector: '[data-confirm]'
    },

    _create: function () {
        this.element.on("click", function (event) {
            event.stopPropagation();
            event.preventDefault();

            let  message = 'Are you sure ?';
            const href = $(this).attr("href");

            if ($(this).attr("data-confirm") !== "true") {
                message = $(this).attr("data-confirm");
            }

            swal({
                html: message,
                type: 'question',
                showCancelButton: true
            }).then(function (result) {
                if (result.value) {
                    $.ajax({
                        type: "GET",
                        url: href
                    })
                        .done(function (response, textStatus, jqXHR)
                        {
                            if (jqXHR.status === 200) {
                                turbolinks.visit(window.location.href);
                            }
                        })
                    ;
                }
            });

            return false;
        });
    }
});
