import Axios from "axios";

const Api = {

    getHosts(callback) {
        return Api._get("/data/hosts", callback);
    },

    getCpuChart(host, mode, callback) {
        return Api._get("/data/cpu?host=" + host + "&mode=" + mode, callback);
    },

    getRamChart(host, mode, callback) {
        return Api._get("/data/ram?host=" + host + "&mode=" + mode, callback);
    },

    getNetChart(host, mode, callback) {
        return Api._get("/data/net?host=" + host + "&mode=" + mode, callback);
    },

    getDiskIoChart(host, mode, callback) {
        return Api._get("/data/disk/io?host=" + host + "&mode=" + mode, callback);
    },

    getDiskSpaceTable(host, callback) {
        return Api._get("/data/disk/space?host=" + host, callback);
    },

    _get(url, callback) {
        const cancelTokenSource = Axios.CancelToken.source();
        Axios
            .get(url, {cancelToken: cancelTokenSource.token})
            .then(res => callback(res.data, null))
            .catch(err => {
                if (!Axios.isCancel(err)) {
                    callback(null, err);
                }
            });

        return cancelTokenSource;
    },

};

export default Api;
