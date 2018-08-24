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
                Turbolinks.visit($(td).find('a').attr('href'));
            })
    });

    /* Confirm */
    var toast = swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000
    });

    $('[data-confirm]').on("click", function (event) {
        event.stopPropagation();
        event.preventDefault();

        var message = 'Are you sure ?';
        var href = $(this).attr("href");

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
                                    Turbolinks.visit(window.location.href);
                                }
                            }
                        });
                    })
                ;
            }
        });

        return false;
    });
});