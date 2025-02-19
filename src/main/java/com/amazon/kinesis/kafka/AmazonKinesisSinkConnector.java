package com.amazon.kinesis.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

public class AmazonKinesisSinkConnector extends SinkConnector {

	public static final String REGION = "region";

	public static final String STREAM_NAME = "streamName";

	public static final String MAX_BUFFERED_TIME = "maxBufferedTime";

	public static final String MAX_CONNECTIONS = "maxConnections";

	public static final String MIN_CONNECTIONS = "minConnections";

	public static final String RATE_LIMIT = "rateLimit";

	public static final String RECORD_TTL = "ttl";

	public static final String METRICS_LEVEL = "metricsLevel";

	public static final String METRICS_GRANUALITY = "metricsGranuality";

	public static final String METRICS_NAMESPACE = "metricsNameSpace";

	public static final String AGGREGRATION_ENABLED = "aggregration";

	public static final String AGGREGRATION_MAX_SIZE = "aggregrationMaxSize";

	public static final String AGGREGRATION_MAX_COUNT = "aggregrationMaxCount";

	public static final String THREADING_MODEL = "threadModel";

	public static final String THREAD_POOL_SIZE = "threadPoolSize";

	public static final String COLLECTION_MAX_COUNT  = "collectionMaxCount";
	
	public static final String COLLECTION_MAX_SIZE  = "collectionMaxSize";
	
	public static final String USE_PARTITION_AS_HASH_KEY = "usePartitionAsHashKey";
	
	public static final String FLUSH_SYNC = "flushSync";
	
	public static final String SINGLE_KINESIS_PRODUCER_PER_PARTITION = "singleKinesisProducerPerPartition";
	
	public static final String PAUSE_CONSUMPTION = "pauseConsumption"; 
	
	public static final String OUTSTANDING_RECORDS_THRESHOLD = "outstandingRecordsThreshold";
	
	public static final String SLEEP_PERIOD = "sleepPeriod";
	
	public static final String SLEEP_CYCLES = "sleepCycles";

	public static final String ROLE_ARN = "roleARN";

	public static final String ROLE_SESSION_NAME = "roleSessionName";

	public static final String ROLE_EXTERNAL_ID = "roleExternalID";

	public static final String ROLE_DURATION_SECONDS = "roleDurationSeconds";

	public static final String KINESIS_ENDPOINT = "kinesisEndpoint";

	private String region;

	private String streamName;

	private String roleARN;

	private String roleSessionName;

	private String roleExternalID;

	private String roleDurationSeconds;

	private String kinesisEndpoint;

	private String maxBufferedTime;

	private String maxConnections;

	private String rateLimit;

	private String ttl;

	private String metricsLevel;

	private String metricsGranuality;

	private String metricsNameSpace;

	private String aggregration;

	private String usePartitionAsHashKey;
	
	private String flushSync;
	
	private String singleKinesisProducerPerPartition; 
	
	private String pauseConsumption;
	
	private String outstandingRecordsThreshold;
	
	private String sleepPeriod;
	
	private String sleepCycles;

	private String aggregrationMaxSize;

	private String aggregrationMaxCount;

	private String minConnections;

	private String threadingModel;

	private String threadPoolSize;

	private String collectionMaxCount;

	private String collectionMaxSize;

