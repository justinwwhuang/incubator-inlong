/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.broker.stats;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.metric.impl.ESTHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.LongOnlineCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.LongStatsCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.SinceTime;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BrokerSrvStatsHolder, statistic Broker metrics information for RPC services
 *
 * The metrics are placed independently or in switchableSets according to
 * whether switchable statistics are allowed, and the value of metrics is changed
 * via the corresponding metric API.
 */
public class BrokerSrvStatsHolder {

    // Consumer client online statistic
    private static final LongOnlineCounter csmOnlineCnt =
            new LongOnlineCounter("consume_online_cnt", null);
    // Switchable statistic items
    private static final ServiceStatsSet[] switchableSets = new ServiceStatsSet[2];
    // Current writable index
    private static final AtomicInteger writableIndex = new AtomicInteger(0);
    // Last snapshot time
    private static final AtomicLong lstSnapshotTime = new AtomicLong(0);
    // whether the detail statistic is closed
    private static volatile boolean detailStatsClosed = false;

    // Initial service statistic set
    static {
        switchableSets[0] = new ServiceStatsSet();
        switchableSets[1] = new ServiceStatsSet();
    }

    // metric set operate APIs begin
    public static void getValue(Map<String, Long> statsMap) {
        getStatsValue(switchableSets[getIndex()], false, statsMap);
    }

    public static void getValue(StringBuilder strBuff) {
        getStatsValue(switchableSets[getIndex()], false, strBuff);
    }

    public static void snapShort(Map<String, Long> statsMap) {
        if (switchWritingStatsUnit()) {
            getStatsValue(switchableSets[getIndex(writableIndex.get() - 1)], true, statsMap);
        } else {
            getStatsValue(switchableSets[getIndex()], false, statsMap);
        }
    }

    public static void snapShort(StringBuilder strBuff) {
        if (switchWritingStatsUnit()) {
            getStatsValue(switchableSets[getIndex(writableIndex.get() - 1)], true, strBuff);
        } else {
            getStatsValue(switchableSets[getIndex()], false, strBuff);
        }
    }

    /**
     * Set manually the detail statistic status.
     *
     * @param enableStats  enable or disable the statistic.
     */
    public static synchronized void setDetailStatsStatus(boolean enableStats) {
        BrokerSrvStatsHolder.detailStatsClosed = !enableStats;
    }

    /**
     * Query whether the detail statistic is closed.
     *
     * @return the statistic status
     */
    public static boolean isDetailStatsClosed() {
        return BrokerSrvStatsHolder.detailStatsClosed;
    }

    // metric set operate APIs end

    // metric item operate APIs begin
    public static void incConsumeOnlineCnt() {
        csmOnlineCnt.incValue();
    }

    public static void decConsumeOnlineCnt(boolean isTimeout) {
        csmOnlineCnt.decValue();
        if (isTimeout) {
            switchableSets[getIndex()].csmTimeoutStats.incValue();
        }
    }

    public static void incBrokerTimeoutCnt() {
        switchableSets[getIndex()].brokerTimeoutStats.incValue();
    }

    public static void incBrokerHBExcCnt() {
        switchableSets[getIndex()].brokerHBExcStats.incValue();
    }

    public static void incDiskIOExcCnt() {
        switchableSets[getIndex()].fileIOExcStats.incValue();
    }

    public static void incZKExcCnt() {
        switchableSets[getIndex()].zkExcStats.incValue();
    }

    public static void incOffsetFileExcCnt() {
        switchableSets[getIndex()].offsetFileExcStats.incValue();
    }

    public static void updOffsetFileSyncDataDlt(long dltTime) {
        switchableSets[getIndex()].offsetFileSyncDltStats.update(dltTime);
    }

    public static void updDiskSyncDataDlt(long dltTime) {
        if (detailStatsClosed) {
            return;
        }
        switchableSets[getIndex()].fileSyncDltStats.update(dltTime);
    }

