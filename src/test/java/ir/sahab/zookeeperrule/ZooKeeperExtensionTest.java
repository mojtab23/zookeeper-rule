package ir.sahab.zookeeperrule;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static ir.sahab.zookeeperrule.ZooKeeperBase.anOpenPort;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ZooKeeperExtensionTest {
    private static final String LOCAL_ADDRESS = "127.0.0.1:" + anOpenPort();

    @RegisterExtension
    static ZooKeeperExtension zkClassWideExtension = new ZooKeeperExtension(LOCAL_ADDRESS);

    @RegisterExtension
    static ZooKeeperExtension zooKeeperExtension = new ZooKeeperExtension(TestInstance.Lifecycle.PER_METHOD);

    @Test
    void testZKClassRule() throws Exception {
        checkZkIsClean();

        // Check client without namespace
        try (CuratorFramework client = zkClassWideExtension.newClient()) {
            checkWriteAndRead(client, "/no-namespace-path");
        }

        // Check client with specific namespace
        try (CuratorFramework client = zkClassWideExtension.newClient("test-ns")) {
            checkWriteAndRead(client, "/namespace-path");
        }
        // Check external client
        try (CuratorFramework client = CuratorFrameworkFactory
                .newClient(zkClassWideExtension.getAddress(), 30000, 3000, new RetryNTimes(3, 100))) {
            client.start();
            checkWriteAndRead(client, "/external-client-path");
        }

        makeZkDirty();
    }

    @Test
    void testZkRule() throws Exception {
        checkZkIsClean();

        // Check client without namespace
        try (CuratorFramework client = zooKeeperExtension.newClient()) {
            checkWriteAndRead(client, "/no-namespace-path");
        }

        // Check client with specific namespace
        try (CuratorFramework client = zooKeeperExtension.newClient("test-ns")) {
            checkWriteAndRead(client, "/namespace-path");
        }
        // Check external client
        try (CuratorFramework client = CuratorFrameworkFactory
                .newClient(zooKeeperExtension.getAddress(), 30000, 3000, new RetryNTimes(3, 100))) {
            client.start();
            checkWriteAndRead(client, "/external-client-path");
        }

        makeZkDirty();
    }

    @Test
    void testExplicitAddress() throws Exception {
        checkZkIsClean();

        assertEquals(Integer.parseInt(LOCAL_ADDRESS.split(":")[1]), zkClassWideExtension.getPort());
        assertEquals(LOCAL_ADDRESS, zkClassWideExtension.getAddress());

        makeZkDirty();
    }

    private void checkWriteAndRead(CuratorFramework client, String path)
            throws Exception {
        byte[] zNodeDataBytes = "data".getBytes();
        client.create().forPath(path, zNodeDataBytes);
        byte[] resultBytes = client.getData().forPath(path);
        assertArrayEquals(resultBytes, zNodeDataBytes);
    }

    private void checkZkIsClean() throws Exception {
        CuratorFramework client = zooKeeperExtension.newClient();
        List<String> children = client.getChildren().forPath("/");

        // By default, a node named "zookeeper" exists under root.
        // So we expect here the size 1 instead of size 0.
        assertEquals(1, children.size());
        assertEquals("zookeeper", children.get(0));
    }

    private void makeZkDirty()
            throws Exception {
        CuratorFramework client = zooKeeperExtension.newClient();
        client.create().forPath("/dirtyPath", "dirtyData".getBytes());
        client.close();
    }
}