import $ from "jquery";
import "../widget";
import turbolinks from "turbolinks";

$.widget("khq.form-get", $.khq.widget, {

    _create: function () {
        let self = this;

        self.element.on("submit", function (e) {
            e.preventDefault();
            let url = self._url();

            $.each($(this).serializeArray(), function (key, value) {
                if (value.value) {
                    url.setSearch(value.name, value.value);
                } else {
                    url.removeSearch(value.name);
                }
            });

            turbolinks.visit(url.toString());
        })
    }
});
