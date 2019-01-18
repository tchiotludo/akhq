import $ from "jquery";

$.widget("khq.tooltip", $.khq.widget, {
    options: {
        initSelector: '[data-toggle="tooltip"]'
    },

    _create: function () {
        this.element.tooltip();
    }
});
