import $ from  "jquery";
import "../widget";
import turbolinks from "turbolinks";

$.widget("khq.offset-navbar", $.khq.widget, {
    _values: null,

    _create: function () {
        let self = this;
        self._value = this.element.find('input').val();

        this.element.find('.btn').on('click', function() {
            self._filterByStartingOffset($(this).closest('.input-group').find('input'));
        });

    },

    _filterByStartingOffset: function(inputs) {
        let url = this._url();

        var filteredInputs = inputs.filter(function(singleInput){
            return inputs[singleInput].value;
        });

        if(filteredInputs.length > 0){
            var after = "";
            filteredInputs.each(function (singleInput) {
                if(after){
                    after += "_";
                }
                var parsedOffset = parseInt(filteredInputs[singleInput].value);
                if( !isNaN(parsedOffset)  && parsedOffset > -1 ){
                    after += filteredInputs[singleInput].name + "-" + filteredInputs[singleInput].value;
                }
            });
            url.setSearch("after", after);
        }else{
            url.removeSearch('after');
        }

        turbolinks.visit(url);
    }

});