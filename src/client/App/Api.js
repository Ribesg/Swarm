import Axios from "axios";

const Api = {

    _401Callback: null,

    set401Callback(callback) {
        Api._401Callback = callback;
    },

    postLogin(user, password, callback) {
        return Api._request("post", "/login", callback, {
            user: user,
            password: password,
        });
    },

    postLogout(callback) {
        return Api._request("post", "/logout", callback);
    },

    getHosts(callback) {
        return Api._request("get", "/data/hosts", callback);
    },

    getCpuChart(host, mode, callback) {
        return Api._request("get", "/data/cpu?host=" + host + "&mode=" + mode, callback);
    },

    getRamChart(host, mode, callback) {
        return Api._request("get", "/data/ram?host=" + host + "&mode=" + mode, callback);
    },

    getNetChart(host, mode, callback) {
        return Api._request("get", "/data/net?host=" + host + "&mode=" + mode, callback);
    },

    getDiskIoChart(host, mode, callback) {
        return Api._request("get", "/data/disk/io?host=" + host + "&mode=" + mode, callback);
    },

    getDiskSpaceTable(host, callback) {
        return Api._request("get", "/data/disk/space?host=" + host, callback);
    },

    _request(method, url, callback, data) {
        const cancelTokenSource = Axios.CancelToken.source();
        Axios
            .request({
                method: method,
                url: url,
                data: data,
                cancelToken: cancelTokenSource.token,
            })
            .then(res => callback(res.data, null))
            .catch(err => {
                if (!Axios.isCancel(err)) {
                    if (err.response && err.response.status === 401) {
                        Api._401Callback();
                    } else {
                        callback(null, err);
                    }
                }
            });

        return cancelTokenSource;
    },

};

export default Api;
