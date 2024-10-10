/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import type { MetaExportWithBackendList } from '@/plugins/types';
import type { ClusterMetaType } from '../types';

export const allDefaultClusters: MetaExportWithBackendList<ClusterMetaType> = [
  {
    label: 'ALL',
    value: '',
    LoadEntity: () => import('../common/ClusterInfo').then(r => ({ default: r.ClusterInfo })),
  },
  {
    label: 'Agent',
    value: 'AGENT',
    LoadEntity: () => import('./Agent'),
  },
  {
    label: 'DataProxy',
    value: 'DATAPROXY',
    LoadEntity: () => import('./DataProxy'),
  },
  {
    label: 'Kafka',
    value: 'KAFKA',
    LoadEntity: () => import('./Kafka'),
  },
  {
    label: 'Pulsar',
    value: 'PULSAR',
    LoadEntity: () => import('./Pulsar'),
  },
  {
    label: 'TubeMQ',
    value: 'TUBEMQ',
    LoadEntity: () => import('./TubeMq'),
  },
  {
    label: 'Sort CLS',
    value: 'SORT_CLS',
    LoadEntity: () => import('./SortCls'),
  },
  {
    label: 'Sort ES',
    value: 'SORT_ES',
    LoadEntity: () => import('./SortEs'),
  },
  {
    label: 'Sort Pulsar',
    value: 'SORT_PULSAR',
    LoadEntity: () => import('./SortPulsar'),
  },
  {
    label: 'Sort Kafka',
    value: 'SORT_KAFKA',
    LoadEntity: () => import('./SortKafka'),
  },
  {
    label: 'Sort Http',
    value: 'SORT_HTTP',
    LoadEntity: () => import('./SortHttp'),
  },
];
