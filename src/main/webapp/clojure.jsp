<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>

<body>
<script src="jquery-2.1.3.min.js"></script>

<script>
$( document ).ready(function(){
  $("#call").on("click", function() {
                         console.log( "<p> was clicked" );
    var content = $("#formatStrContent").val();
    var action = $("#action").val();

    $.ajax({url: "clojure?action=" + action + "&content=" + encodeURIComponent(content), success: function(result){
        $("#formatStrResult").html(result);
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
