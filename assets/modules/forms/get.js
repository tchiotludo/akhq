import $ from "jquery";
import "../widget";
import turbolinks from "turbolinks";

$.widget("khq.form-get", $.khq.widget, {

    _create: function () {
        let self = this;

        self.element.on("submit", function (e) {
            e.preventDefault();
            let url = self._url().search("");

            $.each($(this).serializeArray(), function (key, value) {
                if (value.value) {
                    url.addSearch(value.name, value.value);
                }
            });

            turbolinks.visit(url.toString());
        })
    }
});
