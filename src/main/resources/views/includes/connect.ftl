<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="plugins" type="java.util.List<org.akhq.models.ConnectPlugin>" -->

<#macro form pluginDefinition definition>
    <#-- @ftlvariable name="definition" type="org.akhq.models.ConnectDefinition" -->
    <#-- @ftlvariable name="pluginDefinition" type="java.util.Optional<org.akhq.models.ConnectPlugin>" -->

    <form method="get">
        <div class="form-group row">
            <label for="type" class="col-sm-2 col-form-label">Type</label>
            <div class="col-sm-10">
                <select onchange="submit()" class="form-control" name="type" id="type" ${(definition?has_content)?then(" readonly", "")}>
                    <#if !(definition?has_content)>
                        <option></option>
                        <#list plugins as plugin>
                            <option value="${plugin.getClassName()}" ${(pluginDefinition.isPresent() && pluginDefinition.get().getClassName() == plugin.getClassName())?then("selected", "")}>${plugin.getClassName()}
                                [${plugin.getVersion()}]
                            </option>
                        </#list>
                    <#else>
                        <option>${definition.getShortClassName()}</option>
                    </#if>
                </select>
            </div>
        </div>
    </form>

    <#if pluginDefinition.isPresent()>
        <form enctype="multipart/form-data" method="post" class="khq-form khq-form-config">
            <input type="hidden" name="configs[connector.class]" value="${pluginDefinition.get().getShortClassName()}" />
            <div class="form-group row">
                <label for="name" class="col-sm-2 col-form-label">Name</label>
                <div class="col-sm-10">
                    <input type="text" class="form-control" name="name" id="name" placeholder="Subject" value="${(definition?has_content)?then(definition.getName()!, "")}" required ${(definition?has_content)?then(" readonly", "")}>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-bordered table-striped mb-0 khq-form-config">
                    <thead class="thead-dark">
                        <tr>
                            <th style="width: 50%">Name</th>
                            <th>Value</th>
                        </tr>
                    </thead>
                    <tbody>
                        <#assign oldGroup = "">
                        <#list pluginDefinition.get().getDefinitions() as plugin>
                            <#if plugin.getName() == "name"  || plugin.getName() == "connector.class">
                                <#continue>
                            </#if>

                            <#if oldGroup != plugin.getGroup()>
                                <tr class="bg-primary">
                                    <td colspan="3">${plugin.getGroup()}</td>
                                </tr>
                            </#if>
                            <tr>
                                <td>
                                    <#if plugin.getDisplayName()??>
                                        <span>
                                        ${plugin.getDisplayName()}
                                            <#if plugin.getImportance() == "HIGH" >
                                                &nbsp;<i class="fa fa-exclamation text-danger" aria-hidden="true"></i>
                                        <#elseif plugin.getImportance() == "MEDIUM">
                                                &nbsp;<i class="fa fa-info text-warning" aria-hidden="true"></i>
                                            </#if>
                                    </span><br/>
                                    </#if>
                                    <code>${plugin.getName()}</code>
                                    <#if plugin.getDocumentation()??>
                                        <small class="form-text text-muted">
                                            ${plugin.getDocumentation()?replace('<[^>]+>','','r')}
                                        </small>
                                        <#if plugin.getDependents()?size != 0>
                                            <small class="form-text text-muted">
                                                Depends on: ${plugin.getDependentsAsString()}
                                            </small>
                                        </#if>
                                    </#if>
                                </td>
                                <td>
                                    <input type="text"
                                           class="form-control"
                                           autocomplete="off"
                                            <#if plugin.isRequired()>
                                                required
                                            </#if>
                                            <#if definition?has_content && definition.getConfig(plugin.getName()).isPresent()>
                                                value="${definition.getConfig(plugin.getName()).get()}"
                                            </#if>
                                           placeholder="${plugin.getDefaultValue()!""}"
                                           name="configs[${plugin.getName()}]"
                                    />
                                    <small class="humanize form-text text-muted"></small>
                                </td>
                            </tr>
                            <#if plugin.getName() == "transforms">
                                <tr>
                                    <td>
                                        <code>Transforms additional properties</code>
                                        <small class="form-text text-muted">
                                            Json object to be added to configurations. example:
                                            {
                                                "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
                                                "transforms.createKey.fields":"c1",
                                                "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
                                                "transforms.extractInt.field":"c1"
                                            }
                                        </small>
                                    </td>
                                    <td>
                                        <div class="khq-ace-editor" data-type="json">
                                            <div></div>
                                            <textarea class="form-control"
                                                      name="transforms-value"
                                                      id="transforms-value"
                                                      placeholder="Value">
                                            <#if definition?has_content>
                                                ${definition.getTransformConfigsAsJson()}
                                            </#if>
                                            </textarea>
                                        </div>
                                    </td>
                                </tr>
                            </#if>
                            <#assign oldGroup = plugin.getGroup()>
                        </#list>
                    </tbody>
                </table>
            </div>
            <div class="khq-submit">
                <button type="submit" class="btn btn-primary">${(definition?has_content)?then("Update", "Create")}</button>
            </div>
        </form>
    </#if>
</#macro>
