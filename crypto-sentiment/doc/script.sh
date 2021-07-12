echo "Waiting for Kafka to come online..."

# create the tweets topic
/Users/user/Documents/dev/kaf/kafka_2.12-2.5.0/bin/kafka-topics.sh \
  --bootstrap-server broker01:9092 \
  --topic tweets \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create the crypto-sentiment topic
/Users/user/Documents/dev/kaf/kafka_2.12-2.5.0/bin/kafka-topics.sh \
  --bootstrap-server broker01:9092 \
  --topic crypto-sentiment \
  --replication-factor 1 \
  --partitions 4 \
  --create
