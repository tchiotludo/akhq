import $ from  "jquery";
import swal from 'sweetalert2';
import turbolinks from "turbolinks";
import "../widget";

$.widget("khq.data-confirm", $.khq.widget, {
    options: {
        initSelector: '[data-confirm]'
    },

    _create: function () {
        const toast = swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 3000
        });

        this.element.on("click", function (event) {
            event.stopPropagation();
            event.preventDefault();

            let  message = 'Are you sure ?';
            const href = $(this).attr("currentUrl");

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
                        url: href,
                        dataType: "json"
                    })
                        .always(function (response)
                        {
                            response = response.result ? response : response.responseJSON;

                            toast({
                                type: response.result ? 'success' : 'error',
                                title: response.message,
                                timer: response.result === false ? 0 : 300,
                                onAfterClose: function () {
                                    if (response.result === true) {
                                        turbolinks.visit(window.location.href);
                                    }
                                }
                            });
                        })
                    ;
                }
            });

            return false;
        });
    }
});
