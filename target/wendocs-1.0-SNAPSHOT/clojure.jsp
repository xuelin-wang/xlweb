<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
    <script src="jquery-2.1.3.min.js"></script>
    <script src="react/react.js"></script>
</head>

<body>
<script>
$( document ).ready(function(){
  $("#call").on("click", function() {
                         console.log( "<p> was clicked" );
    var content = $("#formatStrContent").val();
    var action = $("#action").val();

    var ResultElement = React.createClass({
        getInitialState: function() {
            return {result:""};
        },
      render: function() {
        return (
      React.createElement('span', null,
        "result: " + this.state.result
      )
        );
      }
      });

        var resultElement = React.createElement(ResultElement, null);

    $.ajax({url: "clojure?action=" + action + "&content=" + encodeURIComponent(content), success: function(result){
    resultElement.state.result = result;
        resultElement
        React.render(
            resultElement,
            document.getElementById('formatStrResult')
        );
    }});
                     });

});
</script>

    <div><textarea id="formatStrContent" rows="3" cols="60"></textarea></div>
    <select  id = "action">
    <option>formatStr</option>
    <option>formatStr2</option>
    </select>
    <div><button id="call">call</button></div>
    <div id="formatStrResult" />
</body>
</html>
