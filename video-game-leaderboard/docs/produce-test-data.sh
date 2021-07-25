/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:29092 \
  --topic players \
  --property 'parse.key=true' \
  --property 'key.separator=|' < players.json

/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:29092 \
  --topic products \
  --property 'parse.key=true' \
  --property 'key.separator=|' < products.json

/Users/user/Documents/dev/kaf/kafka_2.12-2.7.0/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:29092 \
  --topic score-events < score-events.json
