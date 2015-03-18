
function isNonempty(str) {
    return typeof str != undefined && str != undefined && str != null && str.trim().length > 0;
};

var asyncCallIds = {
};

var LabelField = React.createClass({displayName: "LabelField",
  render: function() {
    var classNames;
      if (this.props.type == 'default')
          classNames = 'label label-default';
      else if (this.props.type == 'primary')
          classNames = 'label label-primary';
      else if (this.props.type == 'success')
          classNames = 'label label-success';
      else if (this.props.type == 'info')
          classNames = 'label label-info';
      else if (this.props.type == 'warning')
          classNames = 'label label-warning';
      else if (this.props.type == 'danger')
          classNames = 'label label-danger';
      else
          throw "Invalid label type: " + this.props.type;

    return (
        React.createElement("h3", null, 
        React.createElement("span", {className: classNames}, 
        this.props.dataValue
        ))
    );
    }
});

var InputField = React.createClass({displayName: "InputField",
    handleChange: function(event) {
        if ("onUserInput" in this.props)
            this.props.onUserInput(event.target.value);
    },
  render: function() {
    return (
      React.createElement("div", {className: "form-group"}, 
         isNonempty(this.props.label) ?
        React.createElement("label", {htmlFor: this.props.id}, this.props.label)
        : null, 
        React.createElement("div", {className: "input-group"}, 
         isNonempty(this.props.preaddon) ?
        React.createElement("span", {className: "input-group-addon"}, this.props.preaddon)
        : null, 
        React.createElement("input", {
            type: this.props.type, 
            className: "form-control", 
            onChange: this.handleChange, 
            value: this.props.dataValue, id: this.props.id, placeholder: this.props.placeHolder
            }
        ), 
         isNonempty(this.props.postaddon) ?
        React.createElement("span", {className: "input-group-addon"}, this.props.postaddon)
        : null
        )
      )
    );
  }
});

var DateField = React.createClass({displayName: "DateField",
    handleChange: function(date) {
        if ("onUserInput" in this.props) {
            this.props.onUserInput(date);
        }
    },
  render: function() {
    return (
      React.createElement("div", {className: "form-group"}, 
         isNonempty(this.props.label) ?
        React.createElement("label", {htmlFor: this.props.id}, this.props.label)
        : null, 
        React.createElement("div", {className: "input-group"}, 
            React.createElement(DatePicker, {
              ref: "theDatePicker", 
              id: this.props.id, 
              selected: this.props.dataValue, 
              onChange: this.handleChange, 
              placeholderText: this.props.placeHolder, 
              dateFormat: "YYYYMMDD"
            }
            )
        )
      )
    );
  }
});


var SelectField = React.createClass({displayName: "SelectField",
    handleChange: function(event) {
        if ("onUserInput" in this.props)
            this.props.onUserInput(event.target.value);
    },
    render: function(){
      return (
      React.createElement("div", {className: "form-group"}, 
         isNonempty(this.props.label) ?
        React.createElement("label", {htmlFor: this.props.id}, this.props.label)
        : null, 
React.createElement("div", {className: "select2-wrapper"}, 
      React.createElement("select", {
         id: this.props.id, 
         value: this.props.dataValue, 
         onChange: this.handleChange, 
         className: "form-control select2", 
         size: this.props.visibleSize
      }, 
      this.props.options.map(function(opt, index, arr) {
        return (
        React.createElement("option", {key: index, value: opt.value}, opt.hasOwnProperty('label') ? opt.label : opt.value)
        );
      })
      
      )
)
)
      );
    }
});

var CheckboxesField = React.createClass({displayName: "CheckboxesField",
    handleChange: function(val, checked) {
        if ("onUserInput" in this.props) {
            var dataValue = this.props.dataValue;
            var found = false;
            for (var index = 0; index < dataValue.length; index++) {
                var thisVal = dataValue[index];
                if (val == thisVal) {
                    found = true;
                    if (!checked) {
                        dataValue.splice(index, 1);
                    }
                    break;
                }
            }
            if (checked && !found)
                dataValue.push(val);
            this.props.onUserInput(dataValue);
        }
    },
  render: function() {
    var component = this;
    return (
      React.createElement("div", {className: "input-group"}, 
         isNonempty(this.props.label) ?
        React.createElement("label", {htmlFor: this.props.id}, this.props.label)
        : null, 

        this.props.options.map(function(opt, index, arr) {

    var thisHandleChange = function(event) {
        return component.handleChange(opt.value, event.target.checked);
    };
    var thisChecked = false;
    for (var tmpi = 0; tmpi < component.props.dataValue.length; tmpi++) {
        if (component.props.dataValue[tmpi] == opt.value) {
            thisChecked = true;
            break;
        }
    }

                return (
                React.createElement("label", {className: "input-group", key: index}, 
         isNonempty(opt.preaddon) ?
        React.createElement("span", {className: "input-group-addon"}, opt.preaddon)
        : null, 

        React.createElement("input", {
            type: "checkbox", 
            onChange: thisHandleChange, 
            defaultChecked: thisChecked
            }
        ), 

         isNonempty(opt.postaddon) ?
        React.createElement("span", {className: "input-group-addon"}, opt.postaddon)
        : null
                )
                );
            })
        

      )
    );
  }
});



