import $ from "jquery";
import "jquery-ui/ui/widget";
import urijs from "urijs";

/**
 * Base widget
 */
$.widget("khq.widget", {
    _createWidget: function ()
    {
        $.Widget.prototype._createWidget.apply(this, arguments);
        this._trigger('init');
    },

    _getCreateOptions: function ()
    {
        var options = {};

        options = $.extend(true, {}, this.options);

        // all data-key camelCase
        $.each(this.element.data(), function (key, value)
        {
            if (typeof value !== 'function') {
                options[$.camelCase(key)] = value;
            }
        });

        // <script type="application/json"></script> just next to current element
        $.extend(true, options, this._scriptOptions.apply(this, [this.element]));

        // _initOptions: function() method on widget
        if (typeof this._initOptions === 'function') {
            $.extend(true, options, this._initOptions.apply(this, [options]));
        }

        return options;
    },

    _scriptOptions: function (elem)
    {
        var options = {};
        var scriptElement;

        scriptElement = elem.is('script') ? elem : elem.nextAll('script[type="application/json"]').first();
        if (scriptElement.length > 0) {
            try {
                options = JSON.parse(scriptElement.html());
            } catch (exception) {
                throw('Unable to parse widget options for element ' + exception);
            }
        }

        return options;
    },

    enhanceWithin: function (target)
    {
        var self = this;

        if (!self.options.initSelector) {
            self.options.initSelector = "." + self.namespace + "-" + self.widgetName;
        }

        if (self.options.initSelector instanceof $) {
            self.enhance(self.options.initSelector);
        } else {
            self.enhance($(self.options.initSelector, $(target)));
        }
    },

    enhance: function (targets)
    {
        var self = this;
        var elements = $(targets);
        elements[self.widgetName]();
    },

    _destroyCallback : [],

    /**
     * @return urijs
     * @private
     */
    _url: function()
    {
        let currentUrl = urijs(document.location.href);
        currentUrl = urijs('/' + currentUrl.relativeTo(currentUrl
            .clone()
            .path(null)
            .query(null)
            .fragment(null)
        ));

        return currentUrl;
    },

    addDestroyCallback : function (callback)
    {
        this._destroyCallback.push(callback);

        return this;
    },

    _destroy: function ()
    {
        var self = this;

        if (this._destroyCallback.length) {
            $.each(this._destroyCallback, function (key, value)
            {
                value.apply(self);
            })
        }

        this._super();
    },

    raise: function (msg)
    {
        throw "[" + this.widgetName + "] " + msg;
    }
});


/**
 * Initialize all khq event
 */
$(document).bind("ready turbolinks:load khq.refresh", function (event)
{
    $.each($.khq, function (key, current)
    {
        if (current.prototype.widgetName !== "widget") {
            $.khq[key].prototype.enhanceWithin(event.target);
        }
    });
});

/**
 * destroy all khq event
 */
$(document).bind("turbolinks:before-render", function (event)
{
    if (event.data && event.data.newBody) {
        $(document.body).empty()
    }
});
