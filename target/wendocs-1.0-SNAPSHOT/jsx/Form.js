
function isNonempty(str) {
    return typeof str != undefined && str != undefined && str != null && str.trim().length > 0;
};

var asyncCallIds = {
};

var LabelField = React.createClass({
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
        <h3>
        <span className={classNames}>
        {this.props.dataValue}
        </span></h3>
    );
    }
});

var InputField = React.createClass({
    handleChange: function(event) {
        if ("onUserInput" in this.props)
            this.props.onUserInput(event.target.value);
    },
  render: function() {
    return (
      <div className="form-group">
        { isNonempty(this.props.label) ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }
        <div className="input-group">
        { isNonempty(this.props.preaddon) ?
        <span className="input-group-addon">{this.props.preaddon}</span>
        : null }
        <input
            type={this.props.type}
            className="form-control"
            onChange={this.handleChange}
            value={this.props.dataValue} id={this.props.id} placeholder={this.props.placeHolder}
            >
        </input>
        { isNonempty(this.props.postaddon) ?
        <span className="input-group-addon">{this.props.postaddon}</span>
        : null }
        </div>
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
    return (
      <div className="form-group">
        { isNonempty(this.props.label) ?
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
      return (
      <div className="form-group">
        { isNonempty(this.props.label) ?
        <label htmlFor={this.props.id}>{this.props.label}</label>
        : null }
<div className="select2-wrapper">
      <select id={this.props.id} value={this.props.dataValue}
         onChange={this.handleChange}  className="form-control select2"
      >
      {this.props.options.map(function(opt, index, arr) {
        return (
        <option key={index} value={opt.value}>{opt.hasOwnProperty('label') ? opt.label : opt.value}</option>
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
    return (
      <div className="input-group">
        { isNonempty(this.props.label) ?
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
        { isNonempty(opt.preaddon) ?
        <span className="input-group-addon">{opt.preaddon}</span>
        : null }

        <input
            type='checkbox'
            onChange={thisHandleChange}
            defaultChecked={thisChecked}
            >
        </input>

        { isNonempty(opt.postaddon) ?
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

var Form = React.createClass({
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
        deltaState.retval = 'Calculating...';
        component.setState(deltaState);

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

        $.ajax(
            {
                url: component.props.url + "?userData=" + encodeURIComponent(userDataStr),
                error: function(xhr, textStatus, errorThrown) {
                    if (asyncCallIds[thisId] > newCallId)
                      return;
        var retval = 'Error';
                    userData['retval'] = retval;
                    component.setState({
                        retval: retval
                    });
                },
                success:
                    function(result){
                        if (asyncCallIds[thisId] > newCallId)
                          return;
                        var retval;
                        if (result == null || result.trim() == '')
                            retval = '.';
                        else
                            retval = result;
                        component.setState({
                            retval: retval
                        });
                   }
            });


    };

    var getHandleChangeFunc = function(id) {
        var f = function(val) {
            return processItem(val, id);
        };
        return f;
    };

  return (
    <form>
    {this.props.spec.map(
        function(itemSpec, index, arr) {
            var type = itemSpec.type;
            var onUserInput = getHandleChangeFunc(itemSpec.id);
            if (type == 'select') {
                return (
                <SelectField
                    id={itemSpec.id}
                    key={index}
                    options={itemSpec.options}
                    dataValue={getFallbackVal(component.state, defaultVals, itemSpec.id)}
                    label={itemSpec.label}
                    onUserInput = {onUserInput}
                >
                </SelectField>
                );
            }
            else if (type == 'checkboxes') {
                return (
                <CheckboxesField
                    id={itemSpec.id}
                    key={index}
                    options={itemSpec.options}
                    dataValue={getFallbackVal(component.state, defaultVals, itemSpec.id)}
                    label={itemSpec.label}
                    onUserInput = {onUserInput}
                >
                </CheckboxesField>
                );
            }
            else if (type == 'label') {
                return (
                <LabelField
                    id={itemSpec.id}
                    key={index}
                    type={itemSpec.subtype}
                    dataValue={getFallbackVal(component.state, defaultVals, itemSpec.id)}
                >
                </LabelField>
                );
            }
            else if (type == 'date') {
                return (
                <DateField
                    id={itemSpec.id}
                    key={index}
                    label={itemSpec.label}
                    placeHolder={itemSpec.placeHolder}
                    dataValue={getFallbackVal(component.state, defaultVals, itemSpec.id)}
                    onUserInput = {onUserInput}
                >
                </DateField>
                );
            }
            else {
                return (
                <InputField
                    id={itemSpec.id}
                    key={index}
                    label={itemSpec.label}
                    preaddon={itemSpec.preaddon}
                    postaddon={itemSpec.postaddon}
                    placeHolder={itemSpec.placeHolder}
                    dataValue={getFallbackVal(component.state, defaultVals, itemSpec.id)}
                    onUserInput = {onUserInput}
                >
                </InputField>
                );
            }
        }
    )}
    </form>
    );
  }
}
);
