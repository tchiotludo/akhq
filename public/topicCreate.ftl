<#import "/includes/template.ftl" as template>
<#import "/includes/functions.ftl" as functions>

<@template.header "Create a topic", "topic" />

<form enctype="multipart/form-data" method="post" class="khq-form khq-form-config">
    <div class="form-group row">
        <label for="name" class="col-sm-2 col-form-label">Name</label>
        <div class="col-sm-10">
            <input type="text" class="form-control" name="name" id="name" placeholder="Name" required>
        </div>
    </div>
    <div class="form-group row">
        <label for="partition" class="col-sm-2 col-form-label">Partition</label>
        <div class="col-sm-10">
            <input type="number" min="1" class="form-control" name="partition" id="partition" placeholder="Partition" value="1" required>
        </div>
    </div>
    <div class="form-group row">
        <label for="replication" class="col-sm-2 col-form-label">Replicator Factor</label>
        <div class="col-sm-10">
            <input type="number" min="1" class="form-control" name="replication"  id="replication" placeholder="Replicator Factor" value="1" required>
        </div>
    </div>
    <fieldset class="form-group">
        <div class="row">
            <legend class="col-form-label col-sm-2 pt-0">Cleanup Policy</legend>
            <div class="col-sm-10">
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="configs[cleanup.policy]" id="configs[cleanup.policy]1" value="delete" checked>
                    <label class="form-check-label" for="configs[cleanup.policy]1">
                        Delete
                    </label>
                </div>
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="configs[cleanup.policy]" id="configs[cleanup.policy]2" value="compact">
                    <label class="form-check-label" for="configs[cleanup.policy]2">
                        Compact
                    </label>
                </div>
                <div class="form-check disabled">
                    <input class="form-check-input" type="radio" name="configs[cleanup.policy]" id="configs[cleanup.policy]3" value="delete,compact">
                    <label class="form-check-label" for="configs[cleanup.policy]3">
                        Delete & Compact
                    </label>
                </div>
            </div>
        </div>
    </fieldset>
    <div class="form-group row">
        <label for="configs[retention.ms]" class="col-sm-2 col-form-label">Retention</label>
        <div class="col-sm-10">
            <input type="number" class="form-control" name="configs[retention.ms]" id="configs[retention.ms]" placeholder="Retention" value="86400000">
        </div>
    </div>
    <div class="khq-submit">
        <button type="submit" class="btn btn-primary">Create</button>
    </div>
</form>

<@template.footer/>