    public static void updZKSyncDataDlt(long dltTime) {
        switchableSets[getIndex()].zkSyncDltStats.update(dltTime);
    }

    public static void updSendMsgLatency(long dltTime) {
        if (detailStatsClosed) {
            return;
        }
        switchableSets[getIndex()].msgPubLatencyStats.update(dltTime);
    }

    public static void incSendMsgOverFlowCnt() {
        if (detailStatsClosed) {
            return;
        }
        switchableSets[getIndex()].errPubOverFlowStats.incValue();
    }

    public static void updGetMsgLatency(long dltTime) {
        if (detailStatsClosed) {
            return;
        }
        switchableSets[getIndex()].msgSubLatencyStats.update(dltTime);
    }

    public static void updConfirmLatency(long dltTime) {
        if (detailStatsClosed) {
            return;
        }
        switchableSets[getIndex()].msgConfirmLatencyStats.update(dltTime);
    }

    // metric set operate APIs end

    // private functions
    private static boolean switchWritingStatsUnit() {
        long curSnapshotTime = lstSnapshotTime.get();
        // Avoid frequent snapshots
        if ((System.currentTimeMillis() - curSnapshotTime) >= TBaseConstants.CFG_STATS_MIN_SNAPSHOT_PERIOD_MS) {
            if (lstSnapshotTime.compareAndSet(curSnapshotTime, System.currentTimeMillis())) {
                switchableSets[getIndex(writableIndex.incrementAndGet())].resetSinceTime();
                return true;
            }
        }
        return false;
    }

    private static void getStatsValue(ServiceStatsSet statsSet,
            boolean resetValue,
            Map<String, Long> statsMap) {
        statsMap.put(statsSet.lstResetTime.getFullName(),
                statsSet.lstResetTime.getSinceTime());
        statsMap.put("detailStatsClosed", (detailStatsClosed ? 1L : 0L));
        if (resetValue) {
            statsMap.put(statsSet.fileIOExcStats.getFullName(),
                    statsSet.fileIOExcStats.getAndResetValue());
            statsMap.put(statsSet.zkExcStats.getFullName(),
                    statsSet.zkExcStats.getAndResetValue());
            statsMap.put(statsSet.offsetFileExcStats.getFullName(),
                    statsSet.offsetFileExcStats.getAndResetValue());
            statsMap.put(statsSet.brokerTimeoutStats.getFullName(),
                    statsSet.brokerTimeoutStats.getAndResetValue());
            statsMap.put(statsSet.brokerHBExcStats.getFullName(),
                    statsSet.brokerHBExcStats.getAndResetValue());
            statsMap.put(csmOnlineCnt.getFullName(),
                    csmOnlineCnt.getAndResetValue());
            statsMap.put(statsSet.csmTimeoutStats.getFullName(),
                    statsSet.csmTimeoutStats.getAndResetValue());
            statsMap.put(statsSet.errPubOverFlowStats.getFullName(),
                    statsSet.errPubOverFlowStats.getAndResetValue());
            statsSet.fileSyncDltStats.snapShort(statsMap, false);
            statsSet.offsetFileSyncDltStats.snapShort(statsMap, false);
            statsSet.zkSyncDltStats.snapShort(statsMap, false);
            statsSet.msgPubLatencyStats.snapShort(statsMap, false);
            statsSet.msgSubLatencyStats.snapShort(statsMap, false);
            statsSet.msgConfirmLatencyStats.snapShort(statsMap, false);
        } else {
            statsMap.put(statsSet.fileIOExcStats.getFullName(),
                    statsSet.fileIOExcStats.getValue());
            statsMap.put(statsSet.zkExcStats.getFullName(),
                    statsSet.zkExcStats.getValue());
            statsMap.put(statsSet.offsetFileExcStats.getFullName(),
                    statsSet.offsetFileExcStats.getValue());
            statsMap.put(statsSet.brokerTimeoutStats.getFullName(),
                    statsSet.brokerTimeoutStats.getValue());
            statsMap.put(statsSet.brokerHBExcStats.getFullName(),
                    statsSet.brokerHBExcStats.getValue());
            statsMap.put(csmOnlineCnt.getFullName(),
                    csmOnlineCnt.getValue());
            statsMap.put(statsSet.csmTimeoutStats.getFullName(),
                    statsSet.csmTimeoutStats.getValue());
            statsMap.put(statsSet.errPubOverFlowStats.getFullName(),
                    statsSet.errPubOverFlowStats.getValue());
            statsSet.fileSyncDltStats.getValue(statsMap, false);
            statsSet.offsetFileSyncDltStats.getValue(statsMap, false);
            statsSet.zkSyncDltStats.getValue(statsMap, false);
            statsSet.msgPubLatencyStats.getValue(statsMap, false);
            statsSet.msgSubLatencyStats.getValue(statsMap, false);
            statsSet.msgConfirmLatencyStats.getValue(statsMap, false);
        }
    }

