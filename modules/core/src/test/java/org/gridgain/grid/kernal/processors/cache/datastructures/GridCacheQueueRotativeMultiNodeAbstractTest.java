/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.datastructures;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.testframework.junits.common.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Queue multi node tests.
 */
public abstract class GridCacheQueueRotativeMultiNodeAbstractTest extends GridCommonAbstractTest
    implements Externalizable {
    /** */
    protected static final int GRID_CNT = 4;

    /** */
    protected static GridTcpDiscoveryIpFinder ipFinder = new GridTcpDiscoveryVmIpFinder(true);

    /** */
    protected static final int RETRIES = 133;

    /** */
    private static final int QUEUE_CAPACITY = 100000;

    /** */
    private static CountDownLatch lthTake = new CountDownLatch(1);

    /**
     * Constructs test.
     */
    protected GridCacheQueueRotativeMultiNodeAbstractTest() {
        super(/* don't start grid */ false);
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        for (int i = 0; i < GRID_CNT; i++)
            startGrid(i);

        assert G.allGrids().size() == GRID_CNT;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        assert G.allGrids().isEmpty();
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridTcpDiscoverySpi spi = new GridTcpDiscoverySpi();

        spi.setIpFinder(ipFinder);

        cfg.setDiscoverySpi(spi);

        return cfg;
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testPutRotativeNodes() throws Exception {
        String queueName = UUID.randomUUID().toString();

        GridCacheQueue<Integer> queue = grid(0).cache(null).dataStructures().queue(queueName, QUEUE_CAPACITY,
            true, true);

        assertTrue(queue.isEmpty());

        // Start and stop GRID_CNT*2 new nodes.
        for (int i = GRID_CNT; i < GRID_CNT * 3; i++) {
            startGrid(i);

            grid(i).forLocal().compute().call(new PutJob(queueName, RETRIES)).get();

            // last node must be alive.
            if (i < (GRID_CNT * 3) - 1)
                stopGrid(i);
        }

        queue = grid((GRID_CNT * 3) - 1).cache(null).dataStructures().queue(queueName, QUEUE_CAPACITY, true, false);

        assertEquals(RETRIES * GRID_CNT * 2, queue.size());
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testPutTakeRotativeNodes() throws Exception {
        String queueName = UUID.randomUUID().toString();

        GridCacheQueue<Integer> queue = grid(0).cache(null).dataStructures().queue(queueName, QUEUE_CAPACITY,
            true, true);

        assertTrue(queue.isEmpty());

        // Start and stop GRID_CNT*2 new nodes.
        for (int i = GRID_CNT; i < GRID_CNT * 3; i++) {
            startGrid(i);

            grid(i).forLocal().compute().call(new PutTakeJob(queueName, RETRIES));

            // last node must be alive.
            if (i < (GRID_CNT * 3) - 1)
                stopGrid(i);
        }

        queue = grid((GRID_CNT * 3) - 1).cache(null).dataStructures().queue(queueName, QUEUE_CAPACITY, true, false);

        assertEquals(0, queue.size());
    }

    /**
     * JUnit.
     *
     * @throws Exception If failed.
     */
    public void testTakeRemoveRotativeNodes() throws Exception {
        final String queueName = UUID.randomUUID().toString();

        GridCacheQueue<Integer> queue = grid(0).cache(null).dataStructures()
            .queue(queueName, QUEUE_CAPACITY, true, true);

        assertTrue(queue.isEmpty());

        Thread th = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    assert grid(1).compute().call(new TakeJob(queueName)).get();
                }
                catch (GridException e) {
                    error(e.getMessage(), e);
                }
            }
        });

        th.start();

        assert lthTake.await(5, TimeUnit.MINUTES) : "Timeout happened.";

        grid(2).compute().call(new RemoveQueueJob(queueName)).get();

        assert queue.removed();

        info("Queue was removed: " + queue);

        th.join();
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // No-op.
    }

    /**
     * Test job putting data to queue.
     */
    protected class PutJob implements GridCallable<Integer> {
        /** */
        @GridToStringExclude
        @GridInstanceResource
        private Grid grid;

        /** Queue name. */
        private final String queueName;

        /** */
        private final int retries;

        /**
         * @param queueName Queue name.
         * @param retries  Number of operations.
         */
        PutJob(String queueName, int retries) {
            this.queueName = queueName;
            this.retries = retries;
        }

        /** {@inheritDoc} */
        @Override public Integer call() throws GridException {
            assertNotNull(grid);

            grid.log().info("Running job [node=" + grid.localNode().id() + ", job=" + this + "]");

            GridCacheQueue<Integer> queue = grid.cache(null).dataStructures()
                .queue(queueName, QUEUE_CAPACITY, true, true);

            assertNotNull(queue);

            for (int i = 0; i < retries; i++)
                queue.put(i);

            return queue.size();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(PutJob.class, this);
        }
    }

    /**
     * Test job putting data to queue.
     */
    protected class GetJob implements GridCallable<Integer> {
        /** */
        @GridToStringExclude
        @GridInstanceResource
        private Grid grid;

        /** Queue name. */
        private final String queueName;

        /** */
        private final int retries;

        /** */
        private final String expVal;

        /**
         * @param queueName Queue name.
         * @param retries  Number of operations.
         * @param expVal Expected value.
         */
        GetJob(String queueName, int retries, String expVal) {
            this.queueName = queueName;
            this.retries = retries;
            this.expVal = expVal;
        }

        /** {@inheritDoc} */
        @Override public Integer call() throws GridException {
            assertNotNull(grid);

            grid.log().info("Running job [node=" + grid.localNode().id() + ", job=" + this + "]");

            GridCacheQueue<String> queue = grid.cache(null).dataStructures()
                .queue(queueName, QUEUE_CAPACITY, true, true);

            assertNotNull(queue);

            assertEquals(1, queue.size());

            for (int i = 0; i < retries; i++) {
                assertEquals(expVal, queue.peek());

                assertEquals(expVal, queue.element());
            }

            return queue.size();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(GetJob.class, this);
        }
    }

    /**
     * Test job putting and taking data to/from queue.
     */
    protected class PutTakeJob implements GridCallable<Integer> {
        /** */
        @GridToStringExclude
        @GridInstanceResource
        private Grid grid;

        /** Queue name. */
        private final String queueName;

        /** */
        private final int retries;

        /**
         * @param queueName Queue name.
         * @param retries  Number of operations.
         */
        PutTakeJob(String queueName, int retries) {
            this.queueName = queueName;
            this.retries = retries;
        }

        /** {@inheritDoc} */
        @Override public Integer call() throws GridException {
            assertNotNull(grid);

            grid.log().info("Running job [node=" + grid.localNode().id() + ", job=" + this + ']');

            GridCacheQueue<Integer> queue = grid.cache(null).dataStructures()
                .queue(queueName, QUEUE_CAPACITY, true, true);

            assertNotNull(queue);

            for (int i = 0; i < retries; i++) {
                queue.put(i);

                assertNotNull(queue.peek());

                assertNotNull(queue.element());

                assertNotNull(queue.take());
            }

            return queue.size();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(PutTakeJob.class, this);
        }
    }

    /**
     * Test job taking data from queue.
     */
    protected class TakeJob implements GridCallable<Boolean> {
        /** */
        @GridInstanceResource
        private Grid grid;

        /** Queue name. */
        private final String queueName;

        /**
         * @param queueName Queue name.
         */
        TakeJob(String queueName) {
            this.queueName = queueName;
        }

        /** {@inheritDoc} */
        @Override public Boolean call() throws GridException {
            assertNotNull(grid);

            grid.log().info("Running job [node=" + grid.localNode().id() + ", job=" + this + ']');

            GridCacheQueue<Integer> queue = grid.cache(null).dataStructures()
                .queue(queueName, QUEUE_CAPACITY, true, true);

            assertNotNull(queue);

            try {
                // Queue can be removed.
                lthTake.countDown();

                queue.take();
            }
            catch (GridRuntimeException e) {
                grid.log().info("Caught expected exception: " + e.getMessage());
            }

            return queue.removed();
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(TakeJob.class, this);
        }
    }

    /**
     * Job removing queue.
     */
    protected class RemoveQueueJob implements GridCallable<Boolean> {
        /** */
        @GridInstanceResource
        private Grid grid;

        /** Queue name. */
        private final String queueName;

        /**
         * @param queueName Queue name.
         */
        RemoveQueueJob(String queueName) {
            this.queueName = queueName;
        }

        /** {@inheritDoc} */
        @Override public Boolean call() throws GridException {
            assertNotNull(grid);

            grid.log().info("Running job [node=" + grid.localNode().id() + ", job=" + this + "]");

            GridCacheQueue<Integer> queue = grid.cache(null).dataStructures()
                .queue(queueName, QUEUE_CAPACITY, true, true);

            assert queue.capacity() == QUEUE_CAPACITY;

            assert grid.cache(null).dataStructures().removeQueue(queueName);

            return true;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(RemoveQueueJob.class, this);
        }
    }
}
