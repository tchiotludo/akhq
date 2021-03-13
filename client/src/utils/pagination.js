
export const handlePageChange = ({ currentTarget: input, state }) => {
    const { value } = input;
    state.pageNumber = value;
};

export const getPageNumber = (value, totalPageNumber) => {
    if (value <= 0) {
        value = 1;
    } else if (value > totalPageNumber) {
        value = totalPageNumber;
    }
    return value;
};

export default {handlePageChange, getPageNumber};
