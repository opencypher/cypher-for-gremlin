<html>
<head>
    <title>TCK Regression test</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<#macro scenarios scenarios>
    <#if scenarios?size != 0>
        <p>
    <table class="table table-striped">
        <th>Feature</th>
        <th>Scenario</th>
        <th>Status</th>
    </#if>
    <#list scenarios as scenario>
        <tr>
            <td>${scenario.featureName}</td>
            <td>${scenario.name}</td>
            <td <#if scenario.status == 'Passed'>class="success"
                <#else>class="danger"</#if>>${scenario.status}</td>
        </tr>
    </#list>
    <#if scenarios?size != 0>
    </table></p>
    </#if>
</#macro>
</head>
<body>
<div class="container">
<h1>TCK Regression Test</h1>
<#if diff.newlyFailedScenarios?size == 0 && diff.newlyPassingScenarios?size != 0>
<img title="TCK Progress" width="200" height="200" class="img-rounded"
     src="success.png"/>
    <p/>
<div class="alert alert-success">
    <strong>Success!</strong> TCK coverage improved.
</div>
<#elseif diff.newlyFailedScenarios?size != 0>
<img title="Degradation" width="200" height="200" class="img-rounded"
     src="failure.png"/>
    <p/>
<div class="alert alert-danger">
    <strong>Warning!</strong> TCK coverage degraded.
</div>
<#else >
<div class="alert alert-info">
    <strong>Info!</strong> Nothing changed. Please make sure to run 'gradle tck' on old version (before running 'gradle
    tckAndCmp' on updated
    version).
</div>
</#if>

<#if diff.newlyFailedScenarios?size != 0>
<h2>Newly failed scenarios: <span class="label label-danger">${diff.newlyFailedScenarios?size}</span></h2>
<@scenarios scenarios=diff.newlyFailedScenarios/>
</#if>

<#if diff.newlyPassingScenarios?size != 0>
<h2>Newly passing scenarios: <span class="label label-success">${diff.newlyPassingScenarios?size}</span></h2>
<@scenarios scenarios=diff.newlyPassingScenarios/>
</#if>

<br/>
<br/>
<br/>
<hr/>
<br/>
<br/>
<br/>

<h1>Currently</h1>

<p>Passing <span class="label label-info">${diff.totalPassingScenarios}</span> scenarios out of <span
    class="label label-info">${diff.totalScenarios}</span> <span
    class="label label-info">${diff.passingPercentage}</span></p>
<@scenarios scenarios=diff.allScenarios/>

<br/>
<br/>
</div>
</body>
</html>
