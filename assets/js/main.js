import $ from  "jquery";
import 'bootstrap';
import turbolinks from "turbolinks";
import swal from 'sweetalert2';
import 'moment';
import 'tempusdominus-bootstrap-4';
import urijs from 'urijs';

turbolinks.start();
turbolinks.setProgressBarDelay(0);

$(document).on('ready turbolinks:load', function () {
    /* Sidebar */
    $('#sidebar-collapse').on('click', function () {
        $('#sidebar').toggleClass('active');
    });

    /* Tooltip */
    $('[data-toggle="tooltip"]').tooltip();

    /* Tabs */
    const hash = window.location.hash;
    if (hash) {
        $('ul.nav a[href="' + hash + '"]')
            .tab('show')
    }

    $('.tabs-container').removeClass('invisible');

    $('.nav-tabs a').click(function (e) {
        $(this).tab('show');
        window.location.hash = this.hash;
    });


    /* Main action */
    $('td.main-row-action').each(function (key, td) {
        $(td).parent()
            .addClass("main-row-action")
            .on('dblclick', function () {
                turbolinks.visit($(td).find('a').attr('href'));
            })
    });

    /* Confirm */
    const toast = swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000
    });

    $('[data-confirm]').on("click", function (event) {
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

    /* Datetime Tempus */
    const datetimeTempus = $('.datetime-tempus');
    let datetimeTempusOptions = {
        inline: true,
        sideBySide: true,
        useCurrent: false,
    };

    if (datetimeTempus.find('input').val()) {
        datetimeTempusOptions.date = datetimeTempus.find('input').val();
    }

    datetimeTempus
        .find('.datetime-container')
        .on('change.datetimepicker', function (event) {
            if (event.date) {
                datetimeTempus
                    .find('input')
                    .val(event.date.toISOString());
            }
        })
        .datetimepicker(datetimeTempusOptions);

    const dateTimeSubmit = function(input) {
        let href = urijs(document.location.href);

        if (input.val()) {
            href.setSearch(input.attr('name'), input.val());
        } else {
            href.removeSearch(input.attr('name'));
        }

        turbolinks.visit(href);
    };

    datetimeTempus
        .find('.input-group button')
        .on('click', function () {
            dateTimeSubmit($(this).closest('.input-group').find('input'));
        });

    datetimeTempus
        .find('input')
        .on('keypress', function (e) {
            if (e.which === 13) {
                dateTimeSubmit($(this))
            }
    });
});