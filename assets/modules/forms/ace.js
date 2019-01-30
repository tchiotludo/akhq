import $ from "jquery";
import "../widget";
import ace from 'ace-builds';
import "ace-builds/src-noconflict/theme-tomorrow";
import "ace-builds/webpack-resolver";

$.widget("khq.ace-editor", $.khq.widget, {

    _create: function () {
        let textarea = this.element.find('> textarea');

        let editor = ace.edit(this.element.find('> div')[0], {
            minLines: 5,
            maxLines: 48,
            autoScrollEditorIntoView: true,
            theme: "ace/theme/tomorrow"
        });

        editor.renderer.setScrollMargin(10, 10, 10, 10);

        editor.getSession().setValue(textarea.val());
        editor.getSession().on('change', function () {
            textarea.val(editor.getSession().getValue());
        });
    }
});
