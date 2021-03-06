/*
Copyright (c) 2013 Marian Ciobanu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package net.bluehornreader.data;

import com.google.common.collect.*;
import com.netflix.astyanax.*;
import com.netflix.astyanax.connectionpool.*;
import com.netflix.astyanax.connectionpool.exceptions.*;
import com.netflix.astyanax.connectionpool.impl.*;
import com.netflix.astyanax.impl.*;
import com.netflix.astyanax.model.*;
import com.netflix.astyanax.serializers.*;
import com.netflix.astyanax.thrift.*;
import net.bluehornreader.misc.*;
import net.bluehornreader.model.*;
import org.apache.commons.logging.*;

import java.nio.*;
import java.util.*;
import java.util.concurrent.*;

/*

./cassandra
pkill -f CassandraDaemon
./cqlsh -3

 */

/**
 * Created with IntelliJ IDEA.
 * User: ciobi
 * Date: 2013-04-20
 * Time: 15:55
 * <p/>
 *
 * On the constructor it connects to the database, but there's a "shutDown()" to disconnect
 */
public class LowLevelDbAccess {

    private static final Log LOG = LogFactory.getLog(LowLevelDbAccess.class);

    public static ColumnFamily<Integer, String> RESULTS_CF = ColumnFamily.newColumnFamily(
            "ResultsCF",
            IntegerSerializer.get(),
            StringSerializer.get());

    private Keyspace mainKeyspace;
    private AstyanaxContext<Keyspace> mainContext;

    private Keyspace electionKeyspace;
    private AstyanaxContext<Keyspace> electionContext;

    /**
     * To be used for blob writing. Thread-safe.
     */
    public static ByteBufferSerializer BYTE_BUFFER_SERIALIZER = new ByteBufferSerializer();

    public LowLevelDbAccess() throws Exception {
        start();
    }

    public Keyspace getMainKeyspace() {
        return mainKeyspace;
    }

    public Keyspace getElectionKeyspace() {
        return electionKeyspace;
    }

    public void shutDown() {
        electionContext.shutdown();
        mainContext.shutdown();
        LOG.info("LowLevelDbAccess closed connection");
    }


    private void start() throws Exception {
        Config config = Config.getConfig();
        LOG.info("Entering LowLevelDbAccess.start()");

        {
            mainContext = new AstyanaxContext.Builder()
                    .forCluster(config.clusterName)
                    .forKeyspace("bluehorn_reader")
                            /*.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                                    .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
                            )*/
                    .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                            .setCqlVersion(config.cqlVersion)
                            .setTargetCassandraVersion(config.targetCassandraVersion)
                            .setDefaultReadConsistencyLevel(config.mainReadConsistency)
                            .setDefaultWriteConsistencyLevel(config.mainWriteConsistency)
                    )
                    .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MainConnectionPool")
                            .setPort(config.cassandraDbPort)
                            .setMaxConnsPerHost(config.cassandraMaxConnPerHost)
                            .setSeeds(config.cassandraDbSeeds)
                    )
                    .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                    .buildKeyspace(ThriftFamilyFactory.getInstance());


            mainContext.start();
            mainKeyspace = mainContext.getEntity();

