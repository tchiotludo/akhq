import React from 'react';

function Pagination(props) {
    return (
        <ul className="pagination mb-0 ml-sm-2 pull-right pr-0" style={{padding: 1 + 'rem'}}>
            {/*<#if pagination["before"]??>
        <#assign before = pagination[" before"] >*/}
            {/*<li className=" page-item before ${(before == "") ? then('disabled', '')}">*/}
            <li className=" page-item before disabled">
                {/*<a className=" page-link" aria-label=" Previous" ${(before != "") ? then(' href="' ? no_esc + before + '"' ? no_esc, '')}>*/}
                <a className=" page-link">
                    <span aria-hidden=" true">&laquo;</span>
                    <span className=" sr-only">Previous</span>
                </a>
            </li>
            {/*</#if>*/}
            <li className=" page-item info">
                <a className=" page-link">
                    {/*${size}*/}
                    0
                </a>
            </li>
            {/*<li className=" page-item after ${(after == "") ? then('disabled', '')}">*/}
            <li className=" page-item after disabled">
                {/*<a className=" page-link" aria-label=" Next" ${(after != "")?then(' href="'?no_esc + after + '"'?no_esc, '')}>*/}
                <a className=" page-link" aria-label=" Next">
                    <span aria-hidden=" true">&raquo;</span>
                    <span className=" sr-only">Next</span>
                </a>
            </li>
        </ul>
    );
}

export default Pagination;