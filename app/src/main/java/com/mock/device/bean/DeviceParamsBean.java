package com.mock.device.bean;

/**
 * Description:
 * Created by Quinin on 2019-08-09.
 **/
public class DeviceParamsBean {

    /**
     * code : 200
     * msg : 成功
     * data : {"id":115695,"bluetooth":"60:A4:D0:80:69:2D","imei":"354734080197698","brand":"samsung","android":"6.0.1","mac":"60:A4:D0:80:69:2E","system":"MMB29M.C7010ZCU1AQK1","sn":"0625ca46","imsi":"460005514156013","model":"SM-C7010","useragent":"Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-C7010 Build/MMB29M)"}
     */

    private int code;
    private String msg;
    private DeviceBean data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DeviceBean getData() {
        return data;
    }

    public void setData(DeviceBean data) {
        this.data = data;
    }

    public static class DeviceBean {
        /**
         * id : 115695
         * bluetooth : 60:A4:D0:80:69:2D
         * imei : 354734080197698
         * brand : samsung
         * android : 6.0.1
         * mac : 60:A4:D0:80:69:2E
         * system : MMB29M.C7010ZCU1AQK1
         * sn : 0625ca46
         * imsi : 460005514156013
         * model : SM-C7010
         * useragent : Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-C7010 Build/MMB29M)
         */

        private long id;
        private String bluetooth;
        private String imei;
        private String brand;
        private String android;
        private String mac;
        private String system;
        private String sn;
        private String imsi;
        private String model;
        private String useragent;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getBluetooth() {
            return bluetooth;
        }

        public void setBluetooth(String bluetooth) {
            this.bluetooth = bluetooth;
        }

        public String getImei() {
            return imei;
        }

        public void setImei(String imei) {
            this.imei = imei;
        }

        public String getBrand() {
            return brand;
        }

        public void setBrand(String brand) {
            this.brand = brand;
        }

        public String getAndroid() {
            return android;
        }

        public void setAndroid(String android) {
            this.android = android;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getSn() {
            return sn;
        }

        public void setSn(String sn) {
            this.sn = sn;
        }

        public String getImsi() {
            return imsi;
        }

        public void setImsi(String imsi) {
            this.imsi = imsi;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getUseragent() {
            return useragent;
        }

        public void setUseragent(String useragent) {
            this.useragent = useragent;
        }
    }
}
