import $ from  "jquery";
import "../widget";
import hljs from "highlight.js";
import ace from 'ace-builds';
import "ace-builds/src-noconflict/theme-merbivore_soft";
import "ace-builds/src-noconflict/mode-json";
import "ace-builds/src-noconflict/mode-xml";
import "ace-builds/src-noconflict/mode-yaml";

$.widget("khq.data-highlight", $.khq.widget, {
    _editor: null,
    _create: function () {
        let self = this;

        this.element
            .find('code')
            .on('click', function() {
                let code = $(this);

                if (!code.closest('tr').hasClass("khq-data-highlight-row")) {
                    code.data('raw', code.html());

                    try {
                        let json = JSON.parse(code.html());
                        code.html(JSON.stringify(json, null, 2));
                    } catch (e) {}

                    let result = hljs.highlightAuto(code.text());

                    self._editor = ace.edit(code[0], {
                        autoScrollEditorIntoView: true,
                        useWorker: false,
                        theme: "ace/theme/merbivore_soft",
                        readOnly: true,
                        minLines: 1,
                        maxLines: 50
                    });

                    if (["json", "xml", "yaml"].indexOf(result.language) >= 0) {
                        self._editor.session.setMode("ace/mode/" + result.language);
                    }

                    code.closest('tr').addClass("khq-data-highlight-row");
                    code.closest('td').find('button.close').toggleClass('d-none');
                }
            })
            .closest('td')
            .find('button.close')
            .on('click', function() {
                $(this).toggleClass('d-none');

                self._editor.destroy();

                let code = $(this).parent().find('code');
                $(this).parent().find('code')
                    .removeClass()
                    .css('height', '')
                    .html(code.data('raw'))
                    .closest('tr')
                    .removeClass("khq-data-highlight-row")
            });
    }
});