function getDefaultValsFromSpec(theSpec)
{
    var defaultVals = {};
    for (var index = 0; index < theSpec.length; index++) {
        var itemSpec = theSpec[index];
        var id = itemSpec.id;
        if (itemSpec.hasOwnProperty('defaultValue'))
            defaultVals[id] = itemSpec.defaultValue;
        else
            defaultVals[id] = '';
    }
    return defaultVals;
}

function getFallbackVal(vals, defaultVals, id)
{
    if (vals.hasOwnProperty(id))
        return vals[id];
    else if (defaultVals.hasOwnProperty(id))
        return defaultVals[id];
    else
        return null;
}

var Form = React.createClass({displayName: "Form",
    getInitialState: function() {
        return {
            retval: '.'
        };
    },

  render: function() {
    var theSpec = this.props.spec;
    var defaultVals = getDefaultValsFromSpec(theSpec);
    var thisId = this.props.id;

    var component = this;
    var processItem = function(updateVal, updateId) {
        var deltaState = {};
        deltaState[updateId] = updateVal;
        component.setState(deltaState);

        if (component.props.hasOwnProperty('url') && component.props.url != undefined && component.props.url != null) {
            var userData = {};
            for (var index = 0; index < theSpec.length; index++) {
                var itemSpec = theSpec[index];
                var id = itemSpec.id;
                var val = getFallbackVal(component.state, defaultVals, id);
                console.log('param id:' + id + ', val:' + val);
                userData[id] = val;
            }
            userData[updateId] = updateVal;

            var userDataStr = JSON.stringify(userData);

            var newCallId;
            if (!asyncCallIds.hasOwnProperty(thisId))
                newCallId = 0;
            else
                newCallId = asyncCallIds[thisId] + 1;

            asyncCallIds[thisId] = newCallId;
            if (component.props.hasOwnProperty('beforeAjax') && component.props.beforeAjax != undefined && component.props.beforeAjax != null) {
                component.props.beforeAjax(userData);
            }

            $.ajax(
                {
                    url: component.props.url + "?userData=" + encodeURIComponent(userDataStr),
                    dataType: component.props.dataType,
                    error: function(xhr, textStatus, errorThrown) {
                        if (asyncCallIds[thisId] > newCallId)
                          return;
                        if (component.props.hasOwnProperty('error') && component.props.error != undefined && component.props.error != null) {
                            component.props.error(xhr, textStatus, errorThrown);
                        }
                    },
                    success:
                        function(result, textStatus, xhr){
                            if (asyncCallIds[thisId] > newCallId)
                              return;
                            if (component.props.hasOwnProperty('success') && component.props.success != undefined && component.props.success != null) {
                                component.props.success(result, textStatus, xhr);
                            }
                       }
                });
        }
    };

    var getHandleChangeFunc = function(id) {
        var f = function(val) {
            return processItem(val, id);
        };
        return f;
    };

  return (
    React.createElement("form", null, 
    this.props.spec.map(
        function(itemSpec, index, arr) {
            var type = itemSpec.type;
            var onUserInput = getHandleChangeFunc(itemSpec.id);
            if (type == 'select') {
                return (
                React.createElement(SelectField, {
                    id: itemSpec.id, 
                    key: index, 
                    options: itemSpec.options, 
                    dataValue: getFallbackVal(component.state, defaultVals, itemSpec.id), 
                    label: itemSpec.label, 
                    onUserInput: onUserInput
                }
                )
                );
            }
            else if (type == 'checkboxes') {
                return (
                React.createElement(CheckboxesField, {
                    id: itemSpec.id, 
                    key: index, 
                    options: itemSpec.options, 
                    dataValue: getFallbackVal(component.state, defaultVals, itemSpec.id), 
                    label: itemSpec.label, 
                    onUserInput: onUserInput
                }
                )
                );
            }
            else if (type == 'label') {
                return (
                React.createElement(LabelField, {
                    id: itemSpec.id, 
                    key: index, 
                    type: itemSpec.subtype, 
                    dataValue: getFallbackVal(component.state, defaultVals, itemSpec.id)
                }
                )
                );
            }
            else if (type == 'date') {
                return (
                React.createElement(DateField, {
                    id: itemSpec.id, 
                    key: index, 
                    label: itemSpec.label, 
                    placeHolder: itemSpec.placeHolder, 
                    dataValue: getFallbackVal(component.state, defaultVals, itemSpec.id), 
                    onUserInput: onUserInput
                }
                )
                );
            }
            else {
                return (
                React.createElement(InputField, {
                    id: itemSpec.id, 
                    key: index, 
                    label: itemSpec.label, 
                    preaddon: itemSpec.preaddon, 
                    postaddon: itemSpec.postaddon, 
                    placeHolder: itemSpec.placeHolder, 
                    dataValue: getFallbackVal(component.state, defaultVals, itemSpec.id), 
                    onUserInput: onUserInput
                }
                )
                );
            }
        }
    )
    )
    );
  }
}
);
