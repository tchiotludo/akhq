import React from 'react';

function Header({title, children}) {
    return (
        <span>
            <div className="title">
                <h1>
                    {title}
                </h1>
                {children}
            </div>
        </span>
    );
}

export default Header;