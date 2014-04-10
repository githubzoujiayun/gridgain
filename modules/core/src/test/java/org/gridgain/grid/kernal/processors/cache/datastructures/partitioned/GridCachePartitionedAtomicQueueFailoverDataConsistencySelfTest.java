/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.datastructures.partitioned;

import org.gridgain.grid.cache.*;
import org.gridgain.grid.kernal.processors.cache.datastructures.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;

/**
 * Queue failover test for atomic cache.
 */
public class GridCachePartitionedAtomicQueueFailoverDataConsistencySelfTest extends
    GridCacheAbstractQueueFailoverDataConsistencySelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheAtomicityMode atomicityMode() {
        return ATOMIC;
    }
}
