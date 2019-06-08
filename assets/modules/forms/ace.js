import $ from "jquery";
import "../widget";
import ace from 'ace-builds';
import "ace-builds/src-noconflict/theme-merbivore_soft";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/ext-searchbox";
import "ace-builds/src-noconflict/ext-error_marker";
import "ace-builds/src-noconflict/ext-language_tools";

$.widget("khq.ace-editor", $.khq.widget, {

    _create: function () {
        let textarea = this.element.find('> textarea');

        let editor = ace.edit(this.element.find('> div')[0], {
            minLines: 5,
            maxLines: 48,
            autoScrollEditorIntoView: true,
            useWorker: false,
            theme: "ace/theme/merbivore_soft"
        });

        editor.renderer.setScrollMargin(10, 10, 10, 10);
        if (this.options.type) {
            editor.session.setMode("ace/mode/" + this.options.type);
        }

        let val = textarea.val();
        if (this.options.type === "json" && val) {
            val = JSON.stringify(JSON.parse(val), null, 2);
        }

        editor.getSession().setValue(val);
        editor.getSession().on('change', function () {
            textarea.val(editor.getSession().getValue());
        });
    }
});
