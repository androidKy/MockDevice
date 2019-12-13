package com.gun.local.report;

import java.util.Map;

/**
 * description:
 * author: diff
 * date: 2018/1/15.
 */
public interface IReporter {
    String getServerAddress();

    Map<String, String> getParams();

    void report();
}
