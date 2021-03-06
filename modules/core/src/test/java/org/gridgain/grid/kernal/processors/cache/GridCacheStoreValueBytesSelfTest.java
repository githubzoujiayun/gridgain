/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
*  __  ____/___________(_)______  /__  ____/______ ____(_)_______
*  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
*  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
*  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
*/

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.testframework.junits.common.*;

import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCacheWriteSynchronizationMode.*;

/**
 * Test for {@link GridCacheConfiguration#isStoreValueBytes()}.
 */
public class GridCacheStoreValueBytesSelfTest extends GridCommonAbstractTest {
    /** */
    private boolean storeValBytes;

    /** VM ip finder for TCP discovery. */
    private static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi disco = new GridTcpDiscoverySpi();

        disco.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(disco);

        GridCacheConfiguration ccfg = defaultCacheConfiguration();

        ccfg.setCacheMode(REPLICATED);
        ccfg.setWriteSynchronizationMode(FULL_SYNC);
        ccfg.setStoreValueBytes(storeValBytes);

        cfg.setCacheConfiguration(ccfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        stopAllGrids();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testDisabled() throws Exception {
        storeValBytes = false;

        Grid g0 = startGrid(0);
        Grid g1 = startGrid(1);

        GridCache<Integer, String> c = g0.cache(null);

        c.put(1, "Cached value");

        GridCacheEntryEx<Object, Object> entry = ((GridKernal)g1).internalCache().peekEx(1);

        assert entry != null;
        assert entry.valueBytes().isNull();
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testEnabled() throws Exception {
        storeValBytes = true;

        Grid g0 = startGrid(0);
        Grid g1 = startGrid(1);

        GridCache<Integer, String> c = g0.cache(null);

        c.put(1, "Cached value");

        GridCacheEntryEx<Object, Object> entry = ((GridKernal)g1).internalCache().peekEx(1);

        assert entry != null;
        assert entry.valueBytes() != null;
    }
}
