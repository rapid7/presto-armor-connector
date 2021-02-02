/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rapid7.presto.armor;

import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.rapid7.armor.shard.ShardId;

import javax.inject.Inject;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class ArmorSplitManager
        implements ConnectorSplitManager
{
	private ArmorClient armorClient;
    @Inject
    public ArmorSplitManager(ArmorClient armorClient)
    {
        this.armorClient = requireNonNull(armorClient, "client is null");
    }

    @Override
    public ConnectorSplitSource getSplits(
            ConnectorTransactionHandle transactionHandle,
            ConnectorSession session,
            ConnectorTableLayoutHandle layout,
            SplitSchedulingContext splitSchedulingContext)
    {
        ArmorTableLayoutHandle layoutHandle = (ArmorTableLayoutHandle) layout;
        ArmorTableHandle tableHandle = layoutHandle.getTable();
        
        String org = tableHandle.getSchema();
        String table = tableHandle.getTableName();
        try {
	        List<ShardId> shards = armorClient.getShardIds(org, table);
	        List<ArmorSplit> splits = shards.stream()
	                .map(shard -> new ArmorSplit(shard.getShardNum()))
	                .collect(toImmutableList());
	
	        return new FixedSplitSource(splits);
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }
    }
}