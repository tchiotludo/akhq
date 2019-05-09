<#ftl output_format="HTML">

<#-- @ftlvariable name="topic" type="org.kafkahq.models.Topic" -->

<#import "includes/template.ftl" as template>
<#import "includes/functions.ftl" as functions>

<@template.header "Produce to  " + topic.getName(), "topic" />

<form enctype="multipart/form-data" method="post" class="khq-form khq-form-config">
    <div class="form-group row">
        <label for="partition" class="col-sm-2 col-form-label">Partition</label>
        <div class="col-sm-10">
            <select class="form-control" name="partition" id="partition">
                <option></option>
                <#list topic.getPartitions() as partition>
                    <option>${partition.getId()}</option>
                </#list>
            </select>
        </div>
    </div>
    <div class="form-group row">
        <label for="key" class="col-sm-2 col-form-label">Key</label>
        <div class="col-sm-10">
            <input type="text" class="form-control" name="key" id="key" autocomplete="off" placeholder="Key">
        </div>
    </div>
    <div class="form-group row">
        <label class="col-sm-2 col-form-label">Headers</label>
        <div class="col-sm-10 khq-multiple">
            <div>
                <input type="text" class="form-control" name="headers[key]" autocomplete="off" placeholder="Key">
                <input type="text" class="form-control" name="headers[value]" autocomplete="off" placeholder="Value">
                <button class="btn btn-secondary"><i class="fa fa-plus"></i></button>
            </div>
        </div>
    </div>
    <div class="form-group row">
        <label for="timestamp" class="col-sm-2 col-form-label">Timestamp</label>
        <div class="col-sm-10 khq-datetime">
            <input type="text" class="form-control datetimepicker-input" name="timestamp" id="timestamp" autocomplete="off" data-toggle="datetimepicker" data-target="#timestamp" placeholder="Timestamp"/>
        </div>
    </div>
    <div class="form-group row">
        <label for="value" class="col-sm-2 col-form-label">Value</label>
        <div class="col-sm-10">
            <div class="khq-ace-editor">
                <div></div>
                <textarea class="form-control" name="value" id="value" placeholder="Value"></textarea>
            </div>
        </div>
    </div>

    <div class="khq-submit">
        <button type="submit" class="btn btn-primary">Produce</button>
    </div>
</form>

<@template.footer/>