            try {
                mainKeyspace.createKeyspace(ImmutableMap.<String, Object>builder()
                        .put("strategy_options", ImmutableMap.<String, Object>builder()
                                .put("replication_factor", "" + config.mainReplicationFactor)
                                .build())
                        .put("strategy_class", config.mainStrategyClass)
                        .build()
                );
            } catch (BadRequestException e) {
                LOG.info("Keyspace probably exists: " + e.getMessage());
            }
        }

        {
            electionContext = new AstyanaxContext.Builder()
                    .forCluster(config.clusterName)
                    .forKeyspace("bluehorn_reader_election")
                            /*.withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                                    .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
                            )*/
                    .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                            .setCqlVersion(config.cqlVersion)
                            .setTargetCassandraVersion(config.targetCassandraVersion)
                            .setDefaultReadConsistencyLevel(config.electionReadConsistency)
                            .setDefaultWriteConsistencyLevel(config.electionWriteConsistency)
                    )
                    .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("ElectionConnectionPool")
                            .setPort(config.cassandraDbPort)
                            .setMaxConnsPerHost(config.cassandraMaxConnPerHost)
                            .setSeeds(config.cassandraDbSeeds)
                    )
                    .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                    .buildKeyspace(ThriftFamilyFactory.getInstance());


            electionContext.start();
            electionKeyspace = electionContext.getEntity();

            try {
                electionKeyspace.createKeyspace(ImmutableMap.<String, Object>builder()
                        .put("strategy_options", ImmutableMap.<String, Object>builder()
                                .put("replication_factor", "" + config.electionReplicationFactor)
                                .build())
                        .put("strategy_class", config.electionStrategyClass)
                        .build()
                );
            } catch (BadRequestException e) {
                LOG.info("Keyspace probably exists: " + e.getMessage());
            }
        }

        createTable(mainKeyspace, Article.CQL_TABLE);
        createTable(mainKeyspace, Feed.CQL_TABLE);
        createTable(mainKeyspace, ReadArticlesColl.CQL_TABLE);
        createTable(mainKeyspace, User.CQL_TABLE);
        createTable(mainKeyspace, Crawler.CQL_TABLE);
        createTable(mainKeyspace, LoginInfo.CQL_TABLE);
        addAdminUserIfMissing();

        createTable(electionKeyspace, Election.CQL_TABLE);

        LOG.info("Exiting LowLevelDbAccess.start()");
    }


    private void createTable(Keyspace keyspace, CqlTable cqlTable) throws Exception {
        OperationResult<CqlResult<Integer, String>> result;
        String cql = cqlTable.getCreateStatement();
        LOG.info("Executing " + cql);
        try {
            result = keyspace
                    .prepareQuery(RESULTS_CF)
                    .withCql(cql)
                    .execute();
            LOG.info("Latency (ms): " + result.getLatency(TimeUnit.MILLISECONDS));
        } catch (BadRequestException e) {
            LOG.info("Table probably exists: " + e.getMessage());
        }
        if (cqlTable.indexInfos != null) {
            for (CqlTable.IndexInfo indexInfo : cqlTable.indexInfos) {
                addIndex(keyspace, cqlTable, indexInfo.columnNames);
            }
        }
    }


    private void addIndex(Keyspace keyspace, CqlTable cqlTable, String... columnNames) throws Exception {
        OperationResult<CqlResult<Integer, String>> result;
        String cql = cqlTable.getCreateIndexStatement(columnNames);
        LOG.info("Executing " + cql);
        try {
            result = keyspace
                    .prepareQuery(RESULTS_CF)
                    .withCql(cql)
                    .execute();
            LOG.info("Latency (ms): " + result.getLatency(TimeUnit.MILLISECONDS));
        } catch (BadRequestException e) {
            LOG.info("Index probably exists: " + e.getMessage());
        }
    }


    private void addAdminUserIfMissing() throws Exception {
        User.DB db = new User.DB(this);
        User admin = db.get("admin");
        if (admin != null) {
            return;
        }
        String password = "admin";
        String salt = "salt";

        admin = new User("admin", "admin", User.computeHashedPassword(password, salt), salt, "admin@admins", new ArrayList<String>(), true, true);
        db.add(admin);
    }


    /**
     * Apparently you can read blobs with astyanax but you cannot write them. This adds write support.
     */
    public static class ByteBufferSerializer extends AbstractSerializer<byte[]> {

        @Override
        public ByteBuffer toByteBuffer(byte[] obj) {
            return ByteBuffer.wrap(obj);
        }

        @Override
        public byte[] fromByteBuffer(ByteBuffer byteBuffer) {
            if (byteBuffer == null) {
                return null;
            }
            return byteBuffer.array();
        }
    }

}

//ttt1 see about security/users
