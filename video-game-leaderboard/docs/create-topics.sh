# create the game events topic

#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic score-events
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic players
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic products
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic high-scores
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic dev-KSTREAM-AGGREGATE-STATE-STORE-0000000015-changelog
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic dev-KSTREAM-AGGREGATE-STATE-STORE-0000000015-repartition
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic dev-KSTREAM-KEY-SELECT-0000000001-repartition
#/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh --delete --bootstrap-server broker01:9092 --topic dev-players-STATE-STORE-0000000002-changelog

/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 \
  --topic score-events \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create the players topic
/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 \
  --topic players \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create the products topic
/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 \
  --topic products \
  --replication-factor 1 \
  --partitions 4 \
  --create

# create the high-scores topic
/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 \
  --topic high-scores \
  --replication-factor 1 \
  --partitions 4 \
  --create

# bin/kafka-topics.sh --bootstrap-server localhost:29092 --list