	@Override
	public void start(Map<String, String> props) {
		region = props.get(REGION);
		streamName = props.get(STREAM_NAME);
		roleARN = props.get(ROLE_ARN);
		roleSessionName = props.get(ROLE_SESSION_NAME);
		roleExternalID = props.get(ROLE_EXTERNAL_ID);
		roleDurationSeconds = props.get(ROLE_DURATION_SECONDS);
		kinesisEndpoint = props.get(KINESIS_ENDPOINT);
		maxBufferedTime = props.get(MAX_BUFFERED_TIME);
		maxConnections = props.get(MAX_CONNECTIONS);
		rateLimit = props.get(RATE_LIMIT);
		ttl = props.get(RECORD_TTL);
		metricsLevel = props.get(METRICS_LEVEL);
		metricsGranuality = props.get(METRICS_GRANUALITY);
		metricsNameSpace = props.get(METRICS_NAMESPACE);
		aggregration = props.get(AGGREGRATION_ENABLED);
		usePartitionAsHashKey = props.get(USE_PARTITION_AS_HASH_KEY);
		flushSync = props.get(FLUSH_SYNC);
		singleKinesisProducerPerPartition = props.get(SINGLE_KINESIS_PRODUCER_PER_PARTITION);
		pauseConsumption = props.get(PAUSE_CONSUMPTION);
		outstandingRecordsThreshold = props.get(OUTSTANDING_RECORDS_THRESHOLD);
		sleepPeriod = props.get(SLEEP_PERIOD);
		sleepCycles = props.get(SLEEP_CYCLES);
		aggregrationMaxSize = props.get(AGGREGRATION_MAX_SIZE);
		aggregrationMaxCount = props.get(AGGREGRATION_MAX_COUNT);
		minConnections = props.get(MIN_CONNECTIONS);
		threadingModel = props.get(THREADING_MODEL);
		threadPoolSize = props.get(THREAD_POOL_SIZE);
		collectionMaxCount = props.get(COLLECTION_MAX_COUNT);
		collectionMaxSize = props.get(COLLECTION_MAX_SIZE);
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public Class<? extends Task> taskClass() {
		return AmazonKinesisSinkTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {
		ArrayList<Map<String, String>> configs = new ArrayList<>();
		for (int i = 0; i < maxTasks; i++) {
			Map<String, String> config = new HashMap<>();
			if (streamName != null)
				config.put(STREAM_NAME, streamName);

			if (region != null)
				config.put(REGION, region);

			if (roleARN != null)
				config.put(ROLE_ARN, roleARN);

			if (roleSessionName != null)
				config.put(ROLE_SESSION_NAME, roleSessionName);

			if (roleExternalID != null)
				config.put(ROLE_EXTERNAL_ID, roleExternalID);

			if (roleDurationSeconds != null)
				config.put(ROLE_DURATION_SECONDS, roleDurationSeconds);
			else
				config.put(ROLE_DURATION_SECONDS, "3600");

			if (kinesisEndpoint != null)
				config.put(KINESIS_ENDPOINT, kinesisEndpoint);

			if (maxBufferedTime != null)
				config.put(MAX_BUFFERED_TIME, maxBufferedTime);
			else
				// default value of 15000 ms
				config.put(MAX_BUFFERED_TIME, "15000");

			if (maxConnections != null)
				config.put(MAX_CONNECTIONS, maxConnections);
			else
				config.put(MAX_CONNECTIONS, "24");

			if (rateLimit != null)
				config.put(RATE_LIMIT, rateLimit);
			else
				config.put(RATE_LIMIT, "100");

			if (ttl != null)
				config.put(RECORD_TTL, ttl);
			else
				config.put(RECORD_TTL, "60000");

			if (metricsLevel != null)
				config.put(METRICS_LEVEL, metricsLevel);
			else
				config.put(METRICS_LEVEL, "none");

			if (metricsGranuality != null)
				config.put(METRICS_GRANUALITY, metricsGranuality);
			else
				config.put(METRICS_GRANUALITY, "global");

			if (metricsNameSpace != null)
				config.put(METRICS_NAMESPACE, metricsNameSpace);
			else
				config.put(METRICS_NAMESPACE, "KinesisProducer");

			if (aggregration != null)
				config.put(AGGREGRATION_ENABLED, aggregration);
			else
				config.put(AGGREGRATION_ENABLED, "false");

			if (usePartitionAsHashKey != null)
				config.put(USE_PARTITION_AS_HASH_KEY, usePartitionAsHashKey);
			else
				config.put(USE_PARTITION_AS_HASH_KEY, "false");
			
			if(flushSync != null)
				config.put(FLUSH_SYNC, flushSync);
			else
				config.put(FLUSH_SYNC, "true");
			
			if(singleKinesisProducerPerPartition != null)
				config.put(SINGLE_KINESIS_PRODUCER_PER_PARTITION, singleKinesisProducerPerPartition);
			else
				config.put(SINGLE_KINESIS_PRODUCER_PER_PARTITION, "false");
			
			if(pauseConsumption != null)
				config.put(PAUSE_CONSUMPTION, pauseConsumption);
			else
				config.put(PAUSE_CONSUMPTION, "true");
			
			if(outstandingRecordsThreshold != null)
				config.put(OUTSTANDING_RECORDS_THRESHOLD, outstandingRecordsThreshold);
			else
				config.put(OUTSTANDING_RECORDS_THRESHOLD, "500000");
			
			if(sleepPeriod != null)
				config.put(SLEEP_PERIOD, sleepPeriod);
			else
				config.put(SLEEP_PERIOD, "1000");
			
			if(sleepCycles != null)
				config.put(SLEEP_CYCLES, sleepCycles);
			else
				config.put(SLEEP_CYCLES, "10");

			if(aggregrationMaxSize != null)
				config.put(AGGREGRATION_MAX_SIZE, aggregrationMaxSize);
			else
				config.put(AGGREGRATION_MAX_SIZE, "1048576");
			
			if(aggregrationMaxCount != null)
				config.put(AGGREGRATION_MAX_COUNT, aggregrationMaxCount);
			else
				config.put(AGGREGRATION_MAX_COUNT, "9223372036854775807");

			if(minConnections != null)
				config.put(MIN_CONNECTIONS, minConnections);
			else
				config.put(MIN_CONNECTIONS, "16");

			if(threadingModel != null)
				config.put(THREADING_MODEL, threadingModel);
			else
				config.put(THREADING_MODEL, "PER_REQUEST");

			if(threadPoolSize != null)
				config.put(THREAD_POOL_SIZE, threadPoolSize);
			else
				config.put(THREAD_POOL_SIZE, "25");

			if(collectionMaxCount != null)
				config.put(COLLECTION_MAX_COUNT, collectionMaxCount);
			else
				config.put(COLLECTION_MAX_COUNT, "500");

			if(collectionMaxSize != null)
				config.put(COLLECTION_MAX_SIZE, collectionMaxSize);
			else
				config.put(COLLECTION_MAX_SIZE, "5242880");

			configs.add(config);

		}
		return configs;
	}

	@Override
	public String version() {
		// Currently using Kafka version, in future release use Kinesis-Kafka version
		return AppInfoParser.getVersion();

	}

	@Override
	public ConfigDef config() {
		// TODO Auto-generated method stub
		return new ConfigDef();
	}

}