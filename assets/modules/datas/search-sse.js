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

            if (searchBody.body.trim()) {
                table.append(searchBody.body);
                $(document).trigger('khq.refresh');
            }

            if (searchBody.progress && searchBody.offsets) {
                self._updateProgress(searchBody.offsets, searchBody.progress)
            }
        }, false);

        this._eventSource.addEventListener("searchEnd", function(e) {
            self._close(e);
            let searchEnd = JSON.parse(e.data);

            if (searchEnd.after !== null) {
                self._after
                    .removeClass('disabled')
                    .find('.page-link')
                    .attr('href', self._url().setSearch("after", searchEnd.after));
            } else {
                self._updateProgressBar(100);
            }
        });
    },

    _close(e) {
        this._eventSource.close();
        this._cancelButton.addClass('disabled btn-outline-dark');
    },

    _updateProgress(offsets, progress) {
        let current = 0;
        let total = 0;

        $.each(offsets, function (partition, offset) {
            total += offset.end - offset.begin;
        });

        $.each(progress, function (partition, remaining) {
            current += remaining;
        });

        this._updateProgressBar(100-(current*100/total));
    },

    _updateProgressBar(number) {
        this._progressBar
            .css('width', number + "%")
            .text((Math.round(number  * 100) / 100) + "%");
    }
});
