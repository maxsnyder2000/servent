const deleteRequest = (url) => {
    return fetch(url, {method: "DELETE"}).then((response) => {
        return response.json();
    }).then((response) => {
        return response.response;
    });
};

const getRequest = (url) => {
    return fetch(url, {method: "GET"}).then((response) => {
        return response.json();
    }).then((response) => {
        return response.response;
    });
};

const patchRequest = (url, body) => {
    const json = JSON.stringify(body);
    return fetch(url, {body: json, headers: {"Content-Type": "application/json"}, method: "PATCH"}).then((response) => {
        return response.json();
    }).then((response) => {
        return response.response;
    });
};

const postRequest = (url, body) => {
    const json = JSON.stringify(body);
    return fetch(url, {body: json, headers: {"Content-Type": "application/json"}, method: "POST"}).then((response) => {
        return response.json();
    }).then((response) => {
        return response.response;
    });
};

export {deleteRequest, getRequest, patchRequest, postRequest};
