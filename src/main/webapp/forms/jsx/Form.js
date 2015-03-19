
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

var LabelField = React.createClass({
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
            <h1 {...this.props}>
            <span className={classNames}>
            {this.props.dataValue}
            </span></h1>
        );
    else if (types.indexOf('h2') >= 0)
        return (
            <h2 {...this.props}>
            <span className={classNames}>
            {this.props.dataValue}
            </span></h2>
        );
    else if (types.indexOf('h3') >= 0)
        return (
            <h3 {...this.props}>
            <span className={classNames}>
            {this.props.dataValue}
            </span></h3>
        );
    else if (types.indexOf('pre') >= 0)
        return (
            <div {...this.props}>
            <pre className={classNames}>
            {this.props.dataValue}
            </pre>
            </div>
        );
    else
        return (
            <div {...this.props}>
            <span className={classNames}>
            {this.props.dataValue}
            </span>
            </div>
        );
    }
});

var InputField = React.createClass({
    handleChange: function(event) {
        if ("onUserInput" in this.props)
            this.props.onUserInput(event.target.value);
    },
  render: function() {
    var classNames = mergeClassNames(null, this.props);
    var divId = 'div_' + this.props.id;
    var inputStyle = getProp(this.props, 'inputStyle', {});
    return (
      <div {...this.props} id={divId} className={classNames}>
        { isNonempty(this.props,'label') ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }
        <div className="input-group">
        { isNonempty(this.props, 'preaddon') ?
        <span className="input-group-addon">{this.props.preaddon}</span>
        : null }
        <input
            type={this.props.type}
            className="form-control"
            style={inputStyle}
            onChange={this.handleChange}
            value={this.props.dataValue} id={this.props.id} placeholder={this.props.placeHolder}
            >
        </input>
        { isNonempty(this.props, 'postaddon') ?
        <span className="input-group-addon">{this.props.postaddon}</span>
        : null }
        </div>
      </div>
    );
  }
});

var TableField = React.createClass({

  getInitialState: function() {
    return {activeHeaderIndex: -1};
  },

  render: function() {
    var component = this;

    var defaultTableHeaderRowRenderer = function(props) {
        var header = getProp(props, 'header', ['']);
        var headerRowClassName = getProp(props, 'headerRowClassName', '');
        var headerColumnClassName = getProp(props, 'headerColumnClassName', '');
        var onMouseOver = function(index) {
            component.setState({activeHeaderIndex: index});
        };
        var onMouseOut = function(index) {
            component.setState({activeHeaderIndex: -1});
        };

        return (
          <tr key='0' className={headerRowClassName}>
              {header.map(
                  function(col, index, cols) {
        var spanClassNames = component.state.activeHeaderIndex == index ? 'display-inline' : 'display-none';
        var leftSpanClassNames = 'float-left ' + spanClassNames;
        var rightSpanClassNames = 'float-right ' + spanClassNames;

                  return (
                  <td key={index} onMouseOver={onMouseOver.bind(this, index)} onMouseOut={onMouseOut.bind(this, index)} className={headerColumnClassName}>
                  <span className={leftSpanClassNames}>{'\u25c0'}</span><span className='display-inline'>{col}</span><span className={rightSpanClassNames}>{'\u25b2 \u25bc \u25b6'}</span>
                  </td>
                  );
                  }
              )}
          </tr>
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
                dataVal[rowIndex][colIndex] = updateVal;
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

    var dataRowRenderer = function(row, rowIndex, rows, props)
    {
        var dataRowIndex = rowIndex - 1;
         var thisColRenderer = function(col, colIndex, cols) {
             var cell = cellRenderer(dataRowIndex, colIndex, col, props);
             return (
              <td key={colIndex} className={colClassName}>
              {cell}
              </td>
              );
         };

        var bgColorClassName = rowIndex % 2 == 0 ? 'table-alternate-row-light' : 'table-alternate-row-dark';
        var rowClassNameBg = rowClassName == '' ? bgColorClassName : (rowClassName + ' ' + bgColorClassName);

        return (
          <tr key={rowIndex} className={rowClassNameBg}>
              {row.map(thisColRenderer)}
          </tr>
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
      <div {...this.props} id={divId} className={classNames}>
        { isNonempty(this.props, 'label') ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }
        <table {...this.props} className={tableClassNames}>
        {renderValue.map(rowRenderer)}
        </table>
      </div>
        );
  }
});

var DateField = React.createClass({
    handleChange: function(date) {
        if ("onUserInput" in this.props) {
            this.props.onUserInput(date);
        }
    },
  render: function() {
    var classNames = mergeClassNames(null, this.props);
    var divId = "div_" + this.props.id;
    return (
      <div {...this.props} id={divId} className={classNames}>
        { isNonempty(this.props, 'label') ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }
        <div className="input-group">
            <DatePicker
              ref='theDatePicker'
              id={this.props.id}
              selected= {this.props.dataValue}
              onChange={this.handleChange}
              placeholderText={this.props.placeHolder}
              dateFormat="YYYYMMDD"
            >
            </DatePicker>
        </div>
      </div>
    );
  }
});


var SelectField = React.createClass({
    handleChange: function(event) {
        if ("onUserInput" in this.props)
            this.props.onUserInput(event.target.value);
    },
    render: function(){
    var classNames = mergeClassNames(null, this.props);
    var divId = 'div_' + this.props.id;
      return (
      <div {...this.props} id={divId}  className={classNames}>
        { isNonempty(this.props, 'label') ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }
<div className="select2-wrapper">
      <select
         id={this.props.id}
         value={this.props.dataValue}
         onChange={this.handleChange}
         className="form-control select2"
         size={this.props.visibleSize}
      >
      {this.props.options.map(function(opt, index, arr) {
        return (
        <option key={index} value={opt.value}>{getProp(opt, 'label', opt.value)}</option>
        );
      })
      }
      </select>
</div>
</div>
      );
    }
});

var CheckboxesField = React.createClass({
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
      <div {...this.props} id={divId} className={classNames}>
        { isNonempty(this.props, 'label') ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }

        {this.props.options.map(function(opt, index, arr) {

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
                <label className='input-group' key={index}>
        { isNonempty(opt, 'preaddon') ?
        <span className="input-group-addon">{opt.preaddon}</span>
        : null }

        <input
            type='checkbox'
            onChange={thisHandleChange}
            defaultChecked={thisChecked}
            >
        </input>

        { isNonempty(opt, 'postaddon') ?
        <span className="input-group-addon">{opt.postaddon}</span>
        : null }
                </label>
                );
            })
        }

      </div>
    );
  }
});



