package com.inesanet.dmedia.model;

import java.util.List;

/**
 * Created by Administrator on 2017/5/3.
 */

public class ConfigModel {
    private List<VersionInfo> version;
    public List<VersionInfo> getVersion() {
        return version;
    }
    public void setVersion(List<VersionInfo> version) {
        this.version = version;
    }
    public static class VersionInfo {
        private String latestversion;
        private String url;

        public String getLatestversion() {
            return latestversion;
        }

        public void setLatestversion(String latestversion) {
            this.latestversion = latestversion;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}

