
function getProp(obj, prop, defVal)
{
    if (typeof obj[prop] == undefined || obj[prop] == undefined)
        return defVal;
    return obj[prop];
}

function isNonempty(obj, prop) {
    var val = getProp(obj, prop, null);
    return val != null && val.trim().length > 0;
};


var asyncCallIds = {
};

var mergeClassNames = function(classNames, props) {
    var passedClassNames;
    if ('className' in props)
        passedClassNames = props.className;
    else
        passedClassNames = '';

    if (classNames == null || classNames == '')
        return passedClassNames;
    else
        return classNames + ' ' + passedClassNames;
};

var LabelField = React.createClass({displayName: "LabelField",
  render: function() {
    var classNames;
    var typesStr = getProp(this.props, 'subtype', null);
    var types;
    if (typesStr != null && typesStr.trim().length > 0)
      types = typesStr.split(" ");
    else
      types = [];

    if (types.indexOf('default') >= 0)
          classNames = 'label label-default';
    else if (types.indexOf('primary') >= 0)
          classNames = 'label label-primary';
    else if (types.indexOf('success') >= 0)
          classNames = 'label label-success';
    else if (types.indexOf('info') >= 0)
          classNames = 'label label-info';
    else if (types.indexOf('warning') >= 0)
          classNames = 'label label-warning';
    else if (types.indexOf('danger') >= 0)
          classNames = 'label label-danger';
      else
          classNames = '';

    classNames = mergeClassNames(classNames, this.props);

    if (types.indexOf('h1') >= 0)
        return (
            React.createElement("h1", React.__spread({},  this.props), 
            React.createElement("span", {className: classNames}, 
            this.props.dataValue
            ))
        );
    else if (types.indexOf('h2') >= 0)
        return (
            React.createElement("h2", React.__spread({},  this.props), 
            React.createElement("span", {className: classNames}, 
            this.props.dataValue
            ))
        );
    else if (types.indexOf('h3') >= 0)
        return (
            React.createElement("h3", React.__spread({},  this.props), 
            React.createElement("span", {className: classNames}, 
            this.props.dataValue
            ))
        );
    else if (types.indexOf('pre') >= 0)
        return (
            React.createElement("div", React.__spread({},  this.props), 
            React.createElement("pre", {className: classNames}, 
            this.props.dataValue
            )
            )
        );
    else
        return (
            React.createElement("div", React.__spread({},  this.props), 
            React.createElement("span", {className: classNames}, 
            this.props.dataValue
            )
            )
        );
    }
});

var InputField = React.createClass({displayName: "InputField",
    handleChange: function(event) {
        if ("onUserInput" in this.props)
            this.props.onUserInput(event.target.value);
    },
  render: function() {
    var classNames = mergeClassNames(null, this.props);
    var divId = 'div_' + this.props.id;
    return (
      React.createElement("div", React.__spread({},  this.props, {id: divId, className: classNames}), 
         isNonempty(this.props,'label') ?
        React.createElement("label", {htmlFor: this.props.id}, this.props.label)
        : null, 
        React.createElement("div", {className: "input-group"}, 
         isNonempty(this.props, 'preaddon') ?
        React.createElement("span", {className: "input-group-addon"}, this.props.preaddon)
        : null, 
        React.createElement("input", {
            type: this.props.type, 
            className: "form-control", 
            onChange: this.handleChange, 
            value: this.props.dataValue, id: this.props.id, placeholder: this.props.placeHolder
            }
        ), 
         isNonempty(this.props, 'postaddon') ?
        React.createElement("span", {className: "input-group-addon"}, this.props.postaddon)
        : null
        )
      )
    );
  }
});

