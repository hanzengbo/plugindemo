package com.wblive.plugin

class WBLivePluginExension {
    def injectEnable = true
    List<String> injectPackagesKey = new ArrayList<String>()
    List<String> disinjectClassSuffix = new ArrayList<String>()
    String logTag = "WBLiveTAG"

    public WBLivePluginExension injectPackagesKey(String...filters) {
        if (filters != null) {
            injectPackagesKey.addAll(filters)
        }
        return this
    }

    public List<String> getInjectPackagesKey() {
        return injectPackagesKey;
    }

    public String getLogTag() {
        return logTag;
    }

    public WBLivePluginExension disinjectClassSuffix(String...filters) {
        if (filters != null) {
            disinjectClassSuffix.addAll(filters)
        }

        return this
    }
}
