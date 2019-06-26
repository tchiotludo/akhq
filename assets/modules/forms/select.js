import $ from "jquery";
import "../widget";
import "bootstrap-select";

$.widget("khq.select", $.khq.widget, {
    _create: function () {
        this.element.selectpicker({
            liveSearchNormalize: true,
            iconBase: 'fa',
            tickIcon: 'fa-check'
        });
    }
});