var TableField = React.createClass({displayName: "TableField",
  render: function() {

    var defaultTableHeaderRowRenderer = function(props) {
        var header = getProp(props, 'header', ['']);
        var headerRowClassName = getProp(props, 'headerRowClassName', '');
        var headerColumnClassName = getProp(props, 'headerColumnClassName', '');
        console.log("default header headerRowClassName: " + headerRowClassName + ",colClassName:" + headerColumnClassName + ",header:" + header.length);
        return (
          React.createElement("tr", {key: "0", className: headerRowClassName}, 
              header.map(
                  function(col, index, cols) {
                  return (
                  React.createElement("td", {key: index, className: headerColumnClassName}, 
                  col
                  )
                  );
                  }
              )
          )
        );
    };

    var defaultTableCellRenderer = function(rowIndex, colIndex, val, props) {
        var cellSpecFunc = getProp(props, 'cellSpecsFunc', null);
        var cellSpec;
        if (cellSpecFunc != null) {
            cellSpec = cellSpecFunc(rowIndex, colIndex);
        }
        if (cellSpec == null) {
            cellSpec = {
                type: 'label',
                subtype: '',
                defaultValue: ' '
            };
        }

        var onUserInput = function(val) {
            var getDeltaData = function(data, updateId, updateVal) {
                var retval = {};
                var dataVal = getProp(data, updateId, props.defaultValue);
                dataVal[rowIndex - 1][colIndex] = updateVal;
                retval[updateId] = dataVal;
                return retval;
            };
            return props.processDataUpdate(props.id, val, getDeltaData);
        };

        return renderBySpec(cellSpec, rowIndex * 100 + colIndex, val, onUserInput);
    };



    var dataValue = getProp(this.props, 'dataValue', null);
    var headerRenderer = getProp(this.props, 'headerRenderer', defaultTableHeaderRowRenderer);

    var cellRenderer = getProp(this.props, 'cellRenderer', defaultTableCellRenderer);

    var rowClassName = getProp(this.props, 'rowClassName', '');
    var colClassName = getProp(this.props, 'columnClassName', '');
    var header = getProp(this.props, 'header', ['']);

    var renderValue = dataValue.slice(0);//clone, may be expensive for large array!!
    renderValue.splice(0, 0, header);
    var component = this;

    var dataRowRenderer = function(row, rowIndex, rows, props)
    {
        var dataRowIndex = rowIndex - 1;
         var thisColRenderer = function(col, colIndex, cols) {
             var cell = cellRenderer(dataRowIndex, colIndex, col, props);
             return (
              React.createElement("td", {key: colIndex, className: colClassName}, 
              cell
              )
              );
         };

        return (
          React.createElement("tr", {key: rowIndex, className: rowClassName}, 
              row.map(thisColRenderer)
          )
        );
    };

    var rowRenderer = function(row, index, rows) {
        if (index == 0)
         return headerRenderer(component.props);
        else
          return dataRowRenderer(row, index, rows, component.props);
    };

    var divId = 'div_' + this.props.id;
    var classNames = mergeClassNames(null, this.props);
    var tableClassNames = getProp(this.props, 'tableClassName', '');
    return (
      React.createElement("div", React.__spread({},  this.props, {id: divId, className: classNames}), 
         isNonempty(this.props, 'label') ?
        React.createElement("label", {htmlFor: this.props.id}, this.props.label)
        : null, 
        React.createElement("table", React.__spread({},  this.props, {className: tableClassNames}), 
        renderValue.map(rowRenderer)
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
    var classNames = mergeClassNames(null, this.props);
    var divId = "div_" + this.props.id;
    return (
      React.createElement("div", React.__spread({},  this.props, {id: divId, className: classNames}), 
         isNonempty(this.props, 'label') ?
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
    var classNames = mergeClassNames(null, this.props);
    var divId = 'div_' + this.props.id;
      return (
      React.createElement("div", React.__spread({},  this.props, {id: divId, className: classNames}), 
         isNonempty(this.props, 'label') ?
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
        React.createElement("option", {key: index, value: opt.value}, getProp(opt, 'label', opt.value))
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
    var classNames = mergeClassNames(null, this.props);
    var divId = 'div_' + this.props.id;
    return (
      React.createElement("div", React.__spread({},  this.props, {id: divId, className: classNames}), 
         isNonempty(this.props, 'label') ?
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
         isNonempty(opt, 'preaddon') ?
        React.createElement("span", {className: "input-group-addon"}, opt.preaddon)
        : null, 

        React.createElement("input", {
            type: "checkbox", 
            onChange: thisHandleChange, 
            defaultChecked: thisChecked
            }
        ), 

         isNonempty(opt, 'postaddon') ?
        React.createElement("span", {className: "input-group-addon"}, opt.postaddon)
        : null
                )
                );
            })
        

      )
    );
  }
});



function renderBySpec(itemSpec, childKey, dataValue, onUserInput, processDataUpdate)
{
    var type = itemSpec.type;
    if (type == 'select') {
        return (
        React.createElement(SelectField, React.__spread({}, 
           itemSpec, 
            {key: childKey, 
            dataValue: dataValue, 
            onUserInput: onUserInput
        })
        )
        );
    }
    else if (type == 'checkboxes') {
        return (
        React.createElement(CheckboxesField, React.__spread({}, 
           itemSpec, 
            {key: childKey, 
            dataValue: dataValue, 
            onUserInput: onUserInput
        })
        )
        );
    }
    else if (type == 'label') {
        return (
        React.createElement(LabelField, React.__spread({}, 
           itemSpec, 
            {key: childKey, 
            dataValue: dataValue
        })
        )
        );
    }
    else if (type == 'date') {
        return (
        React.createElement(DateField, React.__spread({}, 
           itemSpec, 
            {key: childKey, 
            dataValue: dataValue, 
            onUserInput: onUserInput
        })
        )
        );
    }
    else if (type == 'table') {
        return (
        React.createElement(TableField, React.__spread({}, 
           itemSpec, 
            {key: childKey, 
            dataValue: dataValue, 
            onUserInput: onUserInput, 
            processDataUpdate: processDataUpdate
        })
        )
        );
    }
    else if (type == 'input') {
        return (
        React.createElement(InputField, React.__spread({}, 
           itemSpec, 
            {key: childKey, 
            dataValue: dataValue, 
            onUserInput: onUserInput
        })
        )
        );
    }
    else {
        throw 'Invalid type: ' + type;
    }
}


var Form = React.createClass({displayName: "Form",
    getInitialState: function() {
        return {
        };
    },

    getDefaultValsFromSpec: function(theSpec)
    {
        var defaultVals = {};
        for (var index = 0; index < theSpec.length; index++) {
            var itemSpec = theSpec[index];
            var id = itemSpec.id;
            if (getProp(itemSpec, 'defaultValue', null) != null)
                defaultVals[id] = itemSpec.defaultValue;
            else
                defaultVals[id] = '';
        }
        return defaultVals;
    },


  render: function() {
    var theSpec = this.props.spec;
    var defaultVals = this.getDefaultValsFromSpec(theSpec);
    var thisId = this.props.id;

    var component = this;


    var getFallbackVal = function(vals, defaultVals, id)
    {
        if (getProp(vals, id, null) != null)
            return vals[id];
        return getProp(defaultVals, id, null);
    };

    var defaultGetDeltaData = function(data, updateId, updateVal) {
        var retval = {};
        retval[updateId] = updateVal;
        return retval;
    }

    var processDataUpdate = function(updateId, updateVal, getDeltaData) {
        var currState = component.state;
        var deltaState = getDeltaData(currState, updateId, updateVal);
        component.setState(deltaState);

        var url = getProp(component.props, 'url', null);
        if (url != null) {
            var userData = {};
            for (var index = 0; index < theSpec.length; index++) {
                var itemSpec = theSpec[index];
                var id = itemSpec.id;
                var val = getFallbackVal(component.state, defaultVals, id);
                console.log('param id:' + id + ', val:' + val);
                userData[id] = val;
            }
//
//            userData[updateId] = updateVal;

            var userDataStr = JSON.stringify(userData);

            var newCallId = getProp(asyncCallIds, thisId, 0) + 1;
            asyncCallIds[thisId] = newCallId;
            var willAjax = getProp(component.props, 'willAjax', null);
            if (willAjax != null) {
                willAjax(userData);
            }

            $.ajax(
                {
                    url: url + "?userData=" + encodeURIComponent(userDataStr),
                    dataType: component.props.dataType,
                    error: function(xhr, textStatus, errorThrown) {
                        if (asyncCallIds[thisId] > newCallId)
                          return;
                        var error = getProp(component.props, 'error', null);
                        if (error != null) {
                            error(xhr, textStatus, errorThrown);
                        }
                    },
                    success:
                        function(result, textStatus, xhr){
                            if (asyncCallIds[thisId] > newCallId)
                              return;
                            var success = getProp(component.props, 'success', null);
                            if (success != null) {
                                success(result, textStatus, xhr);
                            }
                       }
                });
        }
    };

    var getHandleChangeFunc = function(id) {
        var f = function(val) {
            return processDataUpdate(id, val, defaultGetDeltaData);
        };
        return f;
    };
  return (
    React.createElement("form", React.__spread({},  this.props), 
    this.props.spec.map(
        function(itemSpec, index, arr) {
            var onUserInput = getHandleChangeFunc(itemSpec.id);
            var dataValue = getFallbackVal(component.state, defaultVals, itemSpec.id)
            return renderBySpec(itemSpec, index, dataValue, onUserInput, processDataUpdate);
        }
     )
    )
    );
  }
}
);
