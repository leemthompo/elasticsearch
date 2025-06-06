/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.watcher;

import org.elasticsearch.TransportVersion;
import org.elasticsearch.TransportVersions;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xpack.core.XPackFeatureUsage;
import org.elasticsearch.xpack.core.XPackField;

import java.io.IOException;
import java.util.Map;

public class WatcherFeatureSetUsage extends XPackFeatureUsage {

    private final Map<String, Object> stats;

    public WatcherFeatureSetUsage(StreamInput in) throws IOException {
        super(in);
        stats = in.readGenericMap();
    }

    public WatcherFeatureSetUsage(boolean available, boolean enabled, Map<String, Object> stats) {
        super(XPackField.WATCHER, available, enabled);
        this.stats = stats;
    }

    @Override
    public TransportVersion getMinimalSupportedVersion() {
        return TransportVersions.ZERO;
    }

    public Map<String, Object> stats() {
        return stats;
    }

    @Override
    protected void innerXContent(XContentBuilder builder, Params params) throws IOException {
        super.innerXContent(builder, params);
        if (enabled) {
            for (Map.Entry<String, Object> entry : stats.entrySet()) {
                builder.field(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeGenericMap(stats);
    }
}