function renderBySpec(itemSpec, childKey, dataValue, onUserInput, processDataUpdate)
{
    var type = itemSpec.type;
    if (type == 'select') {
        return (
        <SelectField
           {...itemSpec}
            key={childKey}
            dataValue={dataValue}
            onUserInput = {onUserInput}
        >
        </SelectField>
        );
    }
    else if (type == 'checkboxes') {
        return (
        <CheckboxesField
           {...itemSpec}
            key={childKey}
            dataValue={dataValue}
            onUserInput = {onUserInput}
        >
        </CheckboxesField>
        );
    }
    else if (type == 'label') {
        return (
        <LabelField
           {...itemSpec}
            key={childKey}
            dataValue={dataValue}
        >
        </LabelField>
        );
    }
    else if (type == 'date') {
        return (
        <DateField
           {...itemSpec}
            key={childKey}
            dataValue={dataValue}
            onUserInput = {onUserInput}
        >
        </DateField>
        );
    }
    else if (type == 'table') {
        return (
        <TableField
           {...itemSpec}
            key={childKey}
            dataValue={dataValue}
            onUserInput = {onUserInput}
            processDataUpdate = {processDataUpdate}
        >
        </TableField>
        );
    }
    else if (type == 'input') {
        return (
        <InputField
           {...itemSpec}
            key={childKey}
            dataValue={dataValue}
            onUserInput = {onUserInput}
        >
        </InputField>
        );
    }
    else {
        throw 'Invalid type: ' + type;
    }
}


var Form = React.createClass({
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

            Object.assign(userData, deltaState); // why is this needed???

            var userDataStr = JSON.stringify(userData);

            var newCallId = getProp(asyncCallIds, thisId, 0) + 1;
            asyncCallIds[thisId] = newCallId;
            var willAjax = getProp(component.props, 'willAjax', null);
            if (willAjax != null) {
                willAjax(userData);
            }

            $.ajax(
                {
                    url: url,
                    data: {'userData': userDataStr},
                    type: 'POST',
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
    <form {...this.props}>
    {this.props.spec.map(
        function(itemSpec, index, arr) {
            var onUserInput = getHandleChangeFunc(itemSpec.id);
            var dataValue = getFallbackVal(component.state, defaultVals, itemSpec.id)
            return renderBySpec(itemSpec, index, dataValue, onUserInput, processDataUpdate);
        }
     )}
    </form>
    );
  }
}
);