    private static void getStatsValue(ServiceStatsSet statsSet,
            boolean resetValue,
            StringBuilder strBuff) {
        strBuff.append("{\"").append(statsSet.lstResetTime.getFullName())
                .append("\":\"").append(statsSet.lstResetTime.getStrSinceTime())
                .append("\",\"detailStatsClosed\":").append(detailStatsClosed);
        if (resetValue) {
            strBuff.append(",\"").append(statsSet.fileIOExcStats.getFullName())
                    .append("\":").append(statsSet.fileIOExcStats.getAndResetValue())
                    .append(",\"").append(statsSet.zkExcStats.getFullName())
                    .append("\":").append(statsSet.zkExcStats.getAndResetValue())
                    .append(",\"").append(statsSet.offsetFileExcStats.getFullName())
                    .append("\":").append(statsSet.offsetFileExcStats.getAndResetValue())
                    .append(",\"").append(statsSet.brokerTimeoutStats.getFullName())
                    .append("\":").append(statsSet.brokerTimeoutStats.getAndResetValue())
                    .append(",\"").append(statsSet.brokerHBExcStats.getFullName())
                    .append("\":").append(statsSet.brokerHBExcStats.getAndResetValue())
                    .append(",\"").append(csmOnlineCnt.getFullName())
                    .append("\":").append(csmOnlineCnt.getAndResetValue())
                    .append(",\"").append(statsSet.csmTimeoutStats.getFullName())
                    .append("\":").append(statsSet.csmTimeoutStats.getAndResetValue())
                    .append(",\"").append(statsSet.errPubOverFlowStats.getFullName())
                    .append("\":").append(statsSet.errPubOverFlowStats.getAndResetValue())
                    .append(",");
            statsSet.fileSyncDltStats.snapShort(strBuff, false);
            strBuff.append(",");
            statsSet.offsetFileSyncDltStats.snapShort(strBuff, false);
            strBuff.append(",");
            statsSet.zkSyncDltStats.snapShort(strBuff, false);
            strBuff.append(",");
            statsSet.msgPubLatencyStats.snapShort(strBuff, false);
            strBuff.append(",");
            statsSet.msgSubLatencyStats.snapShort(strBuff, false);
            strBuff.append(",");
            statsSet.msgConfirmLatencyStats.snapShort(strBuff, false);
            strBuff.append("}");
        } else {
            strBuff.append(",\"").append(statsSet.fileIOExcStats.getFullName())
                    .append("\":").append(statsSet.fileIOExcStats.getValue())
                    .append(",\"").append(statsSet.zkExcStats.getFullName())
                    .append("\":").append(statsSet.zkExcStats.getValue())
                    .append(",\"").append(statsSet.offsetFileExcStats.getFullName())
                    .append("\":").append(statsSet.offsetFileExcStats.getAndResetValue())
                    .append(",\"").append(statsSet.brokerTimeoutStats.getFullName())
                    .append("\":").append(statsSet.brokerTimeoutStats.getValue())
                    .append(",\"").append(statsSet.brokerHBExcStats.getFullName())
                    .append("\":").append(statsSet.brokerHBExcStats.getValue())
                    .append(",\"").append(csmOnlineCnt.getFullName())
                    .append("\":").append(csmOnlineCnt.getValue())
                    .append(",\"").append(statsSet.csmTimeoutStats.getFullName())
                    .append("\":").append(statsSet.csmTimeoutStats.getValue())
                    .append(",\"").append(statsSet.errPubOverFlowStats.getFullName())
                    .append("\":").append(statsSet.errPubOverFlowStats.getValue())
                    .append(",");
            statsSet.fileSyncDltStats.getValue(strBuff, false);
            strBuff.append(",");
            statsSet.offsetFileSyncDltStats.getValue(strBuff, false);
            strBuff.append(",");
            statsSet.zkSyncDltStats.getValue(strBuff, false);
            strBuff.append(",");
            statsSet.msgPubLatencyStats.getValue(strBuff, false);
            strBuff.append(",");
            statsSet.msgSubLatencyStats.getValue(strBuff, false);
            strBuff.append(",");
            statsSet.msgConfirmLatencyStats.getValue(strBuff, false);
            strBuff.append("}");
        }
    }

