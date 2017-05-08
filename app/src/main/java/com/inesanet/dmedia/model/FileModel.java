package com.inesanet.dmedia.model;

/**
 * Created by Administrator on 2017/5/3.
 */

public class FileModel {
    private String latestversion;
    private String url;

    public String getName() {
        return latestversion;
    }

    public void setName(String latestversion) {
        this.latestversion = latestversion;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
