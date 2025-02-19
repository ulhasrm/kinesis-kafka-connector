package com.amazon.kinesis.kafka;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.util.StringUtils;
import com.google.common.util.concurrent.MoreExecutors;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.apache.kafka.connect.sink.SinkTaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.kinesis.producer.Attempt;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import com.amazonaws.services.kinesis.producer.UserRecordFailedException;
import com.amazonaws.services.kinesis.producer.UserRecordResult;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration.ThreadingModel;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class AmazonKinesisSinkTask extends SinkTask {
	private final static Logger logger = LoggerFactory.getLogger(AmazonKinesisSinkTask.class);

	private String streamName;

	private String regionName;

	private String roleARN;

	private String roleExternalID;

	private String roleSessionName;

	private int roleDurationSeconds;

	private String kinesisEndpoint;

	private int maxConnections;

	private int rateLimit;

	private int maxBufferedTime;

	private int ttl;

	private String metricsLevel;

	private String metricsGranuality;

	private String metricsNameSpace;

	private boolean aggregration;

	private long aggregrationMaxSize;

	private long aggregrationMaxCount;

	private int minConnections;

	private KinesisProducerConfiguration.ThreadingModel threadingModel;

	private int threadPoolSize;

	private long collectionMaxCount;

	private long collectionMaxSize;

	private boolean usePartitionAsHashKey;

	private boolean flushSync;

	private boolean singleKinesisProducerPerPartition;

	private boolean pauseConsumption;

	private int outstandingRecordsThreshold;

	private int sleepPeriod;

	private int sleepCycles;

	private SinkTaskContext sinkTaskContext;

	private Map<String, KinesisProducer> producerMap = new HashMap<String, KinesisProducer>();

	private KinesisProducer kinesisProducer;

	final FutureCallback<UserRecordResult> callback = new FutureCallback<UserRecordResult>() {
		@Override
		public void onFailure(Throwable t) {
			if (t instanceof UserRecordFailedException) {
				Attempt last = Iterables.getLast(((UserRecordFailedException) t).getResult().getAttempts());
				throw new DataException("Kinesis Producer was not able to publish data - " + last.getErrorCode() + "-"
						+ last.getErrorMessage());

			}
			throw new DataException("Exception during Kinesis put", t);
		}

		@Override
		public void onSuccess(UserRecordResult result) {

		}
	};

	@Override
	public void initialize(SinkTaskContext context) {
		sinkTaskContext = context;
	}

	@Override
	public String version() {
		return null;
	}

	@Override
	public void flush(Map<TopicPartition, OffsetAndMetadata> arg0) {
		// TODO Auto-generated method stub
		if (singleKinesisProducerPerPartition) {
			producerMap.values().forEach(producer -> {
				if (flushSync)
					producer.flushSync();
				else
					producer.flush();
			});
		} else {
			if (flushSync)
				kinesisProducer.flushSync();
			else
				kinesisProducer.flush();
		}
	}

	@Override
	public void put(Collection<SinkRecord> sinkRecords) {

		// If KinesisProducers cannot write to Kinesis Streams (because of
		// connectivity issues, access issues
		// or misconfigured shards we will pause consumption of messages till
		// backlog is cleared

		validateOutStandingRecords();

		String partitionKey;
		for (SinkRecord sinkRecord : sinkRecords) {

			ListenableFuture<UserRecordResult> f;
			// Kinesis does not allow empty partition key
			if (sinkRecord.key() != null && !sinkRecord.key().toString().trim().equals("")) {
				partitionKey = sinkRecord.key().toString().trim();
			} else {
				partitionKey = Integer.toString(sinkRecord.kafkaPartition());
			}

			if (singleKinesisProducerPerPartition)
				f = addUserRecord(producerMap.get(sinkRecord.kafkaPartition() + "@" + sinkRecord.topic()), streamName,
						partitionKey, usePartitionAsHashKey, sinkRecord);
			else
				f = addUserRecord(kinesisProducer, streamName, partitionKey, usePartitionAsHashKey, sinkRecord);

			Futures.addCallback(f, callback, MoreExecutors.directExecutor());

		}
	}

	private boolean validateOutStandingRecords() {
		if (pauseConsumption) {
			if (singleKinesisProducerPerPartition) {
				producerMap.values().forEach(producer -> {
					int sleepCount = 0;
					boolean pause = false;
					// Validate if producer has outstanding records within
					// threshold values
					// and if not pause further consumption
					while (producer.getOutstandingRecordsCount() > outstandingRecordsThreshold) {
						try {
							// Pausing further
							sinkTaskContext.pause(convertTopicPartitionSetToArray(sinkTaskContext.assignment(), true));
							pause = true;
							Thread.sleep(sleepPeriod);
							if (sleepCount++ > sleepCycles) {
								// Dummy message - Replace with your code to
								// notify/log that Kinesis Producers have
								// buffered values
								// but are not being sent
								System.out.println(
										"Kafka Consumption has been stopped because Kinesis Producers has buffered messages above threshold");
								sleepCount = 0;
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if (pause)
						sinkTaskContext.resume(convertTopicPartitionSetToArray(sinkTaskContext.assignment(), false));
				});
				return true;
			} else {
				int sleepCount = 0;
				boolean pause = false;
				// Validate if producer has outstanding records within threshold
				// values
				// and if not pause further consumption
				while (kinesisProducer.getOutstandingRecordsCount() > outstandingRecordsThreshold) {
					try {
						// Pausing further
						sinkTaskContext.pause(convertTopicPartitionSetToArray(sinkTaskContext.assignment(), true));
						pause = true;
						Thread.sleep(sleepPeriod);
						if (sleepCount++ > sleepCycles) {
							// Dummy message - Replace with your code to
							// notify/log that Kinesis Producers have buffered
							// values
							// but are not being sent
							System.out.println(
									"Kafka Consumption has been stopped because Kinesis Producers has buffered messages above threshold");
							sleepCount = 0;
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (pause)
					sinkTaskContext.resume(convertTopicPartitionSetToArray(sinkTaskContext.assignment(), false));
				return true;
			}
		} else {
			return true;
		}
	}

	private ListenableFuture<UserRecordResult> addUserRecord(KinesisProducer kp, String streamName, String partitionKey,
			boolean usePartitionAsHashKey, SinkRecord sinkRecord) {

		// If configured use kafka partition key as explicit hash key
		// This will be useful when sending data from same partition into
		// same shard
		if (usePartitionAsHashKey)
			return kp.addUserRecord(streamName, partitionKey, Integer.toString(sinkRecord.kafkaPartition()),
					DataUtility.parseValue(sinkRecord.valueSchema(), sinkRecord.value()));
		else
			return kp.addUserRecord(streamName, partitionKey,
					DataUtility.parseValue(sinkRecord.valueSchema(), sinkRecord.value()));

	}

	@Override
	public void start(Map<String, String> props) {

		streamName = props.get(AmazonKinesisSinkConnector.STREAM_NAME);

		maxConnections = Integer.parseInt(props.get(AmazonKinesisSinkConnector.MAX_CONNECTIONS));

		rateLimit = Integer.parseInt(props.get(AmazonKinesisSinkConnector.RATE_LIMIT));

		maxBufferedTime = Integer.parseInt(props.get(AmazonKinesisSinkConnector.MAX_BUFFERED_TIME));

		ttl = Integer.parseInt(props.get(AmazonKinesisSinkConnector.RECORD_TTL));

		regionName = props.get(AmazonKinesisSinkConnector.REGION);

		roleARN = props.get(AmazonKinesisSinkConnector.ROLE_ARN);

		roleSessionName = props.get(AmazonKinesisSinkConnector.ROLE_SESSION_NAME);

		roleDurationSeconds =  Integer.parseInt(props.get(AmazonKinesisSinkConnector.ROLE_DURATION_SECONDS));

		roleExternalID = props.get(AmazonKinesisSinkConnector.ROLE_EXTERNAL_ID);

		kinesisEndpoint = props.get(AmazonKinesisSinkConnector.KINESIS_ENDPOINT);

		metricsLevel = props.get(AmazonKinesisSinkConnector.METRICS_LEVEL);

		metricsGranuality = props.get(AmazonKinesisSinkConnector.METRICS_GRANUALITY);

		metricsNameSpace = props.get(AmazonKinesisSinkConnector.METRICS_NAMESPACE);

		aggregration = Boolean.parseBoolean(props.get(AmazonKinesisSinkConnector.AGGREGRATION_ENABLED));

		usePartitionAsHashKey = Boolean.parseBoolean(props.get(AmazonKinesisSinkConnector.USE_PARTITION_AS_HASH_KEY));

		flushSync = Boolean.parseBoolean(props.get(AmazonKinesisSinkConnector.FLUSH_SYNC));

		singleKinesisProducerPerPartition = Boolean
				.parseBoolean(props.get(AmazonKinesisSinkConnector.SINGLE_KINESIS_PRODUCER_PER_PARTITION));

		pauseConsumption = Boolean.parseBoolean(props.get(AmazonKinesisSinkConnector.PAUSE_CONSUMPTION));

		outstandingRecordsThreshold = Integer
				.parseInt(props.get(AmazonKinesisSinkConnector.OUTSTANDING_RECORDS_THRESHOLD));

		sleepPeriod = Integer.parseInt(props.get(AmazonKinesisSinkConnector.SLEEP_PERIOD));

		sleepCycles = Integer.parseInt(props.get(AmazonKinesisSinkConnector.SLEEP_CYCLES));

		aggregrationMaxCount = Long.parseLong(props.get(AmazonKinesisSinkConnector.AGGREGRATION_MAX_COUNT));

		aggregrationMaxSize = Long.parseLong(props.get(AmazonKinesisSinkConnector.AGGREGRATION_MAX_SIZE));

		minConnections = Integer.parseInt(props.get(AmazonKinesisSinkConnector.MIN_CONNECTIONS));

		collectionMaxCount = Long.parseLong(props.get(AmazonKinesisSinkConnector.COLLECTION_MAX_COUNT));

		collectionMaxSize = Long.parseLong(props.get(AmazonKinesisSinkConnector.COLLECTION_MAX_SIZE));

		final String model = props.get(AmazonKinesisSinkConnector.THREADING_MODEL);
		threadingModel = ThreadingModel.PER_REQUEST.name().equals(model) ? ThreadingModel.PER_REQUEST : ThreadingModel.POOLED;

		threadPoolSize = Integer.parseInt(props.get(AmazonKinesisSinkConnector.THREAD_POOL_SIZE));

		if (!singleKinesisProducerPerPartition)
			kinesisProducer = getKinesisProducer();

	}

	public void open(Collection<TopicPartition> partitions) {
		if (singleKinesisProducerPerPartition) {
			for (TopicPartition topicPartition : partitions) {
				producerMap.put(topicPartition.partition() + "@" + topicPartition.topic(), getKinesisProducer());
			}
		}
	}

	public void close(Collection<TopicPartition> partitions) {
		if (singleKinesisProducerPerPartition) {
			for (TopicPartition topicPartition : partitions) {
				producerMap.get(topicPartition.partition() + "@" + topicPartition.topic()).destroy();
				producerMap.remove(topicPartition.partition() + "@" + topicPartition.topic());
			}
		}
	}

	@Override
	public void stop() {
		// destroying kinesis producers which were not closed as part of close
		if (singleKinesisProducerPerPartition) {
			for (KinesisProducer kp : producerMap.values()) {
				kp.flushSync();
				kp.destroy();
			}
		} else {
			kinesisProducer.destroy();
		}

	}

	private KinesisProducer getKinesisProducer() {
		KinesisProducerConfiguration config = new KinesisProducerConfiguration();
		config.setRegion(regionName);
		config.setCredentialsProvider(IAMUtility.createCredentials(regionName, roleARN, roleExternalID, roleSessionName, roleDurationSeconds));
		config.setMaxConnections(maxConnections);
		if (!StringUtils.isNullOrEmpty(kinesisEndpoint))
			config.setKinesisEndpoint(kinesisEndpoint);

		config.setAggregationEnabled(aggregration);

		// Maximum number of bytes to pack into an aggregated Kinesis record.
		// Default: 51200
		// Minimum: 64
		// Maximum (inclusive): 1048576
		config.setAggregationMaxSize(aggregrationMaxSize);

		// Maximum number of items to pack into an aggregated record.
		// Default: 4294967295
		// Minimum: 1
		// Maximum (inclusive): 9223372036854775807
		config.setAggregationMaxCount(aggregrationMaxCount);

		// Minimum number of connections to keep open to the backend.
		// Default: 1
		// Minimum: 1
		// Maximum (inclusive): 16
		config.setMinConnections(minConnections);

		//Sets the threading model that the native process will use.
		// Default = PER_REQUEST
		// Options - PER_REQUEST, POOLED
		if( ThreadingModel.POOLED.equals(threadingModel) )
		{
			config.setThreadingModel(threadingModel);
			config.setThreadPoolSize(threadPoolSize);
		}
		
		// Maximum number of items to pack into an PutRecords request.
		// Default: 500
		// Minimum: 1
		// Maximum (inclusive): 500
		config.setCollectionMaxCount(collectionMaxCount);

		// Maximum amount of data to send with a PutRecords request.
		// Default: 5242880
		// Minimum: 52224
		// Maximum (inclusive): 9223372036854775807
		config.setCollectionMaxSize(collectionMaxSize);

		// Limits the maximum allowed put rate for a shard, as a percentage of
		// the
		// backend limits.
		config.setRateLimit(rateLimit);

		// Maximum amount of time (milliseconds) a record may spend being
		// buffered
		// before it gets sent. Records may be sent sooner than this depending
		// on the
		// other buffering limits
		config.setRecordMaxBufferedTime(maxBufferedTime);

		// Set a time-to-live on records (milliseconds). Records that do not get
		// successfully put within the limit are failed.
		config.setRecordTtl(ttl);

		// Controls the number of metrics that are uploaded to CloudWatch.
		// Expected pattern: none|summary|detailed
		config.setMetricsLevel(metricsLevel);

		// Controls the granularity of metrics that are uploaded to CloudWatch.
		// Greater granularity produces more metrics.
		// Expected pattern: global|stream|shard
		config.setMetricsGranularity(metricsGranuality);

		// The namespace to upload metrics under.
		config.setMetricsNamespace(metricsNameSpace);
		logger.info("Before Creating Producer");
		final KinesisProducer producer = new KinesisProducer(config);
		logger.info("After Creating Producer : " + producer);
		printProducerConfig(config);
		return producer;
	}

	private TopicPartition[] convertTopicPartitionSetToArray(final Set<TopicPartition> partitionSet, final boolean pause ) {
		final String action = pause ? "Pause":"Resume";
		if (partitionSet != null && partitionSet.size() > 0) {
			logger.info(action + " " + partitionSet );
			final TopicPartition[] partitions = new TopicPartition[partitionSet.size()];
				int index = 0;
				for (TopicPartition partition : partitionSet) {
					partitions[index++] = partition;
				}
			return partitions;
		} else {
			logger.info(action + " empty Partitions");
			return null;
		}
	}

	private void printProducerConfig(final KinesisProducerConfiguration config)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("aggregationEnabled: " + config.isAggregationEnabled() + "\n" );
		sb.append("aggregationMaxCount: " + config.getAggregationMaxCount() + "\n" );
		sb.append("aggregationMaxSize: " + config.getAggregationMaxSize() + "\n" );
		sb.append("collectionMaxCount: " + config.getCollectionMaxCount() + "\n" );
		sb.append("CollectionMaxSize: " + config.getCollectionMaxSize() + "\n" );
		sb.append("minConnections: " + config.getMinConnections() + "\n" );
		sb.append("MaxConnections: " + config.getMaxConnections() + "\n" );
		sb.append("threadingModel: " + config.getThreadingModel().name() + "\n" );
		sb.append("threadPoolSize: " + config.getThreadPoolSize() + "\n" );
		sb.append("connectTimeout: " + config.getConnectTimeout() + "\n" );
		sb.append("metricsGranularity: " + config.getMetricsGranularity() + "\n" );
		sb.append("metricsLevel: " + config.getMetricsLevel() + "\n" );
		sb.append("rateLimit: " + config.getRateLimit() + "\n" );
		sb.append("recordMaxBufferedTime: " + config.getRecordMaxBufferedTime() + "\n" );
		sb.append("recordTtl: " + config.getRecordTtl() + "\n" );
		sb.append("connectTimeout: " + config.getRegion() + "\n" );
		sb.append("requestTimeout: " + config.getRequestTimeout() + "\n" );
		sb.append("failIfThrottled: " + config.isFailIfThrottled() + "\n" );
		logger.info("ProducerConfig : \n" + sb.toString() );
	}
}
