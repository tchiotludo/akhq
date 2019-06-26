import $ from  "jquery";
import "../widget";

$.widget("khq.tail-sse", $.khq.widget, {
    _eventSource: null,
    _followScroll: true,
    _removing: false,
    _offsets: [],
    _table: null,
    options: {
        maxRows: 10
    },

    _create: function () {
        const self = this;
        self._table = this.element.find('tbody');

        const cancelScroll = function() {
            self._followScroll = $(window).scrollTop() + $(window).height() >= $(document).height()
        };

        $(window).on("scroll", cancelScroll);

        self.addDestroyCallback(function(e)
        {
            if (self._removing === false) {
                self._close();
                $(window).off("scroll", cancelScroll);
            }
        });

        if (this._url().search()) {
            self._listen();
        }

        this.element.find('.pause')
            .on("click", function (e) {
                e.preventDefault();
                self._close();
            });

        this.element.find('.resume')
            .on("click", function (e) {
                e.preventDefault();
                self._listen();
            });

        this.element.find('.empty')
            .on("click", function (e) {
                e.preventDefault();
                self._removing = true;
                self._table.children().remove();
                self._removing = false;
            })
    },

    _listening() {
        this.element.find('.pause').removeClass("d-none");
        this.element.find('.empty').removeClass("d-none");
        this.element.find('.resume').addClass("d-none");
    },

    _listen() {
        const self = this;

        let url = this._url()
            .clone()
            .filename(this._url().filename() + "/sse")
            .setSearch(this._url().search());

        if (self._offsets && self._offsets.length > 0) {
            $.each(self._offsets, function(key, value) {
                url = url.addSearch("after", value)
            });
        }

        self._listening();

        this._eventSource = new EventSource(url.toString());
        this._eventSource.addEventListener('close', $.proxy(self._close, this), false);
        this._eventSource.addEventListener('error', $.proxy(self._close, this), false);

        this._eventSource.addEventListener('tailBody', function(e) {
            let searchBody = JSON.parse(e.data);

            self._offsets = searchBody.offsets;

            if (searchBody.body) {
                const tr = $(searchBody.body);
                tr.addClass("fade-in");
                self._table.append(tr);

                if (self._table.children().length > self.options.maxRows * 2) {
                    self._removing = true;
                    self._table.children().slice(0, self._table.children().length - self.options.maxRows * 2).remove();
                    self._removing = false;
                }

                $(document).trigger('khq.refresh');

                if (self._followScroll) {
                    self._scroll();
                }
            }

        }, false);
    },

    _scroll(e) {
        $("html, body").animate({ scrollTop: $(document).height() }, 300);
    },

    _close(e) {
        this._eventSource.close();

        this.element.find('.pause').addClass("d-none");
        this.element.find('.resume').removeClass("d-none");
    },
});
