import $ from  "jquery";
import "../widget";

$.widget("khq.search-sse", $.khq.widget, {
    _progressBar: null,
    _cancelButton: null,
    _after: null,
    _eventSource: null,

    _create: function () {
        const self = this;

        let table = this.element.find('tbody');
        this._progressBar = this.element.find('.progress-bar');
        this._cancelButton = this.element.find('.progress-container button');
        this._after = this.element.parent().find('nav .page-item.after');
        this._after.addClass('disabled');

        this._eventSource = new EventSource(this._url()
            .clone()
            .filename(this._url().filename() + "/search/" + this._url().search(true).search)
            .removeSearch("search")
            .toString()
        );

        this._eventSource.addEventListener('open', function(e) {
            self._cancelButton.removeClass('disabled');
        }, false);

        this._eventSource.addEventListener('close', $.proxy(self._close, this), false);
        this._eventSource.addEventListener('error', $.proxy(self._close, this), false);
        this._cancelButton.on('click', $.proxy(self._close, this));
        self.addDestroyCallback(function()
        {
            self._close();
        });

        this._eventSource.addEventListener('searchBody', function(e) {
            let searchBody = JSON.parse(e.data);

            if (searchBody.body) {
                const tr = $(searchBody.body);
                tr.addClass("fade-in");
                table.append(tr);

                $(document).trigger('khq.refresh');
            }

            if (searchBody.after !== null) {
                self._updateAfter(searchBody.after);
            }

            if (searchBody.percent) {
                self._updateProgressBar(searchBody.percent);
            }
        }, false);

        this._eventSource.addEventListener("searchEnd", function(e) {
            self._close(e);
            let searchEnd = JSON.parse(e.data);

            self._updateAfter(searchEnd.after);
            self._updateProgressBar(100);
        });
    },

    _close(e) {
        this._eventSource.close();
        this._cancelButton.addClass('disabled btn-outline-dark');
    },

    _updateProgressBar(number) {
        this._progressBar
            .css('width', number + "%")
            .text((Math.round(number  * 100) / 100) + "%");
    },

    _updateAfter(after) {
        this._after
            .removeClass('disabled')
            .find('.page-link')
            .attr('href', this._url().setSearch("after", after));
    }
});
