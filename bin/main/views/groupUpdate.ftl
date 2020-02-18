<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="group" type="org.kafkahq.models.ConsumerGroup" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "includes/template.ftl" as template>
<#import "includes/functions.ftl" as functions>

<@template.header "Update offsets: " + group.getId(), "group" />

<form enctype="multipart/form-data" method="post" class="khq-form khq-update-consumer-group-offsets">
    <#list group.getGroupedTopicOffset() as topic, offsets>
        <fieldset>
            <legend>${topic}</legend>
            <#list offsets as offset>
                <div class="form-group row">
                    <label for="replication" class="col-sm-2 col-form-label">Partition: ${offset.getPartition()}</label>
                    <div class="col-sm-10">
                        <input type="number"
                               min="${offset.getFirstOffset().orElse(0)?c}"
                               max="${offset.getLastOffset().orElse(0)?c}"
                               class="form-control"
                               name="offset[${topic}][${offset.getPartition()}]"
                               id="offset[${topic}][${offset.getPartition()}]"
                               placeholder="Offset"
                               value="${offset.getOffset().orElse(0)?c}"
                               required>
                    </div>
                </div>
            </#list>
        </fieldset>
    </#list>

    <div class="khq-submit">
        <div>
            <div class="btn-group dropup">
                <button type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                    Reset offsets to ...
                </button>
                <div class="dropdown-menu">
                    <a class="dropdown-item khq-first-offsets">firsts offsets</a>
                    <a class="dropdown-item khq-last-offsets">last offsets</a>
                    <a class="dropdown-item khq-datetime-offsets">datetime</a>
                    <div class="khq-datetime d-none">
                        <div class="input-group mb-2">
                            <input class="form-control"
                                   name="timestamp"
                                   type="text" />
                            <div class="input-group-append">
                                <button class="btn btn-primary" type="button">OK</button>
                            </div>
                        </div>
                        <div class="datetime-container"></div>
                    </div>
                </div>
            </div>

        </div>
        <div>
            <button type="submit" class="btn btn-primary">Update</button>
        </div>
    </div>
</form>

<@template.footer/>
