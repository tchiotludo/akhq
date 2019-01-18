import $ from "jquery";

$.widget("khq.sidebar-collapse", $.khq.widget, {
    options: {
        initSelector: '#khq-sidebar-collapse'
    },

    _create: function () {
        let element = this.element;

        element.on('click', function () {
            $('#khq-sidebar').toggleClass('active');
        })
    }
});
