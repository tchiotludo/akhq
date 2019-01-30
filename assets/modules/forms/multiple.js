import $ from "jquery";
import "../widget";

$.widget("khq.multiple", $.khq.widget, {

    _create: function () {
        let self = this;

        self.element.find('button').on("click", function (e) {
            e.preventDefault();

            let clone = $(this).closest('div').clone();
            clone.find('input').each(function(key, value) {
                $(value).val('');
            });

            self.element.append(clone);
        });
    }
});
