import $ from "jquery";
import swal from 'sweetalert2/dist/sweetalert2.js';

$.widget("khq.toast", $.khq.widget, {
    _create: function () {
        let toast = swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: 10000
        });

        let message = {};

        message.text = this.options.message;

        if (this.options.title) {
            message.title = this.options.title;
        }

        if (this.options.type) {
            message.type = this.options.type;
        }

        toast(message);
    }
});
