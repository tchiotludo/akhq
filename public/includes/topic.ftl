<#macro data datas>
    <!--
    <nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto data-filter">
        <button class="navbar-toggler"
                type="button"
                data-toggle="collapse"
                data-target="#topic-data"
                aria-controls="topic-data"
                aria-expanded="false"
                aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>

        <nav>
            <ul class="pagination mb-0 ml-sm-2">
                <li class="page-item">
                    <a class="page-link" href="#" aria-label="Previous">
                        <span aria-hidden="true">&laquo;</span>
                        <span class="sr-only">Previous</span>
                    </a>
                </li>
                <li class="page-item">
                    <a class="page-link" href="#" aria-label="Next">
                        <span aria-hidden="true">&raquo;</span>
                        <span class="sr-only">Next</span>
                    </a>
                </li>
            </ul>
        </nav>

        <div class="collapse navbar-collapse" id="topic-data">
            <ul class="navbar-nav mr-auto">
                <li class="nav-item dropdown">
                    <div class="dropdown-menu">
                        <a class="dropdown-item" href="#">Action</a>
                        <a class="dropdown-item" href="#">Another action</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="#">Something else here</a>
                    </div>
                </li>

                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        Count (20)
                    </a>
                    <div class="dropdown-menu">
                        <a class="dropdown-item" href="#">Action</a>
                        <a class="dropdown-item" href="#">Another action</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="#">Something else here</a>
                    </div>
                </li>

                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        Partition (All)
                    </a>
                    <div class="dropdown-menu">
                        <a class="dropdown-item" href="#">Action</a>
                        <a class="dropdown-item" href="#">Another action</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="#">Something else here</a>
                    </div>
                </li>
                <li class="nav-item ml-md-2">
                    <form class="form-inline">
                        <input class="form-control mr-sm-2" type="search" placeholder="Search" aria-label="Search">
                        <button class="btn btn-outline-success my-2 my-sm-0" type="submit">Search</button>
                    </form>
                </li>
            </ul>
        </div>
    </nav>
    -->
    <div class="table-responsive">
        <table class="table table-bordered table-striped table-hover mb-0">
            <thead class="thead-dark">
                <tr>
                    <th>Key</th>
                    <th>Date</th>
                    <th>Partition</th>
                    <th>Offset</th>
                </tr>
            </thead>
            <tbody>
            <#if datas?size == 0>
                <tr>
                    <td colspan="5">
                        <div class="alert alert-info mb-0" role="alert">
                            No data available
                        </div>
                    </td>
                </tr>
            </#if>
        <#list datas as data>
            <tr>
                <td><code>${data.key()!'null'}</code></td>
                <td>${data.timestamp()?number_to_datetime?string.medium_short}</td>
                <td>${data.partition()}</td>
                <td>${data.offset()}</td>
            </tr>
            <tr>
                <td colspan="4">
                    <pre class="mb-0"><code>${data.value()!'null'}</code></pre>
                </td>
            </tr>
        </#list>
            </tbody>
        </table>
    </div>
</#macro>
