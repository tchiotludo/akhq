import $ from "jquery";
import "popper.js";

$.widget("khq.popover", $.khq.widget, {
    options: {
        initSelector: '[data-toggle="popover"]'
    },

    _create: function () {
        this.element.popover();
    }
});