    /**
     * Get current writable block index.
     *
     * @return the writable block index
     */
    private static int getIndex() {
        return getIndex(writableIndex.get());
    }

    /**
     * Gets the metric block index based on the specified value.
     *
     * @param origIndex    the specified value
     * @return the metric block index
     */
    private static int getIndex(int origIndex) {
        return Math.abs(origIndex % 2);
    }

    /**
     * ServiceStatsSet, Switchable metric data statistics block
     *
     * In which the object is the metric item that can be counted in stages
     */
    private static class ServiceStatsSet {

        protected final SinceTime lstResetTime =
                new SinceTime("reset_time", null);
        // Delay statistics for syncing data to files
        protected final ESTHistogram fileSyncDltStats =
                new ESTHistogram("file_sync_dlt", null);
        // Disk IO Exception statistics
        protected final LongStatsCounter fileIOExcStats =
                new LongStatsCounter("file_exc_cnt", null);
        // Delay statistics for syncing data to Zookeeper
        protected final ESTHistogram zkSyncDltStats =
                new ESTHistogram("zk_sync_dlt", null);
        // Zookeeper Exception statistics
        protected final LongStatsCounter zkExcStats =
                new LongStatsCounter("zk_exc_cnt", null);
        protected final LongStatsCounter offsetFileExcStats =
                new LongStatsCounter("offset_file_exc_cnt", null);
        // Delay statistics for syncing data to File offset
        protected final ESTHistogram offsetFileSyncDltStats =
                new ESTHistogram("offset_file_sync_dlt", null);
        // Broker 2 Master status statistics
        protected final LongStatsCounter brokerTimeoutStats =
                new LongStatsCounter("broker_timeout_cnt", null);
        protected final LongStatsCounter brokerHBExcStats =
                new LongStatsCounter("broker_hb_exc_cnt", null);
        // Consumer 2 Broker status statistics
        protected final LongStatsCounter csmTimeoutStats =
                new LongStatsCounter("consume_timeout_cnt", null);
        // sendMessage process latency statistics
        protected final ESTHistogram msgPubLatencyStats =
                new ESTHistogram("msg_put_dlt", null);
        // error sendMessage response distribution statistics
        protected final LongStatsCounter errPubOverFlowStats =
                new LongStatsCounter("msg_put_overflow", null);
        // getMessage process latency statistics
        protected final ESTHistogram msgSubLatencyStats =
                new ESTHistogram("msg_get_dlt", null);
        // confirm process latency statistics
        protected final ESTHistogram msgConfirmLatencyStats =
                new ESTHistogram("msg_confirm_dlt", null);

        public ServiceStatsSet() {
            resetSinceTime();
        }

        public void resetSinceTime() {
            this.lstResetTime.reset();
        }
    }
